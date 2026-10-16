package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.common.net.udp.BroadcastAddressResolver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.*
import java.time.Duration
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DiscoveryLifecycleTest {
    @Test fun restartTerminatesOldWorkersAndSendFailureStillPacesAndExpires() {
        val attempts = AtomicInteger()
        val successes = AtomicInteger()
        val errors = AtomicInteger()
        val discovered = CountDownLatch(1)
        val expired = CountDownLatch(1)
        val resolver = BroadcastAddressResolver { Collections.emptyEnumeration() }
        val service = UdpBroadcastPeerDiscoveryService(resolver, { _, packet ->
            attempts.incrementAndGet()
            if (packet.address.isLoopbackAddress) throw java.io.IOException("injected target failure")
            successes.incrementAndGet()
        })
        val port = DatagramSocket(0).use { it.localPort }
        val config = PeerDiscoveryConfig("local", "local", port, 5050, 5051, true, Duration.ofMillis(50), Duration.ofMillis(150))
        val listener = object : PeerDiscoveryListener {
            override fun onPeerDiscovered(peer: DiscoveredPeer) { discovered.countDown() }
            override fun onPeerExpired(peer: DiscoveredPeer) { expired.countDown() }
            override fun onDiscoveryError(message: String, cause: Throwable) { errors.incrementAndGet() }
        }
        val oldThreads = mutableListOf<Thread>()
        try {
            repeat(4) {
                service.start(config, listener)
                oldThreads += Thread.getAllStackTraces().keys.filter { it.name.startsWith("securelan-peer-discovery-") && it.isAlive }
                service.stop()
            }
            oldThreads.distinct().forEach { it.join(2000); assertFalse(it.isAlive) }
            attempts.set(0); errors.set(0)
            service.start(config, listener)
            val payload = DiscoveryMessageCodec.toBytes(DiscoveryMessage("remote", "remote", 5050, 5051))
            DatagramSocket().use { it.send(DatagramPacket(payload, payload.size, InetAddress.getLoopbackAddress(), port)) }
            assertTrue(discovered.await(2, TimeUnit.SECONDS))
            assertTrue(expired.await(2, TimeUnit.SECONDS))
            assertTrue(successes.get() > 0, "other targets must still be attempted")
            assertTrue(attempts.get() < 100, "send failures must not spin")
            assertEquals(1, errors.get(), "repeated errors must be rate limited")
            assertTrue(service.snapshot().isEmpty())
        } finally { service.stop() }
    }
}
