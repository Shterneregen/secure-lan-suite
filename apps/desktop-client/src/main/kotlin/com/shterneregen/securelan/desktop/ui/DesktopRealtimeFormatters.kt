package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.audio.service.AudioCallProfile
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.webcam.service.VideoCallProfile
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import kotlin.math.roundToInt

object DesktopRealtimeFormatters {
    @JvmStatic
    fun formatAudioProfile(profile: AudioCallProfile): String =
        "Audio: ${profile.sampleRateHz} Hz, ${profile.channels} ch, echo cancel=${profile.echoCancellation}, noise suppression=${profile.noiseSuppression}"

    @JvmStatic
    fun formatVideoProfile(profile: VideoCallProfile, localVideoPreviewEnabled: Boolean): String =
        (if (localVideoPreviewEnabled) {
            "Video stage enabled inline with self preview. "
        } else {
            "Video stage enabled inline with self preview disabled by configuration. "
        }) + "Default profile ${profile.width}x${profile.height} @ ${profile.framesPerSecond} FPS."

    @JvmStatic
    fun formatRuntimeStatus(status: RtcRuntimeStatus?): String = when {
        status == null -> "Unavailable"
        status.available -> "${status.providerName} ready"
        else -> "Unavailable — ${status.message}"
    }

    @JvmStatic
    fun formatRuntimeLog(status: RtcRuntimeStatus): String = "[rtc] runtime: ${status.providerName} - ${status.message}"

    @JvmStatic
    fun formatAudioLevel(active: Boolean, local: Boolean, peer: String?, level: Double): String {
        val peerInfo = if (local) "local microphone" else peer
        return "${if (active) "Active" else "Quiet"} — $peerInfo — ${(level * 100).roundToInt()}%"
    }

    @JvmStatic
    fun safePeerName(peer: String?): String = if (peer.isNullOrBlank()) "peer" else peer

    @JvmStatic
    fun videoStageTitle(mode: RtcSessionMode, peer: String?): String {
        val remotePeer = safePeerName(peer)
        return if (mode == RtcSessionMode.AUDIO_VIDEO) {
            "Video call with $remotePeer"
        } else {
            "Video stream with $remotePeer"
        }
    }

    @JvmStatic
    fun videoMediaLabel(mode: RtcSessionMode): String = if (mode == RtcSessionMode.AUDIO_VIDEO) {
        "Audio + camera"
    } else {
        "Camera only"
    }

    @JvmStatic
    fun videoStageBadge(state: RtcSessionState): String = when (state) {
        RtcSessionState.IDLE -> "Idle"
        RtcSessionState.NEGOTIATING -> "Negotiating"
        RtcSessionState.CONNECTING -> "Connecting"
        RtcSessionState.CONNECTED -> "Live"
        RtcSessionState.CLOSING -> "Closing"
        RtcSessionState.CLOSED -> "Ended"
        RtcSessionState.FAILED,
        RtcSessionState.UNAVAILABLE,
        -> "Unavailable"
    }

    @JvmStatic
    fun shouldShowVideoStageAfterState(state: RtcSessionState): Boolean = when (state) {
        RtcSessionState.IDLE,
        RtcSessionState.CLOSED,
        RtcSessionState.FAILED,
        RtcSessionState.UNAVAILABLE,
        -> false
        RtcSessionState.NEGOTIATING,
        RtcSessionState.CONNECTING,
        RtcSessionState.CONNECTED,
        RtcSessionState.CLOSING,
        -> true
    }

    @JvmStatic
    fun voiceStatusText(state: RtcSessionState, peer: String?): String = when (state) {
        RtcSessionState.IDLE -> "Voice idle"
        RtcSessionState.CONNECTED -> "In call with ${safePeerName(peer)}"
        RtcSessionState.CONNECTING,
        RtcSessionState.NEGOTIATING,
        -> "Voice connecting"
        RtcSessionState.CLOSING -> "Voice closing"
        RtcSessionState.CLOSED -> "Voice idle"
        RtcSessionState.FAILED,
        RtcSessionState.UNAVAILABLE,
        -> "Voice unavailable"
    }

    @JvmStatic
    fun videoFrameCaption(local: Boolean, peer: String?, width: Int, height: Int): String = if (local) {
        "Self preview • ${width}x$height"
    } else {
        "${safePeerName(peer)} • ${width}x$height"
    }

    @JvmStatic
    fun cameraPreviewLiveStatus(width: Int, height: Int): String = "Camera preview live • ${width}x$height"

    @JvmStatic
    fun rtcStateDiagnostics(mode: RtcSessionMode, state: RtcSessionState, remotePeer: String?, message: String?): String =
        "[rtc] $mode session $state with ${safePeerName(remotePeer)} - ${message.orEmpty()}"

    @JvmStatic
    fun rtcWarningDiagnostics(message: String): String = "[rtc-warning] $message"
}
