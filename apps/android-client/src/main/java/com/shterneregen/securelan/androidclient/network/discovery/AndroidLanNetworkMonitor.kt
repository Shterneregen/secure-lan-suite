package com.shterneregen.securelan.androidclient.network.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import java.net.Inet6Address

/** Tracks underlying LANs, including networks with no Internet/VALIDATED capability. */
interface LanNetworkSource : AutoCloseable {
    fun snapshot(): List<NetworkBinding>
    fun start(changed: () -> Unit)
    fun hasVpn(): Boolean = false
}

class AndroidLanNetworkMonitor(context: Context) : LanNetworkSource {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val registry = sharedRegistry(context.applicationContext)
    private var subscription: AutoCloseable? = null
    fun networks(): List<Network> = snapshots().filter { it.isLan }.map { it.network }
    override fun hasVpn(): Boolean = snapshots().any { it.isVpn }
    override fun snapshot(): List<NetworkBinding> = snapshots().filter { it.isLan }.flatMap { it.bindings }
    fun network(routeKey: String?): Network? = networks().firstOrNull { routeKey == it.toString() || routeKey?.startsWith("$it:") == true }
    override fun start(changed: () -> Unit) {
        close()
        subscription = registry.subscribe(changed)
    }
    override fun close() { subscription?.close(); subscription = null }

    /** Bootstrap one-shot readers from the default network until callback snapshots are available. */
    private fun snapshots(): List<LanNetworkSnapshot<Network>> = registry.snapshot().ifEmpty {
        if (registry.receivedEvents()) return@ifEmpty emptyList()
        val network = manager.activeNetwork ?: return@ifEmpty emptyList()
        val capabilities = manager.getNetworkCapabilities(network) ?: return@ifEmpty emptyList()
        listOf(LanNetworkSnapshot(network, isLan(capabilities), capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
            manager.getLinkProperties(network)?.let { bindings(network, it) }.orEmpty()))
    }

    companion object {
        @Volatile private var shared: LanNetworkSnapshotRegistry<Network>? = null

        private fun sharedRegistry(context: Context): LanNetworkSnapshotRegistry<Network> = shared ?: synchronized(this) {
            shared ?: LanNetworkSnapshotRegistry<Network> { events ->
                val manager = context.getSystemService(ConnectivityManager::class.java)
                val listener = object : ConnectivityManager.NetworkCallback() {
                    // API 26+ follows onAvailable with capabilities/properties; consume those values directly.
                    override fun onLost(network: Network) { events.remove(network) }
                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        events.update(network) { it.copy(isLan = isLan(networkCapabilities), isVpn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) }
                    }
                    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                        val bindings = bindings(network, linkProperties)
                        events.update(network) { it.copy(bindings = bindings) }
                    }
                }
                val request = NetworkRequest.Builder()
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .build()
                manager.registerNetworkCallback(request, listener)
                AutoCloseable { manager.unregisterNetworkCallback(listener) }
            }.also { shared = it }
        }

        private fun isLan(capabilities: NetworkCapabilities): Boolean = !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

        private fun bindings(network: Network, properties: LinkProperties): List<NetworkBinding> = properties.linkAddresses.mapNotNull { address ->
            val ip = IpLiteral.parse(address.address.hostAddress.orEmpty().substringBefore('%')) ?: return@mapNotNull null
            NetworkBinding("$network:${ip.value}", network.toString(), ip, address.prefixLength,
                scopeId = (address.address as? Inet6Address)?.scopeId?.takeIf { it > 0 })
        }
    }
}
