package com.shterneregen.securelan.lantools.discovery

import com.shterneregen.securelan.common.net.address.IpLiteral
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class LanDeviceRegistryTest {
    private fun record(name: String = "Workstation", type: String = "_http._tcp.local.", hostname: String? = "work.local.",
        scope: String = "eth0:192.168.1.2", addresses: List<String> = listOf("192.168.1.20")) =
        LanServiceObservation(LanServiceKey(scope, type, name), hostname, addresses.map { IpLiteral.parse(it)!! }, 8080, Instant.EPOCH, 0)

    @Test fun mergesOverlappingServicesOnOneTargetAndPreservesDisplayCaseAndSessionId() {
        val registry = LanDeviceRegistry("run")
        registry.upsert(record())
        val first = registry.snapshot(0).devices.single()
        registry.upsert(record("SSH", "_ssh._tcp.local.", "WORK.LOCAL", addresses = listOf("192.168.1.20", "192.168.1.21")))
        val merged = registry.snapshot(0).devices.single()
        assertEquals(first.id, merged.id)
        assertEquals("Workstation", merged.displayName)
        assertEquals(2, merged.services.size)
        assertEquals(2, merged.addresses.size)
        assertNull(merged.deviceType)
        assertEquals(setOf("mDNS/DNS-SD"), merged.sources)
        registry.remove(record().key)
        assertEquals(first.id, registry.snapshot(0).devices.single().id)
    }

    @Test fun hostnameOrIpAloneDoesNotMergeDifferentNetworksOrConflictingClaims() {
        val registry = LanDeviceRegistry()
        registry.upsert(record())
        registry.upsert(record("Other host", hostname = "other.local."))
        registry.upsert(record("Other network", scope = "eth1:192.168.1.2"))
        registry.upsert(record("Disjoint addresses", addresses = listOf("192.168.1.30")))
        registry.upsert(record("Unknown host", hostname = null))
        assertEquals(5, registry.snapshot(0).devices.size)
    }

    @Test fun oneInstanceCarriesMultipleAddressesAndCaseInsensitiveUpdatesReplaceItsSnapshot() {
        val registry = LanDeviceRegistry()
        registry.upsert(record(addresses = listOf("192.168.1.20", "192.168.1.21", "192.168.1.21")))
        val id = registry.snapshot(0).devices.single().id
        registry.upsert(record("WORKSTATION", addresses = listOf("192.168.1.22")))
        assertEquals(id, registry.snapshot(0).devices.single().id)
        assertEquals(listOf("192.168.1.22"), registry.snapshot(0).devices.single().addresses.map { it.value })
        registry.remove(record("workstation").key)
        assertTrue(registry.snapshot(0).devices.isEmpty())
    }

    @Test fun expiryIsMonotonicAndCompletedSnapshotsNeverClaimPresence() {
        val registry = LanDeviceRegistry()
        registry.upsert(record().copy(leaseMillis = 100))
        val current = registry.snapshot(99_000_000)
        assertFalse(current.devices.single().stale)
        assertTrue(registry.snapshot(99_000_000, stale = true).devices.single().stale)
        assertTrue(registry.snapshot(100_000_000).devices.isEmpty())
        assertEquals(1, current.devices.size)
    }

    @Test fun resettingANetworkForgetsItsRowsAndDoesNotReuseItsPreviousIdentity() {
        val registry = LanDeviceRegistry()
        registry.upsert(record())
        val before = registry.snapshot(0).devices.single().id
        registry.resetScope(record().key.scope)
        registry.upsert(record())
        assertNotEquals(before, registry.snapshot(0).devices.single().id)
    }

    @Test fun recordAndDeviceLimitsAreExplicitAndBounded() {
        val registry = LanDeviceRegistry()
        repeat(600) { registry.upsert(record("Device $it", hostname = "host$it.local")) }
        val result = registry.snapshot(0)
        assertEquals(256, result.devices.size)
        assertTrue(result.partial)
    }

    @Test fun rejectsUnsupportedMalformedOrUnboundedObservationsBeforeTheyBecomeRows() {
        val valid = record()
        val invalid = listOf(
            valid.copy(key = valid.key.copy(type = "_securelan._tcp.local.")),
            valid.copy(key = valid.key.copy(instance = "x".repeat(64))),
            valid.copy(key = valid.key.copy(instance = "a\nb")),
            valid.copy(key = valid.key.copy(instance = "я".repeat(32))),
            valid.copy(hostname = "https://remote.example"), valid.copy(hostname = "external.example"),
            valid.copy(port = 0), valid.copy(port = 65536), valid.copy(leaseMillis = 0),
            valid.copy(addresses = emptyList()), valid.copy(addresses = List(17) { valid.addresses.first() }),
            valid.copy(addresses = listOf(IpLiteral.parse("224.0.0.1")!!)), valid.copy(source = "trusted peer"),
        )
        invalid.forEach { observation ->
            val registry = LanDeviceRegistry()
            registry.upsert(observation)
            assertTrue(registry.snapshot(0).devices.isEmpty(), observation.toString())
            assertTrue(registry.partial)
        }
    }
}
