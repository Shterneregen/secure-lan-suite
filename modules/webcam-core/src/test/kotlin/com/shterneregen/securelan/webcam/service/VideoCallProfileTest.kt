package com.shterneregen.securelan.webcam.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoCallProfileTest {

    @Test
    fun `instantiates with valid parameters`() {
        val profile = VideoCallProfile(
            width = 1920,
            height = 1080,
            framesPerSecond = 60,
            screenShareReady = true,
        )

        assertEquals(1920, profile.width)
        assertEquals(1080, profile.height)
        assertEquals(60, profile.framesPerSecond)
        assertTrue(profile.screenShareReady)
    }

    @Test
    fun `supports false for screenShareReady`() {
        val profile = VideoCallProfile(
            width = 640,
            height = 480,
            framesPerSecond = 15,
            screenShareReady = false,
        )

        assertFalse(profile.screenShareReady)
    }

    @Test
    fun `rejects non-positive width`() {
        val zeroEx = assertThrows(IllegalArgumentException::class.java) {
            VideoCallProfile(width = 0, height = 720, framesPerSecond = 30, screenShareReady = true)
        }
        assertTrue(zeroEx.message?.contains("width") == true)

        val negativeEx = assertThrows(IllegalArgumentException::class.java) {
            VideoCallProfile(width = -1, height = 720, framesPerSecond = 30, screenShareReady = true)
        }
        assertTrue(negativeEx.message?.contains("width") == true)
    }

    @Test
    fun `rejects non-positive height`() {
        val zeroEx = assertThrows(IllegalArgumentException::class.java) {
            VideoCallProfile(width = 1280, height = 0, framesPerSecond = 30, screenShareReady = true)
        }
        assertTrue(zeroEx.message?.contains("height") == true)

        val negativeEx = assertThrows(IllegalArgumentException::class.java) {
            VideoCallProfile(width = 1280, height = -10, framesPerSecond = 30, screenShareReady = true)
        }
        assertTrue(negativeEx.message?.contains("height") == true)
    }

    @Test
    fun `rejects non-positive framesPerSecond`() {
        val zeroEx = assertThrows(IllegalArgumentException::class.java) {
            VideoCallProfile(width = 1280, height = 720, framesPerSecond = 0, screenShareReady = true)
        }
        assertTrue(zeroEx.message?.contains("framesPerSecond") == true)

        val negativeEx = assertThrows(IllegalArgumentException::class.java) {
            VideoCallProfile(width = 1280, height = 720, framesPerSecond = -30, screenShareReady = true)
        }
        assertTrue(negativeEx.message?.contains("framesPerSecond") == true)
    }

    @Test
    fun `verifies equality and hashCode consistency`() {
        val profile1 = VideoCallProfile(1280, 720, 30, true)
        val profile2 = VideoCallProfile(1280, 720, 30, true)
        val different = VideoCallProfile(1920, 1080, 60, false)

        assertEquals(profile1, profile2)
        assertEquals(profile1.hashCode(), profile2.hashCode())
        assertNotEquals(profile1, different)
    }

    @Test
    fun `copy creates expected modified profile`() {
        val base = VideoCallProfile(1280, 720, 30, true)
        val modified = base.copy(screenShareReady = false)

        assertEquals(1280, modified.width)
        assertEquals(720, modified.height)
        assertEquals(30, modified.framesPerSecond)
        assertFalse(modified.screenShareReady)
    }
}
