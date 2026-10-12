package com.shterneregen.securelan.filetransfer.quickshare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class QuickShareEventTest {
    @Test
    fun shouldCreateAndInspectQuickShareEvent() {
        val snapshot = QuickShareSnapshot(
            id = "test-share",
            type = QuickShareType.TEXT,
            displayName = "Greeting",
            fileName = "",
            fileSize = 12L,
            createdAt = Instant.now(),
            expiresAt = null,
            accessLimit = 1,
            accessCount = 0,
            status = QuickShareStatus.ACTIVE,
            urls = listOf("http://127.0.0.1:9090/test-share"),
        )

        val event = QuickShareEvent(
            shareId = "test-share",
            snapshot = snapshot,
            message = "Created share",
            remoteAddress = "127.0.0.1:55442",
        )

        assertEquals("test-share", event.shareId())
        assertEquals(snapshot, event.snapshot())
        assertEquals("Created share", event.message())
        assertEquals("127.0.0.1:55442", event.remoteAddress())
        assertTrue(event.toString().contains("test-share"))

        val copy = QuickShareEvent("test-share", snapshot, "Created share", "127.0.0.1:55442")
        assertEquals(event, copy)
        assertEquals(event.hashCode(), copy.hashCode())

        val different = QuickShareEvent("other-share", null, "Other", "")
        assertNotEquals(event, different)
    }

    @Test
    fun shouldNormalizeServerConfigAdvertisedHosts() {
        val config = QuickShareServerConfig(
            port = 8080,
            advertisedHosts = listOf(" 192.168.1.100 ", "", "192.168.1.100", "  10.0.0.1  "),
        )

        assertEquals(8080, config.port())
        assertEquals(listOf("192.168.1.100", "10.0.0.1"), config.advertisedHosts())
        assertTrue(config.toString().contains("8080"))

        val identical = QuickShareServerConfig(8080, listOf("192.168.1.100", "10.0.0.1"))
        assertEquals(config, identical)
        assertEquals(config.hashCode(), identical.hashCode())
    }

    @Test
    fun shouldVerifyQuickShareSnapshotEqualityAndAccessors() {
        val now = Instant.now()
        val snapshot = QuickShareSnapshot(
            id = "id-1",
            type = QuickShareType.FILE,
            displayName = "File",
            fileName = "data.bin",
            fileSize = 100L,
            createdAt = now,
            expiresAt = now.plusSeconds(300),
            accessLimit = 2,
            accessCount = 1,
            status = QuickShareStatus.ACTIVE,
            urls = listOf("http://127.0.0.1:8080/id-1", "http://192.168.1.5:8080/id-1"),
        )

        assertTrue(snapshot.active())
        assertEquals("http://127.0.0.1:8080/id-1", snapshot.primaryUrl())
        assertEquals(2, snapshot.accessLimit())
        assertEquals(1, snapshot.accessCount())
        assertEquals("data.bin", snapshot.fileName())
        assertEquals(100L, snapshot.fileSize())

        val copy = QuickShareSnapshot(
            id = "id-1",
            type = QuickShareType.FILE,
            displayName = "File",
            fileName = "data.bin",
            fileSize = 100L,
            createdAt = now,
            expiresAt = now.plusSeconds(300),
            accessLimit = 2,
            accessCount = 1,
            status = QuickShareStatus.ACTIVE,
            urls = listOf("http://127.0.0.1:8080/id-1", "http://192.168.1.5:8080/id-1"),
        )
        assertEquals(snapshot, copy)
        assertEquals(snapshot.hashCode(), copy.hashCode())

        val inactive = QuickShareSnapshot(
            id = "id-1",
            type = QuickShareType.FILE,
            displayName = "File",
            fileName = "data.bin",
            fileSize = 100L,
            createdAt = now,
            expiresAt = now.plusSeconds(300),
            accessLimit = 2,
            accessCount = 2,
            status = QuickShareStatus.LIMIT_REACHED,
            urls = listOf(),
        )
        assertFalse(inactive.active())
        assertEquals("", inactive.primaryUrl())
        assertNotEquals(snapshot, inactive)
    }
}
