package com.shterneregen.securelan.desktop.network.discovery

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import com.shterneregen.securelan.common.net.transport.ClientSocketFactory
import com.shterneregen.securelan.common.net.transport.TransportEndpoint
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/** Captures a local source for an explicit attempt. A lost binding fails rather than changing the session route. */
class DesktopLanSocketConnector(private val networks: () -> List<NetworkBinding> = DesktopNetworkMonitor()::snapshot) : ClientSocketFactory {
    @Volatile private var selectedSource: IpLiteral? = null
    fun select(routeKey: String?, host: String) {
        val address = IpLiteral.parse(host.substringBefore('%'))
        if (address == null || !address.isUnicast) { selectedSource = null; return }
        val bindings = NetworkBindingSelector.select(networks())
        if (routeKey != null) {
            selectedSource = requireNotNull(bindings.firstOrNull { it.key == routeKey }) { "Selected LAN is no longer available" }.address
        } else {
            val candidates = bindings.filter { it.prefix.contains(address) }
            require(candidates.map { it.interfaceKey }.distinct().size <= 1) { "Multiple LAN routes match. Select a nearby room endpoint." }
            selectedSource = candidates.firstOrNull()?.address
        }
    }
    override fun connect(endpoint: TransportEndpoint): Socket {
        val source = selectedSource
        return Socket().also { socket ->
            try {
                source?.let { socket.bind(InetSocketAddress(InetAddress.getByAddress(it.bytes()), 0)) }
                socket.connect(InetSocketAddress(endpoint.host(), endpoint.port()), ClientSocketFactory.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            } catch (error: Throwable) { socket.close(); throw error }
        }
    }
}
