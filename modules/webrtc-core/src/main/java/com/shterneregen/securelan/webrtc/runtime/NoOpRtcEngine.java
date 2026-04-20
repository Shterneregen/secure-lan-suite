package com.shterneregen.securelan.webrtc.runtime;

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;
import com.shterneregen.securelan.webrtc.event.RtcDataMessageEvent;
import com.shterneregen.securelan.webrtc.event.RtcEvent;
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent;

import java.util.function.Consumer;

public class NoOpRtcEngine implements RtcEngine {
    private final String message;

    public NoOpRtcEngine() {
        this("Native WebRTC provider is not configured yet. The signaling architecture is integrated, but live RTCDataChannel and audio/video negotiation still require a provider module such as webrtc-java.");
    }

    public NoOpRtcEngine(String message) {
        this.message = message == null || message.isBlank()
                ? "Native WebRTC provider is unavailable."
                : message;
    }

    @Override
    public RtcRuntimeStatus status() {
        return RtcRuntimeStatus.unavailable(message);
    }

    @Override
    public void startSession(String sessionId, String localPeer, String remotePeer, com.shterneregen.securelan.common.model.rtc.RtcSessionMode mode, String dataChannelLabel, Consumer<RtcSignalEnvelope> outboundSignalConsumer, Consumer<RtcEvent> eventConsumer) {
        eventConsumer.accept(new RtcRuntimeWarningEvent(message));
    }

    @Override
    public void handleRemoteSignal(RtcSignalEnvelope signal, Consumer<RtcSignalEnvelope> outboundSignalConsumer, Consumer<RtcEvent> eventConsumer) {
        eventConsumer.accept(new RtcRuntimeWarningEvent(message + " Incoming signal from " + signal.fromPeer() + " was recorded but not executed."));
    }

    @Override
    public void sendData(String sessionId, String payload, Consumer<RtcEvent> eventConsumer) {
        eventConsumer.accept(new RtcDataMessageEvent(sessionId, "", true, payload));
        eventConsumer.accept(new RtcRuntimeWarningEvent(message));
    }

    @Override
    public void closeSession(String sessionId, Consumer<RtcEvent> eventConsumer) {
        eventConsumer.accept(new RtcRuntimeWarningEvent(message));
    }
}
