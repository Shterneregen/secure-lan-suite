package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState

/**
 * Immutable snapshot describing the current state and configuration of an RTC session.
 *
 * @property sessionId unique session identifier.
 * @property localPeer nickname of the local participant.
 * @property remotePeer nickname of the remote participant.
 * @property mode active media mode (DATA, AUDIO, VIDEO, AUDIO_VIDEO).
 * @property dataChannelLabel label of the active RTCDataChannel.
 * @property state lifecycle connectivity state of the session.
 * @property message human-readable status message or error detail.
 */
@JvmRecord
data class RtcSessionSnapshot(
    val sessionId: String?,
    val localPeer: String?,
    val remotePeer: String?,
    val mode: RtcSessionMode?,
    val dataChannelLabel: String?,
    val state: RtcSessionState?,
    val message: String?,
)
