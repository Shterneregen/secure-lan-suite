package com.shterneregen.securelan.androidclient.network.discovery

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import org.junit.Assert.*
import org.junit.Test

class AndroidNsdDiscoveryProviderTest {
    private class Backend : NsdBackend {
        lateinit var resolved: (NsdFacade.Resolved) -> Unit
        lateinit var lost: (String) -> Unit
        var closed = 0
        var scopes = mutableListOf<String?>()
        override fun browse(networkKey: String?, resolved: (NsdFacade.Resolved) -> Unit, lost: (String) -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Browser {
            this.resolved = resolved; this.lost = lost; scopes += networkKey
            return object : NsdFacade.Browser { override fun close() { closed++ } }
        }
        override fun register(value: HostedRoomAdvertisement, networkKey: String?, registered: () -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Registration = error("Browser must not register")
        override fun close() = Unit
    }
    private class Networks : LanNetworkSource {
        var bindings = listOf(NetworkBinding("wifi:192.168.1.10", "wifi", IpLiteral.parse("192.168.1.10")!!, 24))
        lateinit var changed: () -> Unit
        override fun snapshot() = bindings
        override fun start(changed: () -> Unit) { this.changed = changed }
        override fun close() = Unit
    }
    private class Lease : MulticastLeaseOwner {
        var acquired = false
        override fun acquire() { acquired = true }
        override fun close() { acquired = false }
    }
    @Test fun modernNetworkLossAndLateCallbacksNeverResurrectRows() {
        val backend = Backend(); val networks = Networks(); val lease = Lease()
        val provider = AndroidNsdDiscoveryProvider(backend, networks, lease, NsdApiCapabilities(true, true))
        val events = mutableListOf<DiscoveryEvent>()
        provider.start(DiscoveryStartRequest(PeerDiscoveryConfig.listenOnly("local", "Android"))) { events += it }
        assertEquals(listOf("wifi"), backend.scopes)
        val host = HostedRoomAdvertisement("91d44e18-c494-40b6-81c0-edc4d5539c53", "5477fc9b-d3b0-4db3-90d6-90807fb9410d", "Room", 5050, null, true)
        val record = NsdFacade.Resolved("renamed.instance", 5050, listOf("192.168.1.42"), DnsSdRecordCodec.encode(host), "wifi")
        backend.resolved(record)
        val observation = events.filterIsInstance<DiscoveryEvent.Upsert>().single().observation
        assertNull(observation.leaseNanos)
        assertEquals("wifi:192.168.1.10", observation.endpoints.single().routeKey)
        assertNull(observation.filePortHint)
        networks.bindings = emptyList(); networks.changed()
        assertEquals(1, backend.closed)
        assertTrue(events.any { it is DiscoveryEvent.ResetSource && it.networkKey == "wifi" })
        provider.stop()
        val count = events.size
        backend.resolved(record); backend.lost(record.name)
        assertEquals(count, events.size)
        assertFalse(lease.acquired)
    }
    @Test fun legacyBrowserUsesApplicationLeaseAndDoesNotAdvertise() {
        val backend = Backend()
        val provider = AndroidNsdDiscoveryProvider(backend, Networks(), Lease(), NsdApiCapabilities(false, false))
        val events = mutableListOf<DiscoveryEvent>()
        provider.start(DiscoveryStartRequest(PeerDiscoveryConfig.listenOnly("local", "Android"))) { events += it }
        assertEquals(listOf<String?>(null), backend.scopes)
        val host = HostedRoomAdvertisement("91d44e18-c494-40b6-81c0-edc4d5539c53", "5477fc9b-d3b0-4db3-90d6-90807fb9410d", null, 5050, 5051, true)
        backend.resolved(NsdFacade.Resolved("room", 5050, listOf("192.168.1.42"), DnsSdRecordCodec.encode(host), null))
        assertEquals(90_000_000_000L, events.filterIsInstance<DiscoveryEvent.Upsert>().single().observation.leaseNanos)
        provider.close()
    }
}
