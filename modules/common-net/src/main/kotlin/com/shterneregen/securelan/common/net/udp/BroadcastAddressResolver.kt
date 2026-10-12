package com.shterneregen.securelan.common.net.udp

import java.net.Inet4Address
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Collections
import java.util.Enumeration

/**
 * Resolves local and broadcast IPv4 addresses suitable for UDP peer discovery announcements.
 *
 * Always includes:
 * - The local loopback address ([InetAddress.getLoopbackAddress])
 * - The global limited broadcast address (`255.255.255.255`)
 * - Directed subnet broadcast addresses from all active, non-loopback network interfaces
 *
 * @param networkInterfaceProvider provider for active system network interfaces.
 */
class BroadcastAddressResolver(
    private val networkInterfaceProvider: NetworkInterfaceProvider,
) {
    /**
     * Creates a resolver using the default system [NetworkInterfaceProvider].
     */
    constructor() : this(NetworkInterfaceProvider.systemDefault())

    /**
     * Resolves all applicable broadcast and loopback IPv4 addresses.
     *
     * @return a distinct list of resolved [InetAddress] targets.
     * @throws SocketException if an error occurs while accessing network interface information.
     * @throws UnknownHostException if standard broadcast addresses cannot be resolved.
     */
    @Throws(SocketException::class, UnknownHostException::class)
    fun resolve(): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        result += InetAddress.getLoopbackAddress()
        result += InetAddress.getByName("255.255.255.255")
        val interfaces = networkInterfaceProvider.networkInterfaces() ?: Collections.emptyEnumeration()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            try {
                if (!networkInterface.isUp || networkInterface.isLoopback) {
                    continue
                }
                networkInterface.interfaceAddresses.asSequence()
                    .filter(::isIpv4Address)
                    .mapNotNull(InterfaceAddress::getBroadcast)
                    .forEach(result::add)
            } catch (_: SocketException) {
                // Ignore transient or unqueryable interfaces during discovery
            }
        }
        return result.distinct()
    }

    /**
     * Abstraction over system network interface enumeration for testing and decoupling.
     */
    fun interface NetworkInterfaceProvider {
        /**
         * Returns an [Enumeration] of available [NetworkInterface] instances, or `null` if none found.
         *
         * @return an enumeration of network interfaces, or `null`.
         * @throws SocketException if an I/O error occurs while retrieving interfaces.
         */
        @Throws(SocketException::class)
        fun networkInterfaces(): Enumeration<NetworkInterface>?

        companion object {
            /**
             * Returns the default provider querying system [NetworkInterface.getNetworkInterfaces].
             */
            @JvmStatic
            fun systemDefault(): NetworkInterfaceProvider =
                NetworkInterfaceProvider { NetworkInterface.getNetworkInterfaces() ?: Collections.emptyEnumeration() }
        }
    }

    companion object {
        private fun isIpv4Address(address: InterfaceAddress): Boolean = address.address is Inet4Address
    }
}
