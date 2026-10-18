package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiscoveryRegistryTest {
    private val pid = "2da1e450-798a-401f-990b-cb1cc4f61d92"
    private val rid = "ab817e84-78af-40b4-994d-41bfa5ff20fb"
    private fun observation(provider: DiscoveryProviderId, host: String = "192.168.1.2", room: String? = if (provider == DiscoveryProviderId.MDNS) rid else null) =
        DiscoveryObservation(ObservationKey(provider, 1, "network", host), pid, room, "Room", listOf(DiscoveryEndpoint(IpLiteral.parse(host)!!, 5050)), 5051,
            if (provider == DiscoveryProviderId.UDP_BROADCAST) 45_000_000_000L else null)
    @Test fun `merging preserves row identity and losing a source keeps room`() {
        var now = 0L
        val registry = DiscoveryRegistry { now }
        val udp = observation(DiscoveryProviderId.UDP_BROADCAST)
        val mdns = observation(DiscoveryProviderId.MDNS)
        val row = registry.accept(DiscoveryEvent.Upsert(udp)).rooms.single().rowId
        assertEquals(row, registry.accept(DiscoveryEvent.Upsert(mdns)).rooms.single().rowId)
        now = 46_000_000_000L
        assertEquals(setOf(DiscoveryProviderId.MDNS), registry.snapshot().rooms.single().sources)
        assertEquals(row, registry.snapshot().rooms.single().rowId)
        assertTrue(registry.accept(DiscoveryEvent.Remove(mdns.key)).rooms.isEmpty())
    }
    @Test fun `UDP endpoint expiry and conflicting room lifetimes are independent`() {
        val registry = DiscoveryRegistry()
        val first = observation(DiscoveryProviderId.UDP_BROADCAST)
        val second = observation(DiscoveryProviderId.UDP_BROADCAST, "192.168.2.2")
        registry.accept(DiscoveryEvent.Upsert(first)); registry.accept(DiscoveryEvent.Upsert(second))
        assertEquals(2, registry.snapshot().rooms.single().endpoints.size)
        assertEquals("192.168.2.2", registry.accept(DiscoveryEvent.Remove(first.key)).rooms.single().endpoints.single().host)
        registry.accept(DiscoveryEvent.Upsert(observation(DiscoveryProviderId.MDNS, "192.168.2.2")))
        registry.accept(DiscoveryEvent.Upsert(observation(DiscoveryProviderId.MDNS, "192.168.2.3", "bb817e84-78af-40b4-994d-41bfa5ff20fb")))
        assertEquals(3, registry.snapshot().rooms.size)
        assertTrue(registry.snapshot().rooms.all { it.metadataConflict })
    }
    @Test fun `DNS observations survive silence until source loss`() {
        var now = 0L
        val registry = DiscoveryRegistry { now }
        registry.accept(DiscoveryEvent.Upsert(observation(DiscoveryProviderId.MDNS)))
        now = 999_000_000_000L
        assertEquals(1, registry.snapshot().rooms.size)
        registry.accept(DiscoveryEvent.ProviderState(DiscoveryProviderState(DiscoveryProviderId.UDP_BROADCAST, ProviderAvailability.FAILED)))
        assertEquals(1, registry.snapshot().rooms.size)
        assertTrue(registry.accept(DiscoveryEvent.ResetSource(DiscoveryProviderId.MDNS)).rooms.isEmpty())
    }
    @Test fun `room and endpoint flood budgets remain bounded`() {
        var now = 0L
        val registry = DiscoveryRegistry { now }
        repeat(400) { index ->
            now += 20_000_000L
            val value = observation(DiscoveryProviderId.UDP_BROADCAST).copy(peerId = "peer-$index",
                key = ObservationKey(DiscoveryProviderId.UDP_BROADCAST, 1, "lan", "record-$index"))
            registry.accept(DiscoveryEvent.Upsert(value))
        }
        assertEquals(256, registry.snapshot().rooms.size)
        val single = DiscoveryRegistry()
        repeat(20) { index -> single.accept(DiscoveryEvent.Upsert(observation(DiscoveryProviderId.UDP_BROADCAST, "192.168.1.${index + 1}"))) }
        assertEquals(8, single.snapshot().rooms.single().endpoints.size)
    }
}
