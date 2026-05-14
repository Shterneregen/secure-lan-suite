package com.shterneregen.securelan.stego.model;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record ExtractedStegoPayload(StegoContentType contentType, boolean encrypted, byte[] payload) {
    public ExtractedStegoPayload {
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(payload, "payload");
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public String asUtf8String() {
        if (contentType != StegoContentType.UTF8_TEXT) {
            throw new IllegalStateException("Extracted payload is not UTF-8 text");
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "ExtractedStegoPayload[contentType=" + contentType
                + ", encrypted=" + encrypted
                + ", payload=" + payload.length + " bytes]";
    }
}
