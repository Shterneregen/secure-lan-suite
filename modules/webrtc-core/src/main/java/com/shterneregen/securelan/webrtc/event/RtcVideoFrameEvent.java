package com.shterneregen.securelan.webrtc.event;

public record RtcVideoFrameEvent(
        String sessionId,
        String peer,
        boolean local,
        int width,
        int height,
        int rotation,
        byte[] bgraPixels
) implements RtcEvent {
}
