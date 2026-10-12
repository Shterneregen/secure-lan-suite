package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ServerSocket

class ClientSocketFactoryTest {
    @Test
    fun shouldConnectSuccessfullyToActiveEndpoint() {
        ServerSocket(0).use { serverSocket ->
            val endpoint = TransportEndpoint.of("127.0.0.1", serverSocket.localPort)
            val factory = ClientSocketFactory.systemDefault()

            factory.connect(endpoint).use { clientSocket ->
                assertTrue(clientSocket.isConnected)
            }
        }
    }

    @Test
    fun shouldSupportCustomConnectTimeout() {
        ServerSocket(0).use { serverSocket ->
            val endpoint = TransportEndpoint.of("127.0.0.1", serverSocket.localPort)
            val factory = ClientSocketFactory.systemDefault(2_000)

            factory.connect(endpoint).use { clientSocket ->
                assertTrue(clientSocket.isConnected)
            }
        }
    }

    @Test
    fun shouldRejectNonPositiveTimeout() {
        assertThrows(IllegalArgumentException::class.java) {
            ClientSocketFactory.systemDefault(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ClientSocketFactory.systemDefault(-1)
        }
    }

    @Test
    fun shouldThrowOnConnectionFailure() {
        val endpoint = TransportEndpoint.of("127.0.0.1", 1)
        val factory = ClientSocketFactory.systemDefault(500)

        assertThrows(IOException::class.java) {
            factory.connect(endpoint)
        }
    }
}
