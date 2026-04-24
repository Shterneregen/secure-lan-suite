package com.shterneregen.securelan.webrtc.event;

import java.util.Arrays;
import java.util.Objects;

public record RtcVideoFrameEvent(
        String sessionId,
        String peer,
        boolean local,
        int width,
        int height,
        int rotation,
        byte[] bgraPixels
) implements RtcEvent {
    public RtcVideoFrameEvent {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        peer = Objects.requireNonNullElse(peer, "");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Video frame dimensions must be positive: " + width + "x" + height);
        }
        bgraPixels = Arrays.copyOf(
                Objects.requireNonNull(bgraPixels, "bgraPixels must not be null"),
                bgraPixels.length
        );
        int expectedBgraLength = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (bgraPixels.length != expectedBgraLength) {
            throw new IllegalArgumentException("BGRA payload length " + bgraPixels.length
                    + " does not match frame dimensions " + width + "x" + height);
        }
    }

    @Override
    public byte[] bgraPixels() {
        return Arrays.copyOf(bgraPixels, bgraPixels.length);
    }
}
