package com.shterneregen.securelan.webrtc.event

@JvmRecord
data class RtcAudioLevelEvent(
    val sessionId: String?,
    val peer: String?,
    val local: Boolean,
    val level: Double,
    val active: Boolean,
) : RtcEvent
