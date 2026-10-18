package com.shterneregen.securelan.desktop.network.discovery

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Blocking library calls and topology polling belong to a per-run IO owner, never the UI/callback. */
class DesktopMdnsDiscoveryProvider(
    private val networks: () -> List<NetworkBinding> = DesktopNetworkMonitor()::snapshot,
    private val create: (NetworkBinding) -> JmDnsFacade = ::RealJmDnsFacade,
) : DiscoveryProvider {
    override val id = DiscoveryProviderId.MDNS
    private val generation = AtomicLong()
    private class Run(val token: Long, val emit: (DiscoveryEvent) -> Unit, var host: HostedRoomAdvertisement?) {
        val owner = Executors.newSingleThreadScheduledExecutor { Thread(it, "desktop-mdns").apply { isDaemon = true } }
        val handles = java.util.concurrent.ConcurrentHashMap<String, Pair<NetworkBinding, JmDnsFacade>>()
        val failures = mutableMapOf<String, Pair<Int, Long>>()
    }
    @Volatile private var active: Run? = null
    override fun start(request: DiscoveryStartRequest, emit: (DiscoveryEvent) -> Unit) {
        stop()
        val run = Run(generation.incrementAndGet(), emit, request.hostedRoom)
        active = run
        run.owner.scheduleWithFixedDelay({
            if (active !== run) return@scheduleWithFixedDelay
            runCatching {
                val bindings = NetworkBindingSelector.select(networks(), request.networkPolicy)
                val keys = bindings.map { it.key }.toSet()
                run.handles.keys.filter { it !in keys }.forEach { key ->
                    val old = run.handles.remove(key)!!
                    emit(DiscoveryEvent.ResetSource(id, run.token, key))
                    runCatching { old.second.close() }
                }
                run.failures.keys.retainAll(keys)
                bindings.filter { !run.handles.containsKey(it.key) }.forEach { binding ->
                    val failure = run.failures[binding.key]
                    if (failure != null && System.nanoTime() < failure.second) return@forEach
                    var facade: JmDnsFacade? = null
                    try {
                        facade = create(binding)
                        if (active !== run) { facade.close(); return@forEach }
                        val owned = facade
                        run.handles[binding.key] = binding to owned
                        owned.browse({ service ->
                            if (active !== run || run.handles[binding.key]?.second !== owned) return@browse
                            val record = DnsSdRecordCodec.decode(service.txt, service.port) ?: return@browse
                            if (record.peerId == request.udpConfig.peerId) return@browse
                            val endpoints = service.addresses.take(8).mapNotNull { raw ->
                                val ip = IpLiteral.parse(raw.substringBefore('%')) ?: return@mapNotNull null
                                if (!ip.isUnicast || ip.isIpv4 != binding.address.isIpv4 || !binding.prefix.contains(ip)) return@mapNotNull null
                                runCatching { DiscoveryEndpoint(ip, service.port, binding.key, binding.scopeId) }.getOrNull()
                            }
                            if (endpoints.isNotEmpty()) emit(DiscoveryEvent.Upsert(DiscoveryObservation(
                                ObservationKey(id, run.token, binding.key, service.name), record.peerId, record.roomInstanceId,
                                record.displayName, endpoints, record.filePort)))
                        }, { name -> if (active === run) emit(DiscoveryEvent.Remove(ObservationKey(id, run.token, binding.key, name))) })
                        owned.advertise(run.host)
                        if (active !== run) { run.handles.remove(binding.key); owned.close() }
                        run.failures.remove(binding.key)
                    } catch (_: Exception) {
                        run.handles.remove(binding.key)
                        if (active === run) emit(DiscoveryEvent.ResetSource(id, run.token, binding.key))
                        runCatching { facade?.close() }
                        val attempt = (failure?.first ?: 0) + 1
                        val seconds = listOf(1, 2, 5, 15, 30)[(attempt - 1).coerceAtMost(4)]
                        run.failures[binding.key] = attempt to (System.nanoTime() + seconds * 1_000_000_000L)
                    }
                }
                if (active === run) emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id,
                    if (run.handles.isEmpty()) ProviderAvailability.UNAVAILABLE else ProviderAvailability.RUNNING, run.failures.isNotEmpty())))
            }.onFailure { if (active === run) emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, ProviderAvailability.FAILED))) }
        }, 0, 5, TimeUnit.SECONDS)
    }
    override fun updateAdvertisement(value: HostedRoomAdvertisement?) {
        val run = active ?: return
        run.owner.execute {
            if (active !== run) return@execute
            run.host = value
            run.handles.values.forEach { (_, handle) -> runCatching { handle.advertise(value) }.onFailure {
                run.emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, ProviderAvailability.RUNNING, true)))
            } }
        }
    }
    override fun stop() {
        val run = active ?: return
        active = null
        generation.incrementAndGet()
        run.owner.execute {
            run.handles.values.forEach { runCatching { it.second.close() } }
            run.handles.clear()
        }
        run.owner.shutdown()
    }
}
