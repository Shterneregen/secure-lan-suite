package com.shterneregen.securelan.webrtc.runtime.video

import dev.onvoid.webrtc.media.video.VideoCaptureCapability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class VideoCapabilitySelectorTest {
    private val selector = VideoCapabilitySelector()

    @Test
    fun shouldReturnNullWhenNoCapabilitiesAreAvailable() {
        assertNull(selector.select(null))
        assertNull(selector.select(emptyList()))
    }

    @Test
    fun shouldPreferBestLowFrameRateVgaCapability() {
        val fullHd30 = VideoCaptureCapability(1920, 1080, 30)
        val vga30 = VideoCaptureCapability(640, 480, 30)
        val qvga15 = VideoCaptureCapability(320, 240, 15)
        val vga15 = VideoCaptureCapability(640, 480, 15)

        val selected = selector.select(listOf(fullHd30, vga30, qvga15, vga15))

        assertSame(vga15, selected)
    }

    @Test
    fun shouldUseSafestFallbackWhenNoSafeVgaCapabilityExists() {
        val noFrameRate = VideoCaptureCapability(1280, 720, 0)
        val large30 = VideoCaptureCapability(1920, 1080, 30)
        val smaller60 = VideoCaptureCapability(800, 600, 60)

        val selected = selector.select(listOf(noFrameRate, large30, smaller60))

        assertSame(smaller60, selected)
    }

    @Test
    fun shouldFallBackToFirstNonNullCapabilityWhenOnlyInvalidDimensionsArePresent() {
        val invalidWidth = VideoCaptureCapability(0, 480, 30)
        val invalidHeight = VideoCaptureCapability(640, 0, 30)

        val selected = selector.select(listOf(null, invalidWidth, invalidHeight))

        assertSame(invalidWidth, selected)
    }

    @Test
    fun shouldDescribeAndSummarizeCapabilitiesWithoutChangingCopy() {
        val capabilities = listOf(
            VideoCaptureCapability(1, 2, 3),
            VideoCaptureCapability(2, 3, 4),
            VideoCaptureCapability(3, 4, 5),
            VideoCaptureCapability(4, 5, 6),
            VideoCaptureCapability(5, 6, 7),
            VideoCaptureCapability(6, 7, 8),
            VideoCaptureCapability(7, 8, 9),
            VideoCaptureCapability(8, 9, 10),
            VideoCaptureCapability(9, 10, 11),
        )

        assertEquals("<null>", selector.describe(null))
        assertEquals("640x480@15fps", selector.describe(VideoCaptureCapability(640, 480, 15)))
        assertEquals(
            "[1x2@3fps, 2x3@4fps, 3x4@5fps, 4x5@6fps, 5x6@7fps, 6x7@8fps, 7x8@9fps, 8x9@10fps, ... total=9]",
            selector.summarize(capabilities),
        )
    }
}
