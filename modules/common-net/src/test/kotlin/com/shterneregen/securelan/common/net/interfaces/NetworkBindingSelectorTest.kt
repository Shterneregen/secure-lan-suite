package com.shterneregen.securelan.common.net.interfaces

import com.shterneregen.securelan.common.net.address.IpLiteral
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NetworkBindingSelectorTest {
    @Test fun `default policy excludes VPN virtual and down networks`() {
        val lan = NetworkBinding("lan", "eth0", IpLiteral.parse("192.168.1.2")!!, 24)
        val vpn = lan.copy(key = "vpn", interfaceKey = "tun0", kind = NetworkKind.VPN)
        assertEquals(listOf(lan), NetworkBindingSelector.select(listOf(lan, vpn, lan.copy(key = "down", up = false), lan.copy(key = "virtual", kind = NetworkKind.VIRTUAL))))
        assertEquals(2, NetworkBindingSelector.select(listOf(lan, vpn), DiscoveryNetworkPolicy(setOf("tun0"))).size)
    }
}
