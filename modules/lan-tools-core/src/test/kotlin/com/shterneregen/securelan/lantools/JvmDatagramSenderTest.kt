package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class JvmDatagramSenderTest {
    private val route = WakeRoute(network(), ip("192.168.10.255"), 9)
    private class FakeChannel(val write: (ByteArray, InetSocketAddress) -> Int) : SendChannel {
        var closes = 0
        override fun send(payload: ByteArray, target: InetSocketAddress) = write(payload, target)
        override fun close() { closes++ }
    }

    @Test
    fun retriesOnlyBackpressureAndClosesAfterSuccess() = runTest {
        var attempts = 0
        val channel = FakeChannel { bytes, target ->
            assertEquals("192.168.10.255", target.address.hostAddress)
            assertEquals(9, target.port)
            assertEquals(102, bytes.size)
            if (++attempts < 3) 0 else bytes.size
        }
        val sender = JvmDatagramSender(StandardTestDispatcher(testScheduler)) { channel }
        sender.send(route, MagicPacket.create(mac))
        assertEquals(3, attempts)
        assertEquals(20, testScheduler.currentTime)
        assertEquals(1, channel.closes)
    }

    @Test
    fun closesOnIoFailureAndRejectsPartialDatagramsWithoutRetry() = runTest {
        for (write in listOf<(ByteArray, InetSocketAddress) -> Int>({ _, _ -> throw IOException("failed") }, { _, _ -> 1 })) {
            var attempts = 0
            val channel = FakeChannel { bytes, address -> attempts++; write(bytes, address) }
            val sender = JvmDatagramSender(StandardTestDispatcher(testScheduler)) { channel }
            var failure: IOException? = null
            try { sender.send(route, MagicPacket.create(mac)) } catch (error: IOException) { failure = error }
            assertNotNull(failure)
            assertEquals(1, attempts)
            assertEquals(1, channel.closes)
        }
    }

    @Test
    fun closesOnDeadlineAndCallerCancellation() = runTest {
        val channel = FakeChannel { _, _ -> 0 }
        val sender = JvmDatagramSender(StandardTestDispatcher(testScheduler)) { channel }
        assertNull(withTimeoutOrNull(25.milliseconds) { sender.send(route, MagicPacket.create(mac)); true })
        assertEquals(1, channel.closes)
        val operation = launch { sender.send(route, MagicPacket.create(mac)) }
        runCurrent()
        operation.cancelAndJoin()
        assertEquals(2, channel.closes)
    }

    @Test
    fun doesNotOpenResourceForAlreadyCancelledOperation() = runTest {
        val sender = JvmDatagramSender(StandardTestDispatcher(testScheduler)) { error("opened") }
        val operation = launch { currentCoroutineContext().cancel(); sender.send(route, MagicPacket.create(mac)) }
        operation.join()
        assertTrue(operation.isCancelled)
    }

    @Test
    fun sendsActualUdpOnLoopbackWithoutBroadcastingToLan() = runBlocking {
        val local = InetAddress.getByName("127.0.0.1")
        DatagramSocket(InetSocketAddress(local, 0)).use { receiver ->
            receiver.soTimeout = 2_000
            val source = network(address = "127.0.0.1")
            // Test the transport directly; production service policy excludes loopback/unicast targets.
            val actualRoute = WakeRoute(source, ip("127.0.0.1"), receiver.localPort)
            withTimeout(2.seconds) { JvmDatagramSender().send(actualRoute, MagicPacket.create(mac)) }
            val packet = DatagramPacket(ByteArray(512), 512)
            receiver.receive(packet)
            assertEquals(local, packet.address)
            assertArrayEquals(MagicPacket.create(mac), packet.data.copyOf(packet.length))
            // The sending channel has already released its ephemeral source port.
            DatagramSocket(InetSocketAddress(local, packet.port)).use { released -> assertTrue(released.isBound) }
        }
    }
}
