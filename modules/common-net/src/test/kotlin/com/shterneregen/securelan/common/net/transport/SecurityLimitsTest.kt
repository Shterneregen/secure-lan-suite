package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SecurityLimitsTest {
    private fun pair(action: (Socket, Socket) -> Unit) = ServerSocket(0).use { listener ->
        Socket("127.0.0.1", listener.localPort).use { writer -> listener.accept().use { reader ->
            writer.soTimeout = 1000; reader.soTimeout = 1000
            action(writer, reader)
        } }
    }

    @Test fun oversizedLineIsRejectedWithoutWaitingForNewline() = pair { writer, reader ->
        writer.getOutputStream().write("123456789".toByteArray())
        writer.getOutputStream().flush()
        assertThrows(IOException::class.java) { LineTextChannel(reader, 8).readLine() }
    }

    @Test fun advertisedHugeFrameAndUtfFieldAreRejectedBeforeReadingBody() = pair { writer, reader ->
        DataOutputStream(writer.getOutputStream()).apply { writeInt(Int.MAX_VALUE); flush() }
        assertThrows(IOException::class.java) { LengthPrefixedFrameChannel(reader).readFrame() }
        DataOutputStream(writer.getOutputStream()).apply { writeShort(65_535); flush() }
        assertThrows(IOException::class.java) { LengthPrefixedFrameChannel(reader).readUtf() }
    }

    @Test fun absoluteDeadlineClosesAStalledSocket() = pair { _, reader ->
        assertThrows(IOException::class.java) {
            SocketDeadlines.within(reader, 50) { reader.getInputStream().read() }
        }
        assertTrue(reader.isClosed)
    }

    @Test fun excessConnectionsAreClosedWithoutCreatingAnotherWorker() {
        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        TcpServer("bounded-test", ServerSocketFactory.systemDefault(), 1).use { server ->
            server.start(0, { socket ->
                started.countDown()
                try { socket.getInputStream().read() } finally { done.countDown() }
            }, { _, _ -> })
            Socket("127.0.0.1", server.port()).use {
                assertTrue(started.await(1, TimeUnit.SECONDS))
                Socket("127.0.0.1", server.port()).use { excess ->
                    excess.soTimeout = 1000
                    assertEquals(-1, excess.getInputStream().read())
                }
            }
            assertTrue(done.await(1, TimeUnit.SECONDS))
        }
    }
}
