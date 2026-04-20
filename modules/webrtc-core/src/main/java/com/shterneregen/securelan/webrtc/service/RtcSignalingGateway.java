package com.shterneregen.securelan.webrtc.service;

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;

@FunctionalInterface
public interface RtcSignalingGateway {
    void send(RtcSignalEnvelope signal);
}
