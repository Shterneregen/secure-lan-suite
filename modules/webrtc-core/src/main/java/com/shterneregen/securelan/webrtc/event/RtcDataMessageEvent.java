package com.shterneregen.securelan.webrtc.event;

public record RtcDataMessageEvent(String sessionId, String remotePeer, boolean outgoing, String payload) implements RtcEvent {
}
