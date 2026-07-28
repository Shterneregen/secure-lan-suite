package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class QuickShareEntryTest {
    @Test
    fun shouldExposeSnapshotIdentityAndPrimaryUrl() {
        val snapshot = snapshot(QuickShareStatus.ACTIVE, listOf("http://127.0.0.1:8090/share-1"))
        val entry = QuickShareEntry(snapshot)

        assertSame(snapshot, entry.snapshot())
        assertEquals("share-1", entry.id())
        assertEquals("http://127.0.0.1:8090/share-1", entry.url())
        assertTrue(entry.active())
    }

    @Test
    fun shouldExposeEmptyUrlWhenSnapshotHasNoUrls() {
        val entry = QuickShareEntry(snapshot(QuickShareStatus.ACTIVE, emptyList()))

        assertEquals("", entry.url())
    }

    @Test
    fun shouldReflectInactiveSnapshotStatus() {
        val entry = QuickShareEntry(snapshot(QuickShareStatus.STOPPED, listOf("http://127.0.0.1:8090/share-1")))

        assertFalse(entry.active())
    }

    private fun snapshot(status: QuickShareStatus, urls: List<String>): QuickShareSnapshot = QuickShareSnapshot(
        "share-1",
        QuickShareType.TEXT,
        "Demo text",
        "",
        9,
        Instant.parse("2026-05-21T09:00:00Z"),
        Instant.parse("2026-05-21T10:00:00Z"),
        3,
        1,
        status,
        urls,
    )
}
