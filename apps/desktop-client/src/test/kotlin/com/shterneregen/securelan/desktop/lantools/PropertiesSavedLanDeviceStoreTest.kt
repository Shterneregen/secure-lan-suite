package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class PropertiesSavedLanDeviceStoreTest {
    @TempDir lateinit var directory: Path
    private val device get() = SavedLanDevice(UUID.randomUUID(), "Рабочая станция", IpLiteral.parse("192.168.1.42"), "workstation.local",
        WakeConfiguration(MacAddress.parse("02:ab:cd:ef:12:34")!!, IpLiteral.parse("192.168.1.255"), "eth0", 7))

    @Test fun roundTripsAllFieldsAndEmptyDevicesAcrossInstances() {
        val path = directory.resolve("devices.properties")
        val store = PropertiesSavedLanDeviceStore(path)
        assertTrue(store.load().devices.isEmpty())
        val saved = listOf(device, SavedLanDevice(UUID.randomUUID(), "Printer"))
        store.save(saved)
        assertEquals(saved, PropertiesSavedLanDeviceStore(path).load().devices)
        store.save(emptyList())
        assertTrue(PropertiesSavedLanDeviceStore(path).load().devices.isEmpty())
    }

    @Test fun atomicReplacementFailureRetainsOriginalAndRemovesTemporaryFile() {
        val path = directory.resolve("devices.properties")
        val original = PropertiesSavedLanDeviceStore(path)
        original.load(); original.save(listOf(device))
        val bytes = Files.readAllBytes(path)
        val broken = PropertiesSavedLanDeviceStore(path) { _, _ -> throw IOException("disk failure") }
        broken.load()
        assertThrows(IOException::class.java) { broken.save(emptyList()) }
        assertArrayEquals(bytes, Files.readAllBytes(path))
        Files.list(directory).use { assertEquals(1L, it.count()) }
    }

    @Test fun futureCorruptAndOversizedFilesCannotBeOverwritten() {
        val path = directory.resolve("devices.properties")
        for (text in listOf("schemaVersion=99\ncount=0\n", "schemaVersion=1\ncount=999999999\n", "bad=\\uXXXX\n")) {
            Files.writeString(path, text)
            val store = PropertiesSavedLanDeviceStore(path)
            assertNotNull(store.load().warning)
            assertThrows(IOException::class.java) { store.save(emptyList()) }
            assertEquals(text, Files.readString(path))
        }
        Files.write(path, ByteArray(1_048_577))
        assertThrows(IOException::class.java) { PropertiesSavedLanDeviceStore(path).load() }
    }

    @Test fun invalidRowPreservesValidRowsButBlocksRewritingTheFile() {
        val path = directory.resolve("devices.properties")
        val store = PropertiesSavedLanDeviceStore(path)
        store.load(); store.save(listOf(device))
        val text = Files.readString(path).replace("count=1", "count=2") + "\ndevice.1.id=invalid\n"
        Files.writeString(path, text)
        val loaded = store.load()
        assertEquals(1, loaded.devices.size)
        assertNotNull(loaded.warning)
        assertThrows(IOException::class.java) { store.save(loaded.devices) }
    }

    @Test fun externalEditsRequireExplicitReloadAndSavingCannotPrecedeLoad() {
        val path = directory.resolve("devices.properties")
        val store = PropertiesSavedLanDeviceStore(path)
        assertThrows(IOException::class.java) { store.save(emptyList()) }
        store.load(); store.save(listOf(device))
        Files.writeString(path, Files.readString(path) + "\nexternal=value\n")
        assertThrows(IOException::class.java) { store.save(emptyList()) }
        store.load(); store.save(emptyList())
        assertTrue(store.load().devices.isEmpty())
    }

    @Test fun invalidScalarDataCannotReplaceAValidFileOrBecomeWritableOnLoad() {
        val path = directory.resolve("devices.properties")
        val store = PropertiesSavedLanDeviceStore(path)
        val saved = device
        store.load(); store.save(listOf(saved))
        val original = Files.readAllBytes(path)
        for (invalid in listOf(saved.copy(addressHint = IpLiteral.parse("224.0.0.1")),
            saved.copy(wake = saved.wake!!.copy(preferredInterfaceKey = "eth0\ninvalid")))) {
            assertThrows(IllegalArgumentException::class.java) { store.save(listOf(invalid)) }
            assertArrayEquals(original, Files.readAllBytes(path))
        }
        Files.writeString(path, Files.readString(path).replace("device.0.interface=eth0", "device.0.interface=eth0\\ninvalid"))
        val damaged = Files.readAllBytes(path)
        assertNotNull(store.load().warning)
        assertThrows(IOException::class.java) { store.save(emptyList()) }
        assertArrayEquals(damaged, Files.readAllBytes(path))
    }
}
