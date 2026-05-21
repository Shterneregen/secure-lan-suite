package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.audio.service.AudioCallProfile
import com.shterneregen.securelan.webcam.service.VideoCallProfile
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DesktopRealtimeFormattersTest {
    @Test
    fun shouldFormatAudioProfile() {
        val profile = AudioCallProfile(48_000, 2, true, false)

        assertEquals(
            "Audio: 48000 Hz, 2 ch, echo cancel=true, noise suppression=false",
            DesktopRealtimeFormatters.formatAudioProfile(profile),
        )
    }

    @Test
    fun shouldFormatVideoProfileWithLocalPreviewEnabled() {
        val profile = VideoCallProfile(1280, 720, 30, false)

        assertEquals(
            "Video stage enabled inline with self preview. Default profile 1280x720 @ 30 FPS.",
            DesktopRealtimeFormatters.formatVideoProfile(profile, localVideoPreviewEnabled = true),
        )
    }

    @Test
    fun shouldFormatVideoProfileWithLocalPreviewDisabled() {
        val profile = VideoCallProfile(640, 480, 15, false)

        assertEquals(
            "Video stage enabled inline with self preview disabled by configuration. Default profile 640x480 @ 15 FPS.",
            DesktopRealtimeFormatters.formatVideoProfile(profile, localVideoPreviewEnabled = false),
        )
    }

    @Test
    fun shouldFormatNullRuntimeStatusAsUnavailable() {
        assertEquals("Unavailable", DesktopRealtimeFormatters.formatRuntimeStatus(null))
    }

    @Test
    fun shouldFormatAvailableRuntimeStatus() {
        assertEquals(
            "webrtc-java ready",
            DesktopRealtimeFormatters.formatRuntimeStatus(RtcRuntimeStatus("webrtc-java", true, "ready")),
        )
    }

    @Test
    fun shouldFormatUnavailableRuntimeStatus() {
        assertEquals(
            "Unavailable — provider missing",
            DesktopRealtimeFormatters.formatRuntimeStatus(RtcRuntimeStatus("unconfigured", false, "provider missing")),
        )
    }

    @Test
    fun shouldFormatLocalActiveAudioLevel() {
        assertEquals(
            "Active — local microphone — 42%",
            DesktopRealtimeFormatters.formatAudioLevel(active = true, local = true, peer = "Alice", level = 0.42),
        )
    }

    @Test
    fun shouldFormatRemoteQuietAudioLevel() {
        assertEquals(
            "Quiet — Bob — 13%",
            DesktopRealtimeFormatters.formatAudioLevel(active = false, local = false, peer = "Bob", level = 0.125),
        )
    }
}
