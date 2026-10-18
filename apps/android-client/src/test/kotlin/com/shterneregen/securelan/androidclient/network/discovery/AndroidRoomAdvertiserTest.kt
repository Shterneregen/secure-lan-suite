package com.shterneregen.securelan.androidclient.network.discovery

import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement
import com.shterneregen.securelan.chat.discovery.ProviderAvailability
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import org.junit.Assert.*
import org.junit.Test

class AndroidRoomAdvertiserTest {
    private class Backend : NsdBackend {
        val rooms = mutableListOf<HostedRoomAdvertisement>()
        var closed = 0
        lateinit var ready: () -> Unit
        override fun browse(networkKey: String?, resolved: (NsdFacade.Resolved) -> Unit, lost: (String) -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Browser = error("Advertiser must not browse")
        override fun register(value: HostedRoomAdvertisement, networkKey: String?, registered: () -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Registration {
            rooms += value; ready = registered
            return object : NsdFacade.Registration { override fun close() { closed++ } }
        }
        override fun close() = Unit
    }
    private class Networks : LanNetworkSource {
        var vpn = false
        override fun hasVpn() = vpn
        override fun snapshot() = listOf(NetworkBinding("wifi:192.168.1.10", "wifi", IpLiteral.parse("192.168.1.10")!!, 24))
        override fun start(changed: () -> Unit) = Unit
        override fun close() = Unit
    }
    private class Lease : MulticastLeaseOwner {
        var held = false
        override fun acquire() { held = true }
        override fun close() { held = false }
    }
    private val room = HostedRoomAdvertisement("91d44e18-c494-40b6-81c0-edc4d5539c53", "5477fc9b-d3b0-4db3-90d6-90807fb9410d", "Room", 5050, null, true)
    @Test fun hiddenAndStopReleaseRegistrationsWhileKeepingRoomLifetimeStable() {
        val backend = Backend(); val lease = Lease(); val statuses = mutableListOf<NsdAdvertisingStatus>()
        val advertiser = AndroidRoomAdvertiser(backend, Networks(), lease, NsdApiCapabilities(true, true)) { statuses += it }
        advertiser.update(room.copy(discoverable = false))
        assertTrue(backend.rooms.isEmpty())
        advertiser.update(room)
        val oldReady = backend.ready
        assertEquals(ProviderAvailability.STARTING, statuses.last().availability)
        advertiser.update(room.copy(discoverable = false))
        oldReady()
        assertEquals(ProviderAvailability.STOPPED, statuses.last().availability)
        assertEquals(1, backend.closed)
        assertFalse(lease.held)
        advertiser.update(room.copy(activeFilePort = 5051))
        assertEquals(room.roomInstanceId, backend.rooms.last().roomInstanceId)
        assertEquals(5051, backend.rooms.last().activeFilePort)
        advertiser.close()
        assertFalse(lease.held)
    }
    @Test fun legacyVpnCannotPublishUnscopedMdns() {
        val backend = Backend(); val statuses = mutableListOf<NsdAdvertisingStatus>()
        val advertiser = AndroidRoomAdvertiser(backend, Networks().apply { vpn = true }, Lease(), NsdApiCapabilities(false, false)) { statuses += it }
        advertiser.update(room)
        assertTrue(backend.rooms.isEmpty())
        assertTrue(statuses.last().legacyScopeUnavailable)
        advertiser.close()
    }
}
