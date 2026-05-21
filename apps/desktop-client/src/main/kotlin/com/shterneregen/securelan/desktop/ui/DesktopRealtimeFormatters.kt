package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.audio.service.AudioCallProfile
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
    fun formatAudioLevel(active: Boolean, local: Boolean, peer: String?, level: Double): String {
        val peerInfo = if (local) "local microphone" else peer
        return "${if (active) "Active" else "Quiet"} — $peerInfo — ${(level * 100).roundToInt()}%"
    }
}
