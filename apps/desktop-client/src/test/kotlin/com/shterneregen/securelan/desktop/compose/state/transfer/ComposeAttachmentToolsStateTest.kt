package com.shterneregen.securelan.desktop.compose.state.transfer

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeAttachmentToolsStateTest {
    @Test
    fun shouldEnableAttachForAReadyFileTarget() {
        val state = ComposeAttachmentToolsState(
            peerSelected = true,
            fileTargetReady = true,
        )

        assertTrue(state.enabled)
        assertTrue(state.statusText.contains("one or more files"))
    }

    @Test
    fun shouldRequireAReadyFileTarget() {
        val state = ComposeAttachmentToolsState(
            peerSelected = false,
            fileTargetReady = false,
        )

        assertFalse(state.enabled)
        assertTrue(state.statusText.contains("Select an online person"))
    }

    @Test
    fun shouldSuggestQuickShareWhenDirectSendingIsUnavailable() {
        val state = ComposeAttachmentToolsState(
            peerSelected = true,
            fileTargetReady = false,
        )
        assertFalse(state.enabled)
        assertTrue(state.statusText.contains("Quick Share"))
    }
}
