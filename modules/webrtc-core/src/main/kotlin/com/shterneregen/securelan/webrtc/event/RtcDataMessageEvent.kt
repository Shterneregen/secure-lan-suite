package com.shterneregen.securelan.webrtc.event

@JvmRecord
data class RtcDataMessageEvent(
    val sessionId: String?,
    val remotePeer: String?,
    val outgoing: Boolean,
    val payload: String?,
) : RtcEvent
