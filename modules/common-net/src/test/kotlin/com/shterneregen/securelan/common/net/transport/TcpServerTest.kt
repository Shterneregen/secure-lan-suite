package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TcpServerTest {
    @Test
    fun closeDuringAcquisitionRollsBackOnlyTheCancelledRun() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val oldSocket = java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val server = TcpServer("acquisition-race", ServerSocketFactory { port ->
            if (calls.getAndIncrement() == 0) {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                oldSocket
            } else java.net.ServerSocket(port)
        })
        val thread = Thread { server.start(0, { it.close() }, { _, _ -> }) }
        try {
            thread.start()
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            server.close()
            assertFalse(server.isRunning())
            assertEquals(-1, server.port())
            server.start(0, { it.close() }, { _, _ -> })
            val newPort = server.port()
            release.countDown()
            thread.join(5_000)
            assertFalse(thread.isAlive)
            assertTrue(oldSocket.isClosed)
            assertTrue(server.isRunning())
            assertEquals(newPort, server.port())
            Socket("127.0.0.1", newPort).use { assertTrue(it.isConnected) }
        } finally { release.countDown(); server.close(); oldSocket.close(); thread.join(5_000) }
    }

    @Test
    fun shouldStartAcceptAndDispatchClientConnection() {
        val server = TcpServer("test-tcp-server")
        val handled = CountDownLatch(1)
        val errors = mutableListOf<String>()

        server.use {
            server.start(
                0,
                { socket ->
                    socket.use {
                        handled.countDown()
                    }
                },
                { message, _ -> errors.add(message) },
            )

            assertTrue(server.isRunning())
            assertTrue(server.localPort() > 0)
            assertEquals(server.localPort(), server.port())

            // Connect client
            Socket("127.0.0.1", server.localPort()).use { client ->
                assertTrue(client.isConnected)
            }

            assertTrue(handled.await(5, TimeUnit.SECONDS))
            assertTrue(errors.isEmpty())
        }

        assertFalse(server.isRunning())
    }

    @Test
    fun shouldReportHandlerErrorsViaErrorHandler() {
        val server = TcpServer("error-tcp-server")
        val errorReported = CountDownLatch(1)

        server.use {
            server.start(
                0,
                { socket ->
                    socket.use {
                        throw RuntimeException("handler boom")
                    }
                },
                { _, _ -> errorReported.countDown() },
            )

            Socket("127.0.0.1", server.localPort()).use { }

            assertTrue(errorReported.await(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun shouldIgnoreDuplicateStart() {
        val server = TcpServer("dup-tcp-server")
        server.use {
            server.start(0, { it.close() }, { _, _ -> })
            val port = server.localPort()
            // Second start call should be ignored
            server.start(0, { it.close() }, { _, _ -> })
            assertEquals(port, server.localPort())
        }
    }

    @Test
    fun shouldRejectInvalidPort() {
        val server = TcpServer("invalid-port-server")
        server.use {
            assertThrows(IllegalArgumentException::class.java) {
                server.start(-1, { }, { _, _ -> })
            }
            assertThrows(IllegalArgumentException::class.java) {
                server.start(65_536, { }, { _, _ -> })
            }
        }
    }
}
