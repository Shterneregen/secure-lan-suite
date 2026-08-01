package com.shterneregen.securelan.desktop.compose

import androidx.compose.runtime.mutableStateOf
import com.shterneregen.securelan.desktop.ui.TransferEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ComposeTransferEntriesStateTest {
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
