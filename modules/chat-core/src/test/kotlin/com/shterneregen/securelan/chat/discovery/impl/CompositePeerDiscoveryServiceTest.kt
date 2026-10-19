package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class CompositePeerDiscoveryServiceTest {
    private class Provider(override val id: DiscoveryProviderId, val fail: Boolean = false) : DiscoveryProvider {
        lateinit var emit: (DiscoveryEvent) -> Unit
        var stopped = 0
        override fun start(request: DiscoveryStartRequest, emit: (DiscoveryEvent) -> Unit) {
            this.emit = emit
            if (fail) error("injected")
            emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, ProviderAvailability.RUNNING)))
        }
        override fun updateAdvertisement(value: HostedRoomAdvertisement?) = Unit
        override fun stop() { stopped++ }
    }
    private val listener = object : PeerDiscoveryListener {
        override fun onPeerDiscovered(peer: DiscoveredPeer) = Unit
        override fun onPeerExpired(peer: DiscoveredPeer) = Unit
        override fun onDiscoveryError(message: String, cause: Throwable) = Unit
    }
    private val request = DiscoveryStartRequest(PeerDiscoveryConfig.listenOnly("local", "Local"))
    private fun observation(provider: DiscoveryProviderId) = DiscoveryObservation(ObservationKey(provider, 1, "lan", "room"), "remote", null,
        "Room", listOf(DiscoveryEndpoint(IpLiteral.parse("192.168.1.2")!!, 5050)), 5051, 45_000_000_000L)
    @Test fun `provider failure is isolated and late callbacks cannot revive stopped run`() {
        val udp = Provider(DiscoveryProviderId.UDP_BROADCAST)
        val mdns = Provider(DiscoveryProviderId.MDNS, true)
        val updates = LinkedBlockingQueue<DiscoverySnapshot>()
        CompositePeerDiscoveryService(listOf(udp, mdns)).use { service ->
            service.startSession(request, listener) { updates += it }
            // Provider startup can publish an empty snapshot before the first observation.
            awaitSnapshot(updates) { it.providers.any { state -> state.provider == mdns.id && state.availability == ProviderAvailability.FAILED } }
            udp.emit(DiscoveryEvent.Upsert(observation(udp.id)))
            val result = awaitSnapshot(updates) { it.rooms.isNotEmpty() }
            assertEquals(1, result.rooms.size)
            assertEquals(ProviderAvailability.FAILED, result.providers.single { it.provider == mdns.id }.availability)
            val old = udp.emit
            service.stop()
            old(DiscoveryEvent.Upsert(observation(udp.id)))
            assertTrue(service.detailedSnapshot().rooms.isEmpty())
            service.startSession(request, listener) { updates += it }
            old(DiscoveryEvent.Upsert(observation(udp.id)))
            assertTrue(service.detailedSnapshot().rooms.isEmpty())
        }
        assertTrue(udp.stopped >= 2)
    }

    private fun awaitSnapshot(updates: LinkedBlockingQueue<DiscoverySnapshot>, matches: (DiscoverySnapshot) -> Boolean): DiscoverySnapshot {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) break
            val snapshot = updates.poll(remaining, TimeUnit.NANOSECONDS) ?: break
            if (matches(snapshot)) return snapshot
        }
        throw AssertionError("Expected discovery snapshot was not published")
    }
    @Test fun `closing from snapshot callback does not wait on its own executor`() {
        val udp = Provider(DiscoveryProviderId.UDP_BROADCAST)
        val service = CompositePeerDiscoveryService(listOf(udp))
        val closed = java.util.concurrent.CountDownLatch(1)
        service.startSession(request, listener) { if (it.rooms.isNotEmpty()) { service.stop(); closed.countDown() } }
        udp.emit(DiscoveryEvent.Upsert(observation(udp.id)))
        assertTrue(closed.await(2, TimeUnit.SECONDS))
        assertFalse(service.isRunning())
    }
}
