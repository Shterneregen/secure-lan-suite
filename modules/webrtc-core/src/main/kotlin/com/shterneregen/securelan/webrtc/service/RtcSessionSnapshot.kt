package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState

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
