package com.shterneregen.securelan.webrtc.event;

public record RtcAudioLevelEvent(
        String sessionId,
        String peer,
        boolean local,
        double level,
        boolean active
) implements RtcEvent {
}
