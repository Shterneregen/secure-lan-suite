package com.shterneregen.securelan.androidclient.network

import android.content.Context
import android.net.Network
import com.shterneregen.securelan.androidclient.network.discovery.AndroidLanNetworkMonitor
import com.shterneregen.securelan.common.net.address.IpLiteral
import java.net.Socket

/** Captured once before an attempt and retained by chat/file owners. Never binds the process. */
class LanSocketRoute internal constructor(private val network: Network) {
    fun bind(socket: Socket) { network.bindSocket(socket) }
}
class LanSocketConnector(context: Context) {
    private val monitor = AndroidLanNetworkMonitor(context)
    fun capture(routeKey: String?, host: String): LanSocketRoute? {
        if (routeKey != null) return LanSocketRoute(requireNotNull(monitor.network(routeKey)) { "Selected LAN is no longer available" })
        val address = IpLiteral.parse(host) ?: return null
        val routes = monitor.snapshot().filter { it.prefix.contains(address) }.map { it.interfaceKey }.distinct()
        require(routes.size <= 1) { "Multiple LAN routes match. Select a discovered room endpoint." }
        return routes.singleOrNull()?.let { monitor.network(it) }?.let(::LanSocketRoute)
    }
}
