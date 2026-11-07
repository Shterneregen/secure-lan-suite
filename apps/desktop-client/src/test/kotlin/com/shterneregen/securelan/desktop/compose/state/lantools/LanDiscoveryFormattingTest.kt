package com.shterneregen.securelan.desktop.compose.state.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.lantools.model.LanInterfaceSnapshot
import com.shterneregen.securelan.lantools.model.LanNetworkSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class LanDiscoveryFormattingTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-07T20:00:00Z")

    @Test
    fun formatsRelativeObservationTimesWithoutRawIsoValues() {
        assertEquals("Today, 19:40", formatLanObservedTime(Instant.parse("2026-11-07T19:40:00Z"), now, zone, Locale.ENGLISH))
        assertEquals("Yesterday, 18:05", formatLanObservedTime(Instant.parse("2026-11-06T18:05:00Z"), now, zone, Locale.ENGLISH))
        assertEquals("5 Nov, 09:00", formatLanObservedTime(Instant.parse("2026-11-05T09:00:00Z"), now, zone, Locale.ENGLISH))
    }

    @Test
    fun labelsKnownServicesAndKeepsUnknownTypesTruthful() {
        assertEquals("HTTP", formatLanServiceType("_http._tcp.local."))
        assertEquals("SMB", formatLanServiceType("_smb._tcp.local."))
        assertEquals("_custom._tcp.local.", formatLanServiceType("_custom._tcp.local."))
    }

    @Test
    fun prefersCurrentInterfaceLabelAndFallsBackToRawScope() {
        val binding = NetworkBinding("eth0:192.168.1.42", "eth0", IpLiteral.parse("192.168.1.42")!!, 24)
        val networks = LanNetworkSnapshot(listOf(LanInterfaceSnapshot(binding, "Ethernet", true, null)))
        assertEquals("Ethernet · 192.168.1.42", formatLanNetwork(binding.key, networks))
        assertEquals("missing:192.168.1.99", formatLanNetwork("missing:192.168.1.99", networks))
    }
}
