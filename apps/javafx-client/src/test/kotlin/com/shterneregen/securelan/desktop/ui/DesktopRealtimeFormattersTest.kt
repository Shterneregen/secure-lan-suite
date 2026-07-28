package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.audio.service.AudioCallProfile
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
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
    fun shouldFormatRuntimeLogLine() {
        assertEquals(
            "[rtc] runtime: webrtc-java - ready",
            DesktopRealtimeFormatters.formatRuntimeLog(RtcRuntimeStatus("webrtc-java", true, "ready")),
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

    @Test
    fun shouldFallbackBlankPeerNameForRealtimeCopy() {
        assertEquals("peer", DesktopRealtimeFormatters.safePeerName("   "))
        assertEquals("Alice", DesktopRealtimeFormatters.safePeerName("Alice"))
    }

    @Test
    fun shouldFormatVideoStageTitleAndMediaLabel() {
        assertEquals(
            "Video call with Alice",
            DesktopRealtimeFormatters.videoStageTitle(RtcSessionMode.AUDIO_VIDEO, "Alice"),
        )
        assertEquals(
            "Video stream with peer",
            DesktopRealtimeFormatters.videoStageTitle(RtcSessionMode.VIDEO, null),
        )
        assertEquals("Audio + camera", DesktopRealtimeFormatters.videoMediaLabel(RtcSessionMode.AUDIO_VIDEO))
        assertEquals("Camera only", DesktopRealtimeFormatters.videoMediaLabel(RtcSessionMode.VIDEO))
    }

    @Test
    fun shouldFormatVideoStageBadgeAndVisibilityByState() {
        assertEquals("Idle", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.IDLE))
        assertEquals("Negotiating", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.NEGOTIATING))
        assertEquals("Connecting", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.CONNECTING))
        assertEquals("Live", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.CONNECTED))
        assertEquals("Closing", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.CLOSING))
        assertEquals("Ended", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.CLOSED))
        assertEquals("Unavailable", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.FAILED))
        assertEquals("Unavailable", DesktopRealtimeFormatters.videoStageBadge(RtcSessionState.UNAVAILABLE))
        assertEquals(true, DesktopRealtimeFormatters.shouldShowVideoStageAfterState(RtcSessionState.CONNECTED))
        assertEquals(false, DesktopRealtimeFormatters.shouldShowVideoStageAfterState(RtcSessionState.IDLE))
        assertEquals(false, DesktopRealtimeFormatters.shouldShowVideoStageAfterState(RtcSessionState.CLOSED))
    }

    @Test
    fun shouldFormatVoiceStatusText() {
        assertEquals("Voice idle", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.IDLE, "Alice"))
        assertEquals("In call with Alice", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.CONNECTED, "Alice"))
        assertEquals("Voice connecting", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.NEGOTIATING, "Alice"))
        assertEquals("Voice connecting", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.CONNECTING, "Alice"))
        assertEquals("Voice closing", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.CLOSING, "Alice"))
        assertEquals("Voice idle", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.CLOSED, "Alice"))
        assertEquals("Voice unavailable", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.FAILED, "Alice"))
        assertEquals("Voice unavailable", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.UNAVAILABLE, "Alice"))
        assertEquals("In call with peer", DesktopRealtimeFormatters.voiceStatusText(RtcSessionState.CONNECTED, " "))
    }

    @Test
    fun shouldFormatVideoFrameCaptions() {
        assertEquals("Self preview • 640x480", DesktopRealtimeFormatters.videoFrameCaption(true, "Alice", 640, 480))
        assertEquals("Alice • 1280x720", DesktopRealtimeFormatters.videoFrameCaption(false, "Alice", 1280, 720))
        assertEquals("peer • 1280x720", DesktopRealtimeFormatters.videoFrameCaption(false, null, 1280, 720))
    }

    @Test
    fun shouldFormatCameraPreviewAndRtcDiagnostics() {
        assertEquals("Camera preview live • 640x480", DesktopRealtimeFormatters.cameraPreviewLiveStatus(640, 480))
        assertEquals(
            "[rtc] AUDIO_VIDEO session CONNECTED with Alice - Connected",
            DesktopRealtimeFormatters.rtcStateDiagnostics(
                RtcSessionMode.AUDIO_VIDEO,
                RtcSessionState.CONNECTED,
                "Alice",
                "Connected",
            ),
        )
        assertEquals(
            "[rtc] AUDIO session FAILED with peer - ",
            DesktopRealtimeFormatters.rtcStateDiagnostics(RtcSessionMode.AUDIO, RtcSessionState.FAILED, " ", null),
        )
        assertEquals("[rtc-warning] camera unavailable", DesktopRealtimeFormatters.rtcWarningDiagnostics("camera unavailable"))
    }
}
