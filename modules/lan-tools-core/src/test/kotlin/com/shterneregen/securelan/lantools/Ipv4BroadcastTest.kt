package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.common.net.address.IpPrefix
import com.shterneregen.securelan.lantools.wol.Ipv4Broadcast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class Ipv4BroadcastTest {
    @ParameterizedTest
    @CsvSource("192.168.10.42,24,192.168.10.255", "192.168.10.42,23,192.168.11.255",
        "10.20.30.40,16,10.20.255.255", "10.20.30.40,30,10.20.30.43", "10.20.30.40,1,127.255.255.255",
        "172.16.5.42,20,172.16.15.255")
    fun derivesBroadcastFromHostAddressWithoutOctetBoundaryAssumptions(source: String, prefix: Int, expected: String) {
        assertEquals(ip(expected), Ipv4Broadcast.directed(IpPrefix(ip(source), prefix)))
    }

    @Test
    fun refusesUnsupportedPrefixesAndIpv6() {
        for (prefix in listOf(0, 31, 32)) assertNull(Ipv4Broadcast.directed(IpPrefix(ip("10.0.0.1"), prefix)))
        assertNull(Ipv4Broadcast.directed(IpPrefix(ip("fe80::1"), 64)))
    }
}
