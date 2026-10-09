package com.shterneregen.securelan.desktop.compose.state.transfer

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelResponsiveState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelCardKind
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.ui.TransferEntry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeFileTransferStateTest {
    @Test
    fun shouldExplainManualReviewWhenAutoSaveIsOff() {
        val state = transferState(autoAcceptFiles = false)

        assertEquals("Save incoming files automatically", state.receiveModeLabel)
        assertEquals("Off", state.receiveModeStatusLabel)
        assertEquals("Auto-save: off", state.receiveModeShortLabel)
        assertTrue(state.receiveModeSupportingText.contains("review every incoming file"))
    }

    @Test
    fun shouldExplainTrustedPeerScopeWhenAutoSaveIsOn() {
        val state = transferState(autoAcceptFiles = true)

        assertEquals("On", state.receiveModeStatusLabel)
        assertEquals("Auto-save: on", state.receiveModeShortLabel)
        assertTrue(state.receiveModeSupportingText.contains("known online peers"))
        assertTrue(state.receiveModeDescription.contains("Unknown or offline senders are always rejected"))
    }

    @Test
    fun shouldKeepAllActiveTransfersOutsideLimitedHistory() {
        val active = (1..6).map { entry("active-$it", if (it % 2 == 0) "Receiving" else "Sending") }
        val finished = (1..5).map { entry("finished-$it", if (it == 5) "Failed" else "Completed") }
        val state = transferState(false).copy(entries = active + finished)

        assertEquals(6, state.activeEntryRows.size)
        assertEquals(finished.takeLast(4), state.recentEntries)
        assertEquals(4, state.recentEntryRows.size)
        assertTrue(state.requiresAttention)
    }

    @Test
    fun shouldMoveCompletedTransfersIntoHistory() {
        val state = transferState(false).copy(entries = listOf(entry("completed", "Completed")))

        assertTrue(state.activeEntryRows.isEmpty())
        assertEquals(1, state.recentEntryRows.size)
        assertFalse(state.requiresAttention)
    }

    @Test
    fun shouldKeepTransferContentsAvailableAtEveryPanelWidth() {
        val state = transferState(false)
        val panel = ComposeContextPanelState.forRoom(state.peerListState, state)

        assertEquals("Actions", panel.title)
        listOf(1279, 1280, 1439, 1440, 1599, 1600).forEach { width ->
            assertFalse(panel.visibleCardsFor(ComposeContextPanelResponsiveState.forWidth(width)).single().collapsed)
        }
    }

    private fun entry(id: String, status: String) = TransferEntry(id, "$id.txt", false, status, 0, 100)

    @Test
    fun shouldKeepIncomingRequestsVisibleAlongsideCallsAtEveryWidth() {
        val state = transferState(false).copy(
            incomingPrompts = listOf(ComposeIncomingTransferPrompt("request", "Peer", "file.txt", 100, "192.168.1.2")),
        )
        val panel = ComposeContextPanelState.forCall(
            state.peerListState,
            ComposeShellMetadata.DEFAULT_MEDIA_VOICE_STATE,
            ComposeShellMetadata.DEFAULT_VIDEO_STATE,
            state,
        )

        assertTrue(state.requiresAttention)
        listOf(1279, 1280, 1439, 1440, 1599, 1600).forEach { width ->
            val transferCard = panel.visibleCardsFor(ComposeContextPanelResponsiveState.forWidth(width))
                .single { it.kind == ComposeContextPanelCardKind.TRANSFER_DETAILS }
            assertFalse(transferCard.collapsed)
        }
    }

    private fun transferState(autoAcceptFiles: Boolean): ComposeFileTransferState =
        ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(),
            peerListState = ComposePeerListState(peers = emptyList()),
            autoAcceptFiles = autoAcceptFiles,
        )
}
