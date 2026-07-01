package com.shterneregen.securelan.desktop.compose.state.steganography

import com.shterneregen.securelan.stego.model.BmpCapacity
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeSteganographyStateTest {
    @Test
    fun shouldBlockMessageThatExceedsCoverCapacity() {
        val state = ComposeSteganographyState(
            coverPathText = "cover.png",
            outputPathText = "cover-stego.bmp",
            messageDraft = "message",
            capacity = capacity(payloadBytes = 3),
        )

        assertFalse(state.messageFitsCapacity)
        assertFalse(state.canHideMessage)
        assertTrue(state.blockedReasons.any { it.contains("larger", ignoreCase = true) })
    }

    @Test
    fun shouldCountUtf8BytesForCapacity() {
        val state = ComposeSteganographyState(
            coverPathText = "cover.png",
            outputPathText = "cover-stego.bmp",
            messageDraft = "Привет",
            capacity = capacity(payloadBytes = 12),
        )

        assertTrue(state.messageFitsCapacity)
        assertTrue(state.canHideMessage)
    }

    private fun capacity(payloadBytes: Int): BmpCapacity = BmpCapacity(
        width = 32,
        height = 32,
        bitsPerPixel = 24,
        carrierBytes = 3_072,
        headerBytes = 54,
        payloadCapacityBytes = payloadBytes,
    )
}
