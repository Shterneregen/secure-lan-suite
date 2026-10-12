package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
}
