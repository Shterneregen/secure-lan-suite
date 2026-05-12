package com.shterneregen.securelan.chat.discovery.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscoveryMessageCodecTest {
    @Test
    void shouldRoundTripDiscoveryMessageWithEncodedText() {
        DiscoveryMessage message = new DiscoveryMessage("peer|id", "Alice 🔐", 5050, 5051);

        DiscoveryMessage decoded = DiscoveryMessageCodec.decode(DiscoveryMessageCodec.encode(message));

        assertEquals(message, decoded);
    }

    @Test
    void shouldRejectUnsupportedPayload() {
        assertThrows(IllegalArgumentException.class, () -> DiscoveryMessageCodec.decode("wrong|payload"));
    }

    @Test
    void shouldRejectInvalidPorts() {
        String payload = "SECURELAN_DISCOVERY_V1|cGVlcg|QWxpY2U|0|5051";

        assertThrows(IllegalArgumentException.class, () -> DiscoveryMessageCodec.decode(payload));
    }
}
