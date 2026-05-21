package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TransportEndpointTest {
    @Test
    fun shouldTrimHostAndExposeSocketAddress() {
        val endpoint = TransportEndpoint.of(" localhost ", 5050)

        assertEquals("localhost", endpoint.host())
        assertEquals(5050, endpoint.port())
        assertEquals("localhost", endpoint.toSocketAddress().hostString)
        assertEquals(5050, endpoint.toSocketAddress().port)
    }

    @Test
    fun shouldRejectBlankHost() {
        assertThrows(IllegalArgumentException::class.java) { TransportEndpoint.of(" ", 5050) }
    }

    @Test
    fun shouldRejectInvalidPort() {
        assertThrows(IllegalArgumentException::class.java) { TransportEndpoint.of("localhost", 0) }
        assertThrows(IllegalArgumentException::class.java) { TransportEndpoint.of("localhost", 65_536) }
    }
}
