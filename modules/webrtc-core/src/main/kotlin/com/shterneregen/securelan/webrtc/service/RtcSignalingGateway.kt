package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope

/**
 * Gateway contract responsible for routing outbound WebRTC signaling messages
 * over the secure chat transport to destination peers.
 */
fun interface RtcSignalingGateway {
    /**
     * Sends an outbound signaling envelope to the remote peer.
     *
     * @param signal the envelope carrying SDP offer/answer, ICE candidate, hangup, or error.
     */
    fun send(signal: RtcSignalEnvelope)
}
