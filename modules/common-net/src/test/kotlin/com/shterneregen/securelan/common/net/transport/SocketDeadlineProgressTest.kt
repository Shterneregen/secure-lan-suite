package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SocketDeadlineProgressTest {
    @Test fun progressingStreamMayOutliveOneInactivityBudget() {
        Socket().use { socket ->
            var chunks = 0
            val stream = object : InputStream() {
                override fun read(): Int = error("bulk only")
                override fun read(bytes: ByteArray): Int {
                    if (chunks++ == 4) return -1
                    Thread.sleep(35)
                    bytes[0] = 42
                    return 1
                }
            }
            val output = ByteArrayOutputStream()
            SocketDeadlines.copyWithProgress(socket, stream, output, 100)
            assertFalse(socket.isClosed)
            assertArrayEquals(byteArrayOf(42, 42, 42, 42), output.toByteArray())
        }
    }

    @Test fun stalledSourceIsClosedByWatchdog() {
        val closed = CountDownLatch(1)
        val socket = object : Socket() { override fun close() { super.close(); closed.countDown() } }
        socket.use {
            val stream = object : InputStream() {
                override fun read(): Int = error("bulk only")
                override fun read(bytes: ByteArray): Int {
                    check(closed.await(2, TimeUnit.SECONDS))
                    throw java.io.IOException("closed")
                }
            }
            assertThrows(java.io.IOException::class.java) { SocketDeadlines.copyWithProgress(socket, stream, ByteArrayOutputStream(), 50) }
            assertTrue(socket.isClosed)
        }
    }
}
