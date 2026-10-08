package com.shterneregen.securelan.androidclient.network

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidLocalNetworkAddressResolverTest {
    @Test
    fun `filters addresses that cannot identify a LAN room`() {
        val addresses = selectLocalRoomAddresses(
            listOf(
                candidate("127.0.0.1", "lo", loopback = true),
                candidate("0.0.0.0", "wlan0", anyLocal = true),
                candidate("169.254.10.2", "wlan0", siteLocal = false, linkLocal = true),
                candidate("224.0.0.1", "wlan0", siteLocal = false, multicast = true),
                candidate("fe80::1", "wlan0"),
                candidate("192.168.1.24", "wlan0"),
            ),
        )

        assertEquals(listOf("192.168.1.24"), addresses)
    }

    @Test
    fun `prefers active physical LAN and keeps VPN as fallback`() {
        val addresses = selectLocalRoomAddresses(
            listOf(
                candidate("10.8.0.2", "tun0"),
                candidate("192.168.1.24", "wlan0", activeLan = true),
                candidate("192.168.42.1", "ap0"),
            ),
        )

        assertEquals(listOf("192.168.1.24", "192.168.42.1", "10.8.0.2"), addresses)
    }

    @Test
    fun `keeps hotspot address and removes mobile interface`() {
        val addresses = selectLocalRoomAddresses(
            listOf(
                candidate("10.10.0.5", "rmnet_data0"),
                candidate("192.168.232.1", "softap0"),
            ),
        )

        assertEquals(listOf("192.168.232.1"), addresses)
    }

    @Test
    fun `deduplicates addresses and sorts them deterministically`() {
        val addresses = selectLocalRoomAddresses(
            listOf(
                candidate("192.168.1.20", "eth0"),
                candidate("192.168.1.3", "wlan0"),
                candidate("192.168.1.3", "wlan1"),
                candidate("10.0.0.8", "wlan0"),
            ),
        )

        assertEquals(listOf("10.0.0.8", "192.168.1.3", "192.168.1.20"), addresses)
    }

    @Test
    fun `returns empty list when no private IPv4 address is available`() {
        val addresses = selectLocalRoomAddresses(
            listOf(
                candidate("203.0.113.15", "wlan0", siteLocal = false),
                candidate("100.64.0.2", "rmnet0", siteLocal = false),
            ),
        )

        assertEquals(emptyList<String>(), addresses)
    }

    private fun candidate(
        address: String,
        interfaceName: String,
        siteLocal: Boolean = true,
        loopback: Boolean = false,
        linkLocal: Boolean = false,
        multicast: Boolean = false,
        anyLocal: Boolean = false,
        activeLan: Boolean = false,
    ) = LocalAddressCandidate(
        address = address,
        interfaceName = interfaceName,
        siteLocal = siteLocal,
        loopback = loopback,
        linkLocal = linkLocal,
        multicast = multicast,
        anyLocal = anyLocal,
        activeLan = activeLan,
    )
}
