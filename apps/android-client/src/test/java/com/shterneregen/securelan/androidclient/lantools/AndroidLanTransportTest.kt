package com.shterneregen.securelan.androidclient.lantools

import com.shterneregen.securelan.androidclient.lantools.network.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.WakeRoute
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.net.*

class AndroidLanTransportTest {
    private val loopback = InetAddress.getLoopbackAddress()
    private fun platform() = FakeLanPlatform(listOf(link("loopback", "test", loopback.hostAddress!!)))

    @Test fun tcpBindsBeforeConnectingAndClosesWithoutSendingApplicationData() = runBlocking {
        withTimeout(3_000) {
            val platform = platform()
            var bound: Socket? = null
            platform.socketBinding = object : LanSocketBinding {
                override fun bind(socket: Socket) { assertFalse(socket.isConnected); bound = socket }
                override fun bind(socket: DatagramSocket) = Unit
            }
            ServerSocket(0, 1, loopback).use { server ->
                server.soTimeout = 2000
                AndroidTcpConnector(platform).connect(ip(loopback.hostAddress!!), server.localPort, ip(loopback.hostAddress!!))
                server.accept().use { accepted -> accepted.soTimeout = 1000; assertEquals(-1, accepted.getInputStream().read()) }
                assertTrue(bound!!.isClosed)
            }
        }
    }
    @Test fun udpUsesSelectedSocketAndReleasesItAfterOneDatagram() = runBlocking {
        withTimeout(3_000) {
            val platform = platform(); var bound: DatagramSocket? = null
            platform.socketBinding = object : LanSocketBinding {
                override fun bind(socket: Socket) = Unit
                override fun bind(socket: DatagramSocket) { assertFalse(socket.isBound); bound = socket }
            }
            DatagramSocket(0, loopback).use { receiver ->
                receiver.soTimeout = 2000
                val link = platform.entries.single()
                val route = WakeRoute(LanInterfaceSnapshot(link.binding, "test", true, null), ip(loopback.hostAddress!!), receiver.localPort)
                val payload = byteArrayOf(1, 2, 3)
                AndroidDatagramSender(platform).send(route, payload)
                val packet = DatagramPacket(ByteArray(32), 32); receiver.receive(packet)
                assertArrayEquals(payload, packet.data.copyOf(packet.length)); assertTrue(bound!!.isClosed)
            }
        }
    }
    @Test fun missingOrAmbiguousSourceNeverUsesOsFallback() = runBlocking {
        val platform = platform(); platform.socketBinding = null
        try { AndroidTcpConnector(platform).connect(ip("127.0.0.1"), 9, ip(loopback.hostAddress!!)); fail() } catch (_: BindException) { }
        platform.entries = platform.entries + platform.entries.single().copy(binding = platform.entries.single().binding.copy(key = "second", interfaceKey = "second"))
        try { AndroidTcpConnector(platform).connect(ip("127.0.0.1"), 9, ip(loopback.hostAddress!!)); fail() } catch (_: BindException) { }
    }
    @Test fun transportClosesSocketWhenPlatformBindingFails() = runBlocking {
        val platform = platform(); var bound: Socket? = null
        platform.socketBinding = object : LanSocketBinding {
            override fun bind(socket: Socket) { bound = socket; throw SecurityException() }
            override fun bind(socket: DatagramSocket) = Unit
        }
        try { AndroidTcpConnector(platform).connect(ip(loopback.hostAddress!!), 9, ip(loopback.hostAddress!!)); fail() } catch (_: SecurityException) { }
        assertTrue(bound!!.isClosed)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun metadataIsBoundedAndDoesNotInventMissingFacts() = runTest {
        val platform = FakeLanPlatform(listOf(link().copy(broadcast = null, gateways = emptyList(), dns = emptyList(), metadataAvailable = false)))
        val provider = AndroidLanNetworkInfoProvider(platform, StandardTestDispatcher(testScheduler))
        val result = provider.diagnostics() as LanResult.Success
        assertTrue(result.value.interfaces.single().gateways.isEmpty())
        assertFalse(result.value.interfaces.single().source.broadcastCapable)
        platform.entries = List(513) { link() }
        assertEquals(LanError.RESOURCE_LIMIT, (provider.snapshot() as LanResult.Failure).code)
    }
}
