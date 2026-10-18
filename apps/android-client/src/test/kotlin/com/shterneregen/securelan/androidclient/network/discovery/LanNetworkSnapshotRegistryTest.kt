package com.shterneregen.securelan.androidclient.network.discovery

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import org.junit.Assert.*
import org.junit.Test

class LanNetworkSnapshotRegistryTest {
    private class Backend {
        var registrations = 0
        var releases = 0
        lateinit var events: LanNetworkSnapshotRegistry.Events<String>
        val registry = LanNetworkSnapshotRegistry<String> { listener ->
            registrations++
            events = listener
            AutoCloseable { releases++ }
        }
    }

    @Test fun ownersShareOneRegistrationAndOneShotReadersDoNotSubscribe() {
        val backend = Backend()
        assertTrue(backend.registry.snapshot().isEmpty())
        assertEquals(0, backend.registrations)
        var firstUpdates = 0; var secondUpdates = 0
        val first = backend.registry.subscribe { firstUpdates++ }
        val second = backend.registry.subscribe { secondUpdates++ }
        backend.events.update("wifi") { it.copy(isLan = true) }
        assertEquals(1, backend.registrations)
        assertEquals(1, firstUpdates)
        assertEquals(1, secondUpdates)
        first.close(); first.close()
        assertEquals(0, backend.releases)
        assertEquals(listOf("wifi"), backend.registry.snapshot().map { it.network })
        second.close(); second.close()
        assertEquals(1, backend.releases)
        assertTrue(backend.registry.snapshot().isEmpty())
    }

    @Test fun independentLanAndVpnSnapshotsRetainAddressesAcrossCapabilityUpdates() {
        val backend = Backend()
        val binding = NetworkBinding("wifi:192.168.1.10", "wifi", IpLiteral.parse("192.168.1.10")!!, 24)
        backend.registry.subscribe {}.use {
            // Link and capability callbacks may be processed separately, with no Internet requirement.
            backend.events.update("wifi") { it.copy(bindings = listOf(binding)) }
            backend.events.update("wifi") { it.copy(isLan = true) }
            backend.events.update("vpn") { it.copy(isVpn = true) }
            val snapshots = backend.registry.snapshot()
            assertEquals(listOf(binding), snapshots.single { it.isLan }.bindings)
            assertTrue(snapshots.any { it.isVpn })
            backend.events.remove("vpn")
            assertEquals(listOf("wifi"), backend.registry.snapshot().map { it.network })
            backend.events.remove("wifi")
            assertTrue(backend.registry.snapshot().isEmpty())
            assertTrue(backend.registry.receivedEvents())
        }
    }

    @Test fun lateCallbacksCannotResurrectStoppedOrRestartedSnapshots() {
        val backend = Backend()
        val first = backend.registry.subscribe {}
        val old = backend.events
        old.update("old-wifi") { it.copy(isLan = true) }
        first.close()
        old.update("late-wifi") { it.copy(isLan = true) }
        assertTrue(backend.registry.snapshot().isEmpty())
        backend.registry.subscribe {}.use {
            backend.events.update("new-wifi") { it.copy(isLan = true) }
            old.remove("new-wifi")
            old.update("late-vpn") { it.copy(isVpn = true) }
            assertEquals(listOf("new-wifi"), backend.registry.snapshot().map { it.network })
        }
        assertEquals(2, backend.registrations)
        assertEquals(2, backend.releases)
    }

    @Test fun joiningOwnerImmediatelySeesCurrentSnapshot() {
        val backend = Backend()
        backend.registry.subscribe {}.use {
            backend.events.update("ethernet") { it.copy(isLan = true) }
            var observed = emptyList<String>()
            backend.registry.subscribe { observed = backend.registry.snapshot().map { it.network } }.use {
                assertEquals(listOf("ethernet"), observed)
            }
        }
    }

    @Test fun observersCanCloseTheirLeaseInsideCallback() {
        val backend = Backend()
        lateinit var lease: AutoCloseable
        lease = backend.registry.subscribe { lease.close() }
        backend.events.update("wifi") { it.copy(isLan = true) }
        assertEquals(1, backend.releases)
        assertTrue(backend.registry.snapshot().isEmpty())
    }

    @Test fun failedRegistrationCanBeRetriedWithoutLeavingAnOwner() {
        var attempts = 0; var released = false
        val registry = LanNetworkSnapshotRegistry<String> {
            if (++attempts == 1) throw SecurityException("injected")
            AutoCloseable { released = true }
        }
        assertThrows(SecurityException::class.java) { registry.subscribe {} }
        registry.subscribe {}.use { assertEquals(2, attempts) }
        assertTrue(released)
    }

    @Test fun observerFailuresDoNotInterruptOtherOwnersOrLeakSubscriptions() {
        val backend = Backend()
        var updates = 0
        backend.registry.subscribe { updates++ }.use {
            backend.events.update("wifi") { it.copy(isLan = true) }
            backend.registry.subscribe { throw IllegalStateException("injected") }.use {
                backend.events.remove("wifi")
                assertEquals(2, updates)
            }
        }
        assertEquals(1, backend.releases)
        assertTrue(backend.registry.snapshot().isEmpty())
    }
}
