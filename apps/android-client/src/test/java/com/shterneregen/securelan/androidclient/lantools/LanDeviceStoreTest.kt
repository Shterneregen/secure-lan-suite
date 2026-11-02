package com.shterneregen.securelan.androidclient.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID

class LanDeviceStoreTest {
    private val device = SavedLanDevice(UUID.randomUUID(), "Рабочий компьютер", IpLiteral.parse("192.168.1.20"), "pc.local",
        WakeConfiguration(requireNotNull(MacAddress.parse("02:11:22:33:44:55")), IpLiteral.parse("192.168.1.255"), "wifi:1", 9))

    @Test fun unicodeAndWakeSettingsRoundTrip() {
        assertEquals(listOf(device), SavedLanDeviceCodec.decode(SavedLanDeviceCodec.encode(listOf(device))))
    }
    @Test fun emptyStoreRequiresLoadBeforeSave() {
        var payload: String? = null
        val store = PreferencesSavedLanDeviceStore({ payload }, { payload = it; true })
        assertThrows(IllegalStateException::class.java) { store.save(listOf(device)) }
        assertTrue(store.load().isEmpty())
        store.save(listOf(device))
        assertEquals(listOf(device), store.load())
    }
    @Test fun futureOrMalformedDataCannotBeOverwritten() {
        listOf("schema=2\ncount=0", "schema=1\ncount=-1", "schema=1\ncount=257", "broken", "schema=1\ncount=1\n0.id=wrong").forEach { payload ->
            var writes = 0
            val store = PreferencesSavedLanDeviceStore({ payload }, { writes++; true })
            assertThrows(Exception::class.java) { store.load() }
            assertThrows(IllegalStateException::class.java) { store.save(emptyList()) }
            assertEquals(0, writes)
        }
    }
    @Test fun duplicateIdsAndOversizedPayloadAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { SavedLanDeviceCodec.encode(listOf(device, device)) }
        assertThrows(IllegalArgumentException::class.java) { SavedLanDeviceCodec.decode("x".repeat(1_048_577)) }
        val encoded = SavedLanDeviceCodec.encode(listOf(device))
        assertThrows(IllegalArgumentException::class.java) { SavedLanDeviceCodec.decode(encoded.replace("count=1", "count=2") +
            encoded.lineSequence().filter { it.startsWith("0.") }.joinToString("\n", prefix = "\n") { it.replaceFirst("0.", "1.") }) }
    }
    @Test fun failedCommitDoesNotAdvanceExpectedState() {
        var payload: String? = null
        var succeeds = false
        val store = PreferencesSavedLanDeviceStore({ payload }, { if (succeeds) payload = it; succeeds })
        store.load()
        assertThrows(IOException::class.java) { store.save(listOf(device)) }
        assertNull(payload)
        succeeds = true
        store.save(listOf(device))
        assertEquals(listOf(device), store.load())
    }
    @Test fun externallyChangedPreferencesRequireReload() {
        var payload: String? = null
        val store = PreferencesSavedLanDeviceStore({ payload }, { payload = it; true })
        store.load()
        payload = SavedLanDeviceCodec.encode(listOf(device))
        assertThrows(IOException::class.java) { store.save(emptyList()) }
        assertEquals(listOf(device), store.load())
    }
    @Test fun failedCommitRestoresMemoryAndRequiresConfirmedRecoveryBeforeReload() {
        for (original in listOf(null, SavedLanDeviceCodec.encode(listOf(device)))) {
            var cached = original
            var persisted = original
            var succeeds = false
            val replacement = device.copy(displayName = "Changed")
            val store = PreferencesSavedLanDeviceStore({ cached }, {
                // SharedPreferences changes its process cache before the disk commit result arrives.
                cached = it
                if (succeeds) persisted = it
                succeeds
            })
            store.load()
            assertThrows(IOException::class.java) { store.save(listOf(replacement)) }
            assertEquals(original, cached)
            assertEquals(original, persisted)
            assertThrows(IOException::class.java) { store.load() }
            succeeds = true
            assertEquals(if (original == null) emptyList<SavedLanDevice>() else listOf(device), store.load())
            store.save(listOf(replacement))
            assertEquals(listOf(replacement), SavedLanDeviceCodec.decode(requireNotNull(persisted)))
        }
    }
    @Test fun invalidMacIsRejectedAndBlankMacRemovesWakeConfiguration() {
        assertThrows(InvalidLanForm::class.java) { LanDeviceForm(name = "PC", mac = "invalid").device() }
        assertNull(LanDeviceForm.from(device).copy(mac = "").device().wake)
        assertEquals(device, LanDeviceForm.from(device).device())
    }
}
