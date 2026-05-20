package com.shterneregen.securelan.webrtc.event

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState

@JvmRecord
data class RtcStateChangedEvent(
    val sessionId: String?,
    val remotePeer: String?,
    val mode: RtcSessionMode?,
    val state: RtcSessionState?,
    val message: String?,
) : RtcEvent
