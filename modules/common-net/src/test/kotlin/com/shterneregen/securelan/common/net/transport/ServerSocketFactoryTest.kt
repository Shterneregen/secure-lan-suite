package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.BindException
import java.net.Socket

class ServerSocketFactoryTest {
    @Test
    fun shouldOpenBoundServerSocket() {
        val factory = ServerSocketFactory.systemDefault()
        factory.open(0).use { serverSocket ->
            assertTrue(serverSocket.isBound)
            assertTrue(serverSocket.localPort > 0)
        }
    }

    @Test
    fun shouldRejectInvalidPort() {
        val factory = ServerSocketFactory.systemDefault()
        assertThrows(IllegalArgumentException::class.java) {
            factory.open(-1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            factory.open(65_536)
        }
    }

    @Test
    fun shouldRebindAfterClosingAnAcceptedConnection() {
        val factory = ServerSocketFactory.systemDefault()
        val port = factory.open(0).use { listener ->
            assertTrue(listener.reuseAddress)
            Socket("127.0.0.1", listener.localPort).use { client ->
                client.soTimeout = 2_000
                listener.accept().close()
                assertEquals(-1, client.getInputStream().read())
            }
            listener.localPort
        }
        factory.open(port).use { listener ->
            Socket("127.0.0.1", port).use {
                listener.accept().use { accepted -> assertTrue(accepted.isConnected) }
            }
        }
    }

    @Test
    fun shouldRejectAnAlreadyListeningPort() {
        val factory = ServerSocketFactory.systemDefault()
        factory.open(0).use { listener ->
            assertThrows(BindException::class.java) { factory.open(listener.localPort).close() }
        }
    }
}
