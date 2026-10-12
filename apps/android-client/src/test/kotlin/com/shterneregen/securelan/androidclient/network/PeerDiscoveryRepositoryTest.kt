package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.DiscoveryCodec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.BindException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class PeerDiscoveryRepositoryTest {
    @Test(timeout = 10_000)
    fun cancellingDiscoveryClosesSocketWithoutFailure() = runBlocking {
        val port = DatagramSocket(0).use { it.localPort }
        val received = CompletableDeferred<Unit>()
        val discoveryJob = launch {
            PeerDiscoveryRepository(port).discoverPeers().collect { peer ->
                assertEquals("peer", peer.peerId)
                assertEquals("127.0.0.1", peer.host)
                received.complete(Unit)
            }
        }
        try {
            DatagramSocket().use { sender ->
                withTimeout(3_000) {
                    while (!received.isCompleted) {
                        // A malformed datagram must not stop the receiver before a valid announcement.
                        for (payload in listOf("invalid".toByteArray(), DiscoveryCodec.encode("peer", "alice", 5050, 5051))) {
                            sender.send(DatagramPacket(payload, payload.size, InetAddress.getLoopbackAddress(), port))
                        }
                        delay(10)
                    }
                }
            }
        } finally {
            discoveryJob.cancelAndJoin()
        }
        DatagramSocket(port).use { assertEquals(port, it.localPort) }
    }

    @Test(timeout = 10_000)
    fun occupiedPortFailsCollectorInsteadOfLeavingDiscoveryActive() = runBlocking {
        DatagramSocket(0).use { occupied ->
            val result = runCatching {
                withTimeout(3_000) { PeerDiscoveryRepository(occupied.localPort).discoverPeers().first() }
            }
            assertTrue(result.exceptionOrNull() is BindException)
        }
    }
}
