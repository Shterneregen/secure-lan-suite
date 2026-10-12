package com.shterneregen.securelan.common.net.udp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.util.Collections

class BroadcastAddressResolverTest {
    @Test
    fun shouldAlwaysIncludeLoopbackAndGlobalBroadcast() {
        val resolver = BroadcastAddressResolver { Collections.emptyEnumeration() }

        val addresses = resolver.resolve()

        assertTrue(addresses.contains(InetAddress.getLoopbackAddress()))
        assertTrue(addresses.contains(InetAddress.getByName("255.255.255.255")))
    }

    @Test
    fun shouldHandleNullInterfacesFromProviderSafely() {
        val resolver = BroadcastAddressResolver { null }

        val addresses = resolver.resolve()

        assertTrue(addresses.contains(InetAddress.getLoopbackAddress()))
        assertTrue(addresses.contains(InetAddress.getByName("255.255.255.255")))
    }

    @Test
    fun shouldUseSystemDefaultProviderByDefault() {
        val resolver = BroadcastAddressResolver()

        val addresses = resolver.resolve()

        assertTrue(addresses.contains(InetAddress.getLoopbackAddress()))
        assertTrue(addresses.contains(InetAddress.getByName("255.255.255.255")))
    }
}
