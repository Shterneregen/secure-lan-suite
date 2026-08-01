package com.shterneregen.securelan.desktop.compose.state.transfer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ComposeAttachmentToolsStateTest {
    @Test
    fun shouldExposeOnlySecureFileAction() {
        val state = ComposeAttachmentToolsState(
            peerSelected = false,
            fileTargetReady = false,
        )

        assertEquals(1, state.menuItems.size)
        assertEquals(ComposeAttachmentToolKind.SECURE_FILE, state.menuItems.single().kind)
        assertEquals("Send secure file", state.menuItems.single().label)
    }

    @Test
    fun shouldRequireAReadyFileTarget() {
        val state = ComposeAttachmentToolsState(
            peerSelected = false,
            fileTargetReady = false,
        )

        assertFalse(state.menuItems.single().enabled)
        assertEquals("Select an online person before sending a secure file.", state.menuItems.single().statusText)
    }

    @Test
    fun shouldSuggestQuickShareWhenDirectSendingIsUnavailable() {
        val state = ComposeAttachmentToolsState(
            peerSelected = true,
            fileTargetReady = false,
        )
        val directSend = state.menuItems.single {
            it.kind == ComposeAttachmentToolKind.SECURE_FILE
        }

        assertFalse(directSend.enabled)
        assertEquals(
            "Direct sending is unavailable for this person. Use Tools → Share on LAN instead.",
            directSend.statusText,
        )
    }
}
