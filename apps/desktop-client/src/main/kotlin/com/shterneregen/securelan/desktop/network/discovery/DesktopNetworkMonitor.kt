package com.shterneregen.securelan.desktop.network.discovery

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import java.net.Inet6Address
import java.net.NetworkInterface

/** Polled by the discovery IO owner only while a discovery run is active. Classification is heuristic. */
class DesktopNetworkMonitor {
    fun snapshot(): List<NetworkBinding> = NetworkInterface.getNetworkInterfaces().toList().flatMap { nic ->
        if (!nic.isUp || nic.isLoopback) return@flatMap emptyList()
        val name = "${nic.name} ${nic.displayName}".lowercase()
        val kind = when {
            nic.isPointToPoint || listOf("vpn", "tun", "tap", "wireguard", "tailscale", "ppp").any(name::contains) -> NetworkKind.VPN
            nic.isVirtual || listOf("virtual", "vmware", "hyper-v", "vbox", "wsl", "docker", "vethernet").any(name::contains) -> NetworkKind.VIRTUAL
            else -> NetworkKind.LAN
        }
        nic.interfaceAddresses.mapNotNull { binding ->
            val address = binding.address
            val ip = IpLiteral.parse(address.hostAddress.substringBefore('%')) ?: return@mapNotNull null
            NetworkBinding("${nic.name}:${ip.value}", nic.name, ip, binding.networkPrefixLength.toInt(), kind,
                multicast = nic.supportsMulticast(), scopeId = (address as? Inet6Address)?.scopeId?.takeIf { it > 0 })
        }
    }
}
