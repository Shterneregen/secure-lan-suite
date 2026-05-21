package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class SocketCloseTest {
    @Test
    fun shouldCloseQuietly() {
        val closed = AtomicBoolean(false)

        SocketClose.closeQuietly { closed.set(true) }

        assertTrue(closed.get())
    }

    @Test
    fun shouldSuppressCloseExceptionsAndAcceptNull() {
        assertDoesNotThrow { SocketClose.closeQuietly(null) }
        assertDoesNotThrow {
            SocketClose.closeQuietly {
                throw IOException("boom")
            }
        }
    }
}
