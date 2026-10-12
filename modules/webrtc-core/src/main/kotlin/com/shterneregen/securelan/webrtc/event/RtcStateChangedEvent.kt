package com.shterneregen.securelan.webrtc.event

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState

/**
 * Event published when the lifecycle or connectivity state of a WebRTC session changes.
 *
 * @property sessionId identifier of the active WebRTC session, or null if unassigned.
 * @property remotePeer nickname or identifier of the remote peer in the session.
 * @property mode operating mode of the session (e.g. DATA, AUDIO, VIDEO, AUDIO_VIDEO).
 * @property state new lifecycle state of the session.
 * @property message optional human-readable message or reason describing the state transition.
 */
@JvmRecord
data class RtcStateChangedEvent(
    val sessionId: String?,
    val remotePeer: String?,
    val mode: RtcSessionMode?,
    val state: RtcSessionState?,
    val message: String?,
) : RtcEvent
