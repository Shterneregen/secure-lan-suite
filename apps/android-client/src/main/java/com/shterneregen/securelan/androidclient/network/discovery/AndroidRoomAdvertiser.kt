package com.shterneregen.securelan.androidclient.network.discovery

import android.content.Context
import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement

/** Service-owned registration, independent of the Activity's search intent. */
data class NsdAdvertisingStatus(val availability: com.shterneregen.securelan.chat.discovery.ProviderAvailability, val legacyScopeUnavailable: Boolean = false)

class AndroidRoomAdvertiser internal constructor(
    private val facade: NsdBackend,
    private val networks: LanNetworkSource,
    private val multicast: MulticastLeaseOwner,
    private val capabilities: NsdApiCapabilities,
    private val status: (NsdAdvertisingStatus) -> Unit,
) : AutoCloseable {
    constructor(context: Context, status: (NsdAdvertisingStatus) -> Unit = {}) :
        this(NsdFacade(context), AndroidLanNetworkMonitor(context), MulticastLease(context), NsdApiCapabilities.current(), status)
    private var value: HostedRoomAdvertisement? = null
    @Volatile private var generation = 0L
    private val registrations = mutableListOf<NsdFacade.Registration>()
    @Synchronized fun update(advertisement: HostedRoomAdvertisement?) {
        value = advertisement
        networks.close()
        if (advertisement != null) networks.start { refresh() }
        refresh()
    }
    @Synchronized private fun refresh() {
        val run = ++generation
        registrations.forEach { it.close() }; registrations.clear(); multicast.close()
        val host = value?.takeIf { it.discoverable } ?: run { status(NsdAdvertisingStatus(com.shterneregen.securelan.chat.discovery.ProviderAvailability.STOPPED)); return }
        val selected = networks.snapshot().filter { it.address.isIpv4 && it.address.isUnicast }.map { it.interfaceKey }.distinct().take(4)
        if (selected.isEmpty()) { status(NsdAdvertisingStatus(com.shterneregen.securelan.chat.discovery.ProviderAvailability.UNAVAILABLE)); return }
        status(NsdAdvertisingStatus(com.shterneregen.securelan.chat.discovery.ProviderAvailability.STARTING))
        val ready = { if (generation == run) status(NsdAdvertisingStatus(com.shterneregen.securelan.chat.discovery.ProviderAvailability.RUNNING)) }
        val failed: (NsdFailure) -> Unit = { reason -> if (generation == run) status(NsdAdvertisingStatus(
            if (reason == NsdFailure.PERMISSION_DENIED) com.shterneregen.securelan.chat.discovery.ProviderAvailability.UNAVAILABLE
            else com.shterneregen.securelan.chat.discovery.ProviderAvailability.FAILED)) }
        if (capabilities.networkScoped) selected.forEach { registrations += facade.register(host, it, ready, failed) }
        else if (!networks.hasVpn() && selected.size == 1) registrations += facade.register(host, null, ready, failed)
        if (registrations.isNotEmpty()) multicast.acquire()
        else status(NsdAdvertisingStatus(com.shterneregen.securelan.chat.discovery.ProviderAvailability.UNAVAILABLE, true))
    }
    override fun close() { update(null); facade.close() }
}
