package com.shterneregen.securelan.androidclient.network.discovery

import android.content.Context
import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import com.shterneregen.securelan.common.net.address.IpLiteral
import java.util.concurrent.atomic.AtomicLong

/** Browse only: advertisements belong to AndroidChatHostService. */
class AndroidNsdDiscoveryProvider internal constructor(
    private val facade: NsdBackend,
    private val networks: LanNetworkSource,
    private val multicast: MulticastLeaseOwner,
    private val capabilities: NsdApiCapabilities,
) : DiscoveryProvider {
    constructor(context: Context) : this(NsdFacade(context), AndroidLanNetworkMonitor(context), MulticastLease(context), NsdApiCapabilities.current())
    override val id = DiscoveryProviderId.MDNS
    private val generation = AtomicLong()
    private val browsers = linkedMapOf<String, NsdFacade.Browser>()
    private var callback: ((DiscoveryEvent) -> Unit)? = null
    @Synchronized override fun start(request: DiscoveryStartRequest, emit: (DiscoveryEvent) -> Unit) {
        stop()
        val run = generation.incrementAndGet()
        callback = emit
        fun refresh() = synchronized(this) {
            if (generation.get() != run) return@synchronized
            val available = networks.snapshot().filter { it.address.isIpv4 && it.address.isUnicast }.map { it.interfaceKey }.distinct()
            val modern = capabilities.networkScoped
            val selected = if (modern) available.take(4).map { it to it } else if (available.isNotEmpty()) listOf("legacy" to null) else emptyList()
            val keys = selected.map { it.first }.toSet()
            browsers.keys.filter { it !in keys }.forEach { key -> browsers.remove(key)?.close(); emit(DiscoveryEvent.ResetSource(id, run, key)) }
            selected.filter { it.first !in browsers }.forEach { (key, network) ->
                browsers[key] = facade.browse(network, { service ->
                    if (generation.get() != run) return@browse
                    val record = DnsSdRecordCodec.decode(service.txt, service.port) ?: return@browse
                    if (record.peerId == request.udpConfig.peerId) return@browse
                    val bindings = networks.snapshot()
                    val endpoints = service.addresses.mapNotNull { raw ->
                        val ip = IpLiteral.parse(raw.substringBefore('%')) ?: return@mapNotNull null
                        // IPv6 remains disabled pending physical cross-client qualification.
                        if (!ip.isIpv4 || !ip.isUnicast) return@mapNotNull null
                        val binding = bindings.firstOrNull { it.prefix.contains(ip) && (network == null || it.interfaceKey == network) } ?: return@mapNotNull null
                        DiscoveryEndpoint(ip, service.port, binding.key)
                    }.take(8)
                    if (endpoints.isNotEmpty()) emit(DiscoveryEvent.Upsert(DiscoveryObservation(ObservationKey(id, run, key, service.name),
                        record.peerId, record.roomInstanceId, record.displayName, endpoints, record.filePort,
                        if (capabilities.serviceUpdates) null else 90_000_000_000L)))
                }, { name -> if (generation.get() == run) emit(DiscoveryEvent.Remove(ObservationKey(id, run, key, name))) }, { failure ->
                    if (generation.get() == run) emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, when (failure) {
                        NsdFailure.PERMISSION_DENIED -> ProviderAvailability.UNAVAILABLE
                        NsdFailure.LIMIT -> ProviderAvailability.RUNNING
                        NsdFailure.PLATFORM -> ProviderAvailability.FAILED
                    }, failure == NsdFailure.LIMIT)))
                })
            }
            if (browsers.isEmpty()) multicast.close() else multicast.acquire()
            emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, if (browsers.isEmpty()) ProviderAvailability.UNAVAILABLE else ProviderAvailability.RUNNING)))
        }
        try { networks.start(::refresh); refresh() }
        catch (_: SecurityException) {
            networks.close(); multicast.close()
            emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, ProviderAvailability.UNAVAILABLE)))
        }
    }
    override fun updateAdvertisement(value: HostedRoomAdvertisement?) = Unit
    @Synchronized override fun stop() {
        generation.incrementAndGet(); networks.close()
        browsers.values.forEach { it.close() }; browsers.clear(); multicast.close(); callback = null
    }
    override fun close() { stop(); facade.close() }
}
