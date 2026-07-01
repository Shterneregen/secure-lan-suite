package com.shterneregen.securelan.desktop.compose.state.shell

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeSettingsSeparationTest {
    @Test
    fun shouldKeepAudioAndVideoSetupOutOfRoomAndPeerContext() {
        val peerState = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE
        val transferState = ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE
        val roomContext = ComposeContextPanelState.forRoom(peerState, transferState)
        val peerContext = ComposeContextPanelState.forPeer(peerState, transferState)

        assertFalse(roomContext.visibleCardTitles.any { it.contains("Audio", ignoreCase = true) })
        assertFalse(peerContext.visibleCardTitles.any { it.contains("Audio", ignoreCase = true) })
        assertTrue(roomContext.visibleCardKinds.contains(ComposeContextPanelCardKind.QUICK_SHARE))
        assertTrue(peerContext.visibleCardKinds.contains(ComposeContextPanelCardKind.QUICK_SHARE))
    }

}
