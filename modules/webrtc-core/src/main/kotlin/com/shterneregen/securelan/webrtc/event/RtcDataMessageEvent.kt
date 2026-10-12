package com.shterneregen.securelan.webrtc.event

/**
 * Event published when a message is sent or received over a WebRTC RTCDataChannel.
 *
 * @property sessionId identifier of the active WebRTC session, or null if unassigned.
 * @property remotePeer nickname or identifier of the remote peer.
 * @property outgoing `true` if the message was sent locally, `false` if received from remote peer.
 * @property payload text content or diagnostic description of the binary payload.
 */
@JvmRecord
data class RtcDataMessageEvent(
    val sessionId: String?,
    val remotePeer: String?,
    val outgoing: Boolean,
    val payload: String?,
) : RtcEvent
