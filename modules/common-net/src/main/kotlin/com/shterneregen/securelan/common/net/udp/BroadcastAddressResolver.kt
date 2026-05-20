package com.shterneregen.securelan.common.net.udp

import java.net.Inet4Address
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Enumeration
import java.util.Objects

class BroadcastAddressResolver(
    networkInterfaceProvider: NetworkInterfaceProvider,
) {
    private val networkInterfaceProvider: NetworkInterfaceProvider =
        Objects.requireNonNull(networkInterfaceProvider, "networkInterfaceProvider")

    constructor() : this(NetworkInterfaceProvider.systemDefault())

    @Throws(SocketException::class, UnknownHostException::class)
    fun resolve(): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        result += InetAddress.getLoopbackAddress()
        result += InetAddress.getByName("255.255.255.255")
        val interfaces = networkInterfaceProvider.networkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback) {
                continue
            }
            networkInterface.interfaceAddresses.asSequence()
                .filter(BroadcastAddressResolver::isIpv4Address)
                .mapNotNull(InterfaceAddress::getBroadcast)
                .forEach(result::add)
        }
        return result.distinct()
    }

    fun interface NetworkInterfaceProvider {
        @Throws(SocketException::class)
        fun networkInterfaces(): Enumeration<NetworkInterface>

        companion object {
            @JvmStatic
            fun systemDefault(): NetworkInterfaceProvider = NetworkInterfaceProvider { NetworkInterface.getNetworkInterfaces() }
        }
    }

    companion object {
        private fun isIpv4Address(address: InterfaceAddress): Boolean = address.address is Inet4Address
    }
}
