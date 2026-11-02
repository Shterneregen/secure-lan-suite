package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.lantools.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class SavedLanDeviceTest {
    @Test
    fun savedIdentityIsIndependentOfMutableNetworkHints() {
        val id = UUID.randomUUID()
        val saved = SavedLanDevice(id, "Workstation", wake = WakeConfiguration(mac))
        assertEquals(id, saved.copy(addressHint = ip("10.0.0.42"), displayName = "New name").id)
        assertNull(SavedLanDevice(UUID.randomUUID(), "Printer").wake)
    }

    @Test
    fun rejectsUnusableNamesAndHostnameHints() {
        for (name in listOf("", " ", " leading", "trailing ", "new\nline", "a".repeat(129))) {
            assertThrows(IllegalArgumentException::class.java) { SavedLanDevice(UUID.randomUUID(), name) }
        }
        for (hostname in listOf("", "space name", "a".repeat(254))) {
            assertThrows(IllegalArgumentException::class.java) { SavedLanDevice(UUID.randomUUID(), "Name", hostnameHint = hostname) }
        }
    }

    @Test
    fun snapshotDoesNotExposeCallerOwnedMutableCollection() {
        val entries = mutableListOf(network())
        val snapshot = LanNetworkSnapshot(entries)
        entries.clear()
        assertEquals(1, snapshot.interfaces.size)
        assertThrows(UnsupportedOperationException::class.java) { (snapshot.interfaces as MutableList).clear() }
    }
}
