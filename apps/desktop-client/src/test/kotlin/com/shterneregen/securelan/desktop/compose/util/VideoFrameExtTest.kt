package com.shterneregen.securelan.desktop.compose.util

import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VideoFrameExtTest {
    // Asymmetric landscape image with distinct colors to catch mirroring, direction and channel errors.
    private val colors = intArrayOf(
        0xFF123456.toInt(), 0xFFABCDEF.toInt(), 0xFF102030.toInt(),
        0xFF987654.toInt(), 0xFF2468AC.toInt(), 0xFF13579B.toInt(),
    )

    @Test fun `landscape frame without rotation preserves pixels`() {
        assertOrientation(0, 3, 2, intArrayOf(0, 1, 2, 3, 4, 5))
    }

    @Test fun `90 degree phone frame is upright portrait`() {
        assertOrientation(90, 2, 3, intArrayOf(3, 0, 4, 1, 5, 2))
    }

    @Test fun `upside down landscape frame is rotated 180 degrees`() {
        assertOrientation(180, 3, 2, intArrayOf(5, 4, 3, 2, 1, 0))
    }

    @Test fun `270 degree phone frame is upright portrait`() {
        assertOrientation(270, 2, 3, intArrayOf(2, 5, 1, 4, 0, 3))
    }

    @Test fun `wrapped angles retain their display orientation`() {
        assertOrientation(-90, 2, 3, intArrayOf(2, 5, 1, 4, 0, 3))
        assertOrientation(450, 2, 3, intArrayOf(3, 0, 4, 1, 5, 2))
    }

    @Test fun `orientation changes on consecutive frames are applied independently`() {
        listOf(90, 0, 270, 180, 90).forEach { rotation ->
            val frame = frame(rotation)
            val image = frame.toPreviewBufferedImage()
            assertEquals(if (rotation % 180 == 0) 3 else 2, image.width)
            assertEquals(if (rotation % 180 == 0) 2 else 3, image.height)
            // Converting the preview must not modify the native frame event or its pixels.
            assertEquals(3, frame.width())
            assertEquals(2, frame.height())
            assertEquals(rotation, frame.rotation())
            assertArrayEquals(pixels(), frame.bgraPixels())
        }
    }

    private fun assertOrientation(rotation: Int, width: Int, height: Int, pixelIndices: IntArray) {
        val image = frame(rotation).toPreviewBufferedImage()
        assertEquals(width, image.width)
        assertEquals(height, image.height)
        assertArrayEquals(pixelIndices.map { colors[it] }.toIntArray(), image.getRGB(0, 0, width, height, null, 0, width))
    }

    private fun frame(rotation: Int) = RtcVideoFrameEvent("call", "phone", false, 3, 2, rotation, pixels())

    private fun pixels(): ByteArray = colors.flatMap { argb ->
        listOf(argb.toByte(), (argb ushr 8).toByte(), (argb ushr 16).toByte(), (argb ushr 24).toByte())
    }.toByteArray()
}
