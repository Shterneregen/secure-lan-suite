package com.shterneregen.securelan.webrtc.service;

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus;

import java.util.Optional;

public interface RtcSessionService extends AutoCloseable {
    RtcRuntimeStatus runtimeStatus();
    Optional<RtcSessionSnapshot> currentSession();
    RtcSessionSnapshot startSession(RtcSessionRequest request);
    void acceptInboundSignal(String localPeer, RtcSignalEnvelope signal);
    void sendDataMessage(String payload);
    void closeCurrentSession();

    @Override
    default void close() {
    }
}
