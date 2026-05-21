package com.shterneregen.securelan.chat.discovery.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DiscoveryMessageCodecTest {
    @Test
    fun shouldRoundTripDiscoveryMessageWithEncodedText() {
        val message = DiscoveryMessage("peer|id", "Alice 🔐", 5050, 5051)

        val decoded = DiscoveryMessageCodec.decode(DiscoveryMessageCodec.encode(message))

        assertEquals(message, decoded)
    }

    @Test
    fun shouldRejectUnsupportedPayload() {
        assertThrows(IllegalArgumentException::class.java) { DiscoveryMessageCodec.decode("wrong|payload") }
    }

    @Test
    fun shouldRejectInvalidPorts() {
        val payload = "SECURELAN_DISCOVERY_V1|cGVlcg|QWxpY2U|0|5051"

        assertThrows(IllegalArgumentException::class.java) { DiscoveryMessageCodec.decode(payload) }
    }
}
