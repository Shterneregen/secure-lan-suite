package com.shterneregen.securelan.desktop.compose.state.shell

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ComposeSettingsSeparationTest {
    @Test
    fun shouldKeepAudioAndVideoSetupOutOfRoomAndPeerContext() {
        val transferState = ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE
        val roomContext = ComposeContextPanelState.forRoom(transferState)
        val peerContext = ComposeContextPanelState.forPeer(transferState)

        assertFalse(roomContext.visibleCardTitles.any { it.contains("Audio", ignoreCase = true) })
        assertFalse(peerContext.visibleCardTitles.any { it.contains("Audio", ignoreCase = true) })
        assertFalse(roomContext.visibleCardKinds.contains(ComposeContextPanelCardKind.QUICK_SHARE))
        assertFalse(peerContext.visibleCardKinds.contains(ComposeContextPanelCardKind.QUICK_SHARE))
    }

}
