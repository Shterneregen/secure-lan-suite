package com.shterneregen.securelan.desktop.compose.state.transfer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeAttachmentToolsStateTest {
    @Test
    fun shouldExposeOneSteganographyAction() {
        val state = ComposeAttachmentToolsState(
            peerSelected = false,
            fileTargetReady = false,
        )

        val steganographyItems = state.menuItems.filter {
            it.kind == ComposeAttachmentToolKind.STEGANOGRAPHY
        }

        assertEquals(1, steganographyItems.size)
        assertEquals("Steganography", steganographyItems.single().label)
        assertTrue(steganographyItems.single().enabled)
        assertTrue(state.discoverableWithinTwoInteractions)
        assertTrue(state.keepsAdvancedToolsContextual)
    }

    @Test
    fun shouldOmitSteganographyWhenUnavailable() {
        val state = ComposeAttachmentToolsState(
            peerSelected = false,
            fileTargetReady = false,
            steganographyAvailable = false,
        )

        assertFalse(state.menuItems.any { it.kind == ComposeAttachmentToolKind.STEGANOGRAPHY })
        assertFalse(state.discoverableWithinTwoInteractions)
        assertFalse(state.keepsAdvancedToolsContextual)
    }
}
