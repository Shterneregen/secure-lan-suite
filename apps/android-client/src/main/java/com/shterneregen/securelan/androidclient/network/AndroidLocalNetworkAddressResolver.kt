package com.shterneregen.securelan.androidclient.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

internal data class LocalAddressCandidate(
    val address: String,
    val interfaceName: String,
    val siteLocal: Boolean,
    val loopback: Boolean = false,
    val linkLocal: Boolean = false,
    val multicast: Boolean = false,
    val anyLocal: Boolean = false,
    val activeLan: Boolean = false,
)

internal class AndroidLocalNetworkAddressResolver(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    fun resolve(): List<String> {
        val activeLanAddresses = resolveActiveLanAddresses()
        val candidates = runCatching { collectCandidates(activeLanAddresses) }.getOrDefault(emptyList())
        return selectLocalRoomAddresses(candidates)
    }

    private fun resolveActiveLanAddresses(): Set<String> = runCatching {
        val activeNetwork = connectivityManager.activeNetwork ?: return@runCatching emptySet()
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return@runCatching emptySet()
        val hasPhysicalLanTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        if (!hasPhysicalLanTransport || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
            return@runCatching emptySet()
        }
        connectivityManager.getLinkProperties(activeNetwork)
            ?.linkAddresses
            ?.asSequence()
            ?.map { it.address }
            ?.filterIsInstance<Inet4Address>()
            ?.mapNotNull { it.hostAddress }
            ?.toSet()
            .orEmpty()
    }.getOrDefault(emptySet())

    private fun collectCandidates(activeLanAddresses: Set<String>): List<LocalAddressCandidate> {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        return buildList {
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val interfaceName = networkInterface.name.orEmpty()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address !is Inet4Address) continue
                    val hostAddress = address.hostAddress ?: continue
                    add(
                        LocalAddressCandidate(
                            address = hostAddress,
                            interfaceName = interfaceName,
                            siteLocal = address.isSiteLocalAddress,
                            loopback = address.isLoopbackAddress,
                            linkLocal = address.isLinkLocalAddress,
                            multicast = address.isMulticastAddress,
                            anyLocal = address.isAnyLocalAddress,
                            activeLan = hostAddress in activeLanAddresses,
                        ),
                    )
                }
            }
        }
    }
}

internal fun selectLocalRoomAddresses(candidates: List<LocalAddressCandidate>): List<String> = candidates
    .asSequence()
    .filter { candidate ->
        candidate.siteLocal &&
            !candidate.loopback &&
            !candidate.linkLocal &&
            !candidate.multicast &&
            !candidate.anyLocal &&
            isIpv4Literal(candidate.address) &&
            !isMobileInterface(candidate.interfaceName)
    }
    .sortedWith(
        compareBy<LocalAddressCandidate> { candidate -> interfacePriority(candidate) }
            .thenBy { candidate -> ipv4SortKey(candidate.address) }
            .thenBy { candidate -> candidate.interfaceName.lowercase() },
    )
    .distinctBy { candidate -> candidate.address }
    .map { candidate -> candidate.address }
    .toList()

private fun interfacePriority(candidate: LocalAddressCandidate): Int = when {
    candidate.activeLan -> 0
    isVpnInterface(candidate.interfaceName) -> 3
    isPhysicalLanInterface(candidate.interfaceName) -> 1
    else -> 2
}

private fun isPhysicalLanInterface(interfaceName: String): Boolean {
    val normalized = interfaceName.lowercase()
    return PHYSICAL_LAN_INTERFACE_MARKERS.any(normalized::contains)
}

private fun isVpnInterface(interfaceName: String): Boolean {
    val normalized = interfaceName.lowercase()
    return VPN_INTERFACE_MARKERS.any(normalized::contains)
}

private fun isMobileInterface(interfaceName: String): Boolean {
    val normalized = interfaceName.lowercase()
    return MOBILE_INTERFACE_MARKERS.any(normalized::contains)
}

private fun isIpv4Literal(address: String): Boolean {
    val octets = address.split('.')
    return octets.size == 4 && octets.all { octet ->
        octet.isNotEmpty() && octet.all(Char::isDigit) && octet.toIntOrNull() in 0..255
    }
}

private fun ipv4SortKey(address: String): Long = address.split('.').fold(0L) { result, octet ->
    (result shl 8) or (octet.toLongOrNull() ?: 0L)
}

private val PHYSICAL_LAN_INTERFACE_MARKERS = listOf("wlan", "wifi", "wi-fi", "softap", "ap", "eth", "rndis", "usb")
private val VPN_INTERFACE_MARKERS = listOf("tun", "tap", "wireguard", "wg", "vpn", "ppp")
private val MOBILE_INTERFACE_MARKERS = listOf("rmnet", "ccmni", "pdp")
