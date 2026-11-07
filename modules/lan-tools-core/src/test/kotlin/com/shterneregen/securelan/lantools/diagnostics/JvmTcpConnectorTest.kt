package com.shterneregen.securelan.lantools.diagnostics

import com.shterneregen.securelan.lantools.ip
import com.shterneregen.securelan.lantools.networks
import com.shterneregen.securelan.lantools.model.LanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class JvmTcpConnectorTest {
    @Test fun connectsAndClosesRealLoopbackSocketWithoutApplicationData() = runBlocking {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { listener ->
            listener.soTimeout = 2000
            val service = DefaultNetworkDiagnosticsService({ error("DNS") }, networks())
            val result = service.tcp(TcpCheckRequest("127.0.0.1", listener.localPort))
            assertTrue(result is LanResult.Success)
            listener.accept().use { socket -> socket.soTimeout = 2000; assertEquals(-1, socket.getInputStream().read()) }
        }
    }

    @Test fun stalledNonblockingConnectClosesOnDeadlineAndCancel() = runTest {
        var closes = 0
        val fake = object : ConnectChannel {
            override fun connect(target: InetSocketAddress) = false
            override fun finishConnect() = false
            override fun close() { closes++ }
        }
        val connector = JvmTcpConnector(StandardTestDispatcher(testScheduler)) { fake }
        assertNull(withTimeoutOrNull(25.milliseconds) { connector.connect(ip("127.0.0.1"), 1234, null); true })
        assertEquals(1, closes)
        val job = launch { connector.connect(ip("127.0.0.1"), 1234, null) }
        runCurrent(); job.cancelAndJoin()
        assertEquals(2, closes)
    }

    @Test fun connectFailureAlsoClosesChannel() = runTest {
        var closed = false
        val connector = JvmTcpConnector(StandardTestDispatcher(testScheduler)) {
            object : ConnectChannel {
                override fun connect(target: InetSocketAddress): Boolean = throw java.net.ConnectException()
                override fun finishConnect() = error("not reached")
                override fun close() { closed = true }
            }
        }
        try { connector.connect(ip("127.0.0.1"), 1234, null); fail("Expected connect error") } catch (_: java.net.ConnectException) { }
        assertTrue(closed)
    }
}
