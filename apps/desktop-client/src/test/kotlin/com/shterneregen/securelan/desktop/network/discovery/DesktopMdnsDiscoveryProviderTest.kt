package com.shterneregen.securelan.desktop.network.discovery

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DesktopMdnsDiscoveryProviderTest {
    private class Facade : JmDnsFacade {
        lateinit var resolved: (JmDnsFacade.Resolved) -> Unit
        lateinit var removed: (String) -> Unit
        val opened = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val hidden = CountDownLatch(1)
        override fun browse(resolved: (JmDnsFacade.Resolved) -> Unit, removed: (String) -> Unit) { this.resolved = resolved; this.removed = removed }
        override fun advertise(value: HostedRoomAdvertisement?) { if (value?.discoverable == false) hidden.countDown(); opened.countDown() }
        override fun close() { closed.countDown() }
    }
    @Test fun `resolved records retain source and late callbacks are ignored after stop`() {
        val facade = Facade()
        val events = java.util.concurrent.CopyOnWriteArrayList<DiscoveryEvent>()
        val binding = NetworkBinding("eth:192.168.1.10", "eth", IpLiteral.parse("192.168.1.10")!!, 24)
        val host = HostedRoomAdvertisement("2da1e450-798a-401f-990b-cb1cc4f61d92", "ab817e84-78af-40b4-994d-41bfa5ff20fb", "Room", 5050, null, true)
        val provider = DesktopMdnsDiscoveryProvider({ listOf(binding) }, { facade })
        provider.start(DiscoveryStartRequest(PeerDiscoveryConfig.listenOnly("local", "Local"), host)) { events += it }
        assertTrue(facade.opened.await(2, TimeUnit.SECONDS))
        val record = JmDnsFacade.Resolved("actual.renamed.instance", 5050, listOf("192.168.1.42", "10.0.0.42"), DnsSdRecordCodec.encode(host))
        facade.resolved(record)
        val observation = events.filterIsInstance<DiscoveryEvent.Upsert>().single().observation
        assertEquals("actual.renamed.instance", observation.key.identity)
        assertEquals(listOf("192.168.1.42"), observation.endpoints.map { it.host })
        assertNull(observation.filePortHint)
        provider.updateAdvertisement(host.copy(discoverable = false))
        assertTrue(facade.hidden.await(2, TimeUnit.SECONDS))
        provider.stop()
        assertTrue(facade.closed.await(2, TimeUnit.SECONDS))
        val count = events.size
        facade.resolved(record); facade.removed(record.name)
        assertEquals(count, events.size)
    }
}
