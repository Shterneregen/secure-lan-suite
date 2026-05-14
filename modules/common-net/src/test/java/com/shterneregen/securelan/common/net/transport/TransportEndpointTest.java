package com.shterneregen.securelan.common.net.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransportEndpointTest {
    @Test
    void shouldTrimHostAndExposeSocketAddress() {
        TransportEndpoint endpoint = TransportEndpoint.of(" localhost ", 5050);

        assertEquals("localhost", endpoint.host());
        assertEquals(5050, endpoint.port());
        assertEquals("localhost", endpoint.toSocketAddress().getHostString());
        assertEquals(5050, endpoint.toSocketAddress().getPort());
    }

    @Test
    void shouldRejectBlankHost() {
        assertThrows(IllegalArgumentException.class, () -> TransportEndpoint.of(" ", 5050));
    }

    @Test
    void shouldRejectInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> TransportEndpoint.of("localhost", 0));
        assertThrows(IllegalArgumentException.class, () -> TransportEndpoint.of("localhost", 65_536));
    }
}
