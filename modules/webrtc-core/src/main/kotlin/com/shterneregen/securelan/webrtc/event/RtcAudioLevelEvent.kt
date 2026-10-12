package com.shterneregen.securelan.webrtc.event

/**
 * Event published periodically when audio levels change on a local microphone track
 * or an incoming remote audio track.
 *
 * @property sessionId identifier of the active WebRTC session, or null if unassigned.
 * @property peer nickname or identifier of the peer associated with the audio stream.
 * @property local `true` if the audio level corresponds to local microphone capture,
 *                 `false` if it corresponds to incoming remote playback audio.
 * @property level normalized root-mean-square (RMS) audio energy level in the range [0.0, 1.0].
 * @property active `true` when the computed energy level exceeds the voice-activity threshold.
 */
@JvmRecord
data class RtcAudioLevelEvent(
    val sessionId: String?,
    val peer: String?,
    val local: Boolean,
    val level: Double,
    val active: Boolean,
) : RtcEvent
