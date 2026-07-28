package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.webrtc.service.RtcMediaDevice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaDeviceChoiceTest {
    @Test
    fun shouldCreateSystemDefaultChoice() {
        val choice = MediaDeviceChoice.systemDefault("System default microphone")

        assertEquals("", choice.deviceId)
        assertEquals("System default microphone", choice.label)
        assertTrue(choice.systemDefault)
        assertTrue(choice.defaultDevice)
        assertEquals("System default microphone", choice.toString())
    }

    @Test
    fun shouldCreateDeviceChoiceFromRtcMediaDevice() {
        val choice = MediaDeviceChoice.of(RtcMediaDevice(" mic-1 ", " Built-in microphone ", true))

        assertEquals("mic-1", choice.deviceId)
        assertEquals("Built-in microphone", choice.label)
        assertFalse(choice.systemDefault)
        assertTrue(choice.defaultDevice)
        assertEquals("Built-in microphone (default)", choice.toString())
    }

    @Test
    fun shouldMatchTrimmedSelectedDeviceId() {
        val choice = MediaDeviceChoice.of(RtcMediaDevice("camera-1", "USB Camera", false))

        assertTrue(choice.matches(" camera-1 "))
        assertFalse(choice.matches("camera-2"))
    }

    @Test
    fun shouldRenderNonDefaultDeviceWithoutSuffix() {
        val choice = MediaDeviceChoice.of(RtcMediaDevice("speaker-1", "External Capture", false))

        assertEquals("External Capture", choice.toString())
    }
}
