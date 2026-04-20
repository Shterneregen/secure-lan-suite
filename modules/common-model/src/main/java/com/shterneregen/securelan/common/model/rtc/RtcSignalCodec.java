package com.shterneregen.securelan.common.model.rtc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RtcSignalCodec {
    private RtcSignalCodec() {
    }

    public static String serialize(RtcSignalEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("v", "1");
        fields.put("sessionId", encode(envelope.sessionId()));
        fields.put("fromPeer", encode(envelope.fromPeer()));
        fields.put("toPeer", encode(envelope.toPeer()));
        fields.put("type", envelope.type().name());
        fields.put("mode", envelope.mode().name());
        fields.put("dataChannelLabel", encode(envelope.dataChannelLabel()));
        fields.put("audioEnabled", Boolean.toString(envelope.audioEnabled()));
        fields.put("videoEnabled", Boolean.toString(envelope.videoEnabled()));
        fields.put("sdp", encode(envelope.sdp()));
        fields.put("iceCandidate", encode(envelope.iceCandidate()));
        fields.put("sdpMid", encode(envelope.sdpMid()));
        fields.put("sdpMLineIndex", Integer.toString(envelope.sdpMLineIndex()));
        fields.put("message", encode(envelope.message()));
        return fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    public static RtcSignalEnvelope deserialize(String payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        Map<String, String> fields = new LinkedHashMap<>();
        for (String token : payload.split(";")) {
            int splitIndex = token.indexOf('=');
            if (splitIndex <= 0) {
                continue;
            }
            fields.put(token.substring(0, splitIndex), token.substring(splitIndex + 1));
        }
        return new RtcSignalEnvelope(
                decode(fields.get("sessionId")),
                decode(fields.get("fromPeer")),
                decode(fields.get("toPeer")),
                RtcSignalType.valueOf(fields.getOrDefault("type", RtcSignalType.ERROR.name())),
                RtcSessionMode.valueOf(fields.getOrDefault("mode", RtcSessionMode.DATA.name())),
                decode(fields.get("dataChannelLabel")),
                Boolean.parseBoolean(fields.getOrDefault("audioEnabled", "false")),
                Boolean.parseBoolean(fields.getOrDefault("videoEnabled", "false")),
                decode(fields.get("sdp")),
                decode(fields.get("iceCandidate")),
                decode(fields.get("sdpMid")),
                Integer.parseInt(fields.getOrDefault("sdpMLineIndex", "-1")),
                decode(fields.get("message"))
        );
    }

    private static String encode(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
