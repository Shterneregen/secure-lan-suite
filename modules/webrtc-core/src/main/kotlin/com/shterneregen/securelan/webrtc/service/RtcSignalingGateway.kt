package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope

fun interface RtcSignalingGateway {
    fun send(signal: RtcSignalEnvelope)
}
