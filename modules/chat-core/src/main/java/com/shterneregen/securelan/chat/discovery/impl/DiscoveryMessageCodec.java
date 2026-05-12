package com.shterneregen.securelan.chat.discovery.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class DiscoveryMessageCodec {
    private static final String PREFIX = "SECURELAN_DISCOVERY_V1";

    private DiscoveryMessageCodec() {
    }

    static String encode(DiscoveryMessage message) {
        return String.join("|",
                PREFIX,
                encodeText(message.peerId()),
                encodeText(message.nickname()),
                Integer.toString(message.chatPort()),
                Integer.toString(message.filePort())
        );
    }

    static DiscoveryMessage decode(String payload) {
        String[] parts = payload == null ? new String[0] : payload.split("\\|", -1);
        if (parts.length != 5 || !PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported discovery message");
        }
        String peerId = decodeText(parts[1]);
        String nickname = decodeText(parts[2]);
        int chatPort = parsePort(parts[3], "chatPort");
        int filePort = parsePort(parts[4], "filePort");
        return new DiscoveryMessage(peerId, nickname, chatPort, filePort);
    }

    static byte[] toBytes(DiscoveryMessage message) {
        return encode(message).getBytes(StandardCharsets.UTF_8);
    }

    static DiscoveryMessage fromBytes(byte[] data, int length) {
        return decode(new String(data, 0, length, StandardCharsets.UTF_8));
    }

    private static String encodeText(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static int parsePort(String value, String fieldName) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(fieldName + " must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid TCP/UDP port", e);
        }
    }
}
