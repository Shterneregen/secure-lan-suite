package com.shterneregen.securelan.desktop.compose

import androidx.compose.runtime.mutableStateOf
import com.shterneregen.securelan.desktop.ui.TransferEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ComposeTransferEntriesStateTest {
    @Test
    fun shouldKeepPublishedProgressAndSpeedDetachedFromLaterEvents() {
        var now = 0L
        val entry = TransferEntry("send-1", "video.mp4", true, "Sending", 0, 1024) { now }
        now = 1_000_000_000L
        entry.updateProgress(512, 50, 1024)
        val snapshot = entry.snapshot()

        entry.status = "Completed"
        entry.percent = 100
        entry.stopSpeedTracking()

        assertEquals("Sending", snapshot.status)
        assertEquals(50, snapshot.percent)
        assertEquals(512L, snapshot.lastTransferredBytes)
        assertEquals(512.0, snapshot.speedBytesPerSecond)
        assertEquals(1_000_000_000L, snapshot.lastSpeedDisplayNanos)
        assertEquals("Completed", entry.snapshot().status)
        assertEquals(0.0, entry.snapshot().speedBytesPerSecond)
    }

    @Test
    fun shouldPublishNewListSnapshotWhenMutableTransferEntryChanges() {
        val entry = TransferEntry("receive-1", "video.mp4", false, "Receiving", 0, 1024)
        val initial = listOf(entry)
        val state = mutableStateOf(initial, transferEntriesMutationPolicy)

        entry.updateProgress(transferredBytes = 512, percent = 50, totalBytes = 1024)
        val refreshed = ArrayList(initial)
        state.value = refreshed

        assertEquals(initial, refreshed)
        assertSame(refreshed, state.value)
        assertEquals(50, state.value.single().percent)
    }
}
