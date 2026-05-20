package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.webrtc.event.RtcEvent

fun interface RtcEventPublisher {
    fun publish(event: RtcEvent)
}
