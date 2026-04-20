package com.shterneregen.securelan.webrtc.service;

import com.shterneregen.securelan.webrtc.event.RtcEvent;

@FunctionalInterface
public interface RtcEventPublisher {
    void publish(RtcEvent event);
}
