package com.shterneregen.securelan.desktop.compose.state.transfer

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeFileTransferStateTest {
    @Test
    fun shouldExplainManualReviewWhenAutoSaveIsOff() {
        val state = transferState(autoAcceptFiles = false)

        assertEquals("Save incoming files automatically", state.receiveModeLabel)
        assertEquals("Off", state.receiveModeStatusLabel)
        assertTrue(state.receiveModeSupportingText.contains("review every incoming file"))
    }

    @Test
    fun shouldExplainTrustedPeerScopeWhenAutoSaveIsOn() {
        val state = transferState(autoAcceptFiles = true)

        assertEquals("On", state.receiveModeStatusLabel)
        assertTrue(state.receiveModeSupportingText.contains("known online peers"))
        assertTrue(state.receiveModeDescription.contains("Unknown or offline senders are always rejected"))
    }

    private fun transferState(autoAcceptFiles: Boolean): ComposeFileTransferState =
        ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(),
            peerListState = ComposePeerListState(peers = emptyList()),
            autoAcceptFiles = autoAcceptFiles,
        )
}
