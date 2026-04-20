package com.shterneregen.securelan.webrtc.runtime;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;
import com.shterneregen.securelan.webrtc.event.RtcEvent;

import java.util.function.Consumer;

public interface RtcEngine extends AutoCloseable {
    RtcRuntimeStatus status();

    void startSession(
            String sessionId,
            String localPeer,
            String remotePeer,
            RtcSessionMode mode,
            String dataChannelLabel,
            Consumer<RtcSignalEnvelope> outboundSignalConsumer,
            Consumer<RtcEvent> eventConsumer
    );

    void handleRemoteSignal(RtcSignalEnvelope signal, Consumer<RtcSignalEnvelope> outboundSignalConsumer, Consumer<RtcEvent> eventConsumer);

    void sendData(String sessionId, String payload, Consumer<RtcEvent> eventConsumer);

    void closeSession(String sessionId, Consumer<RtcEvent> eventConsumer);

    @Override
    default void close() {
    }
}
