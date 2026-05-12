package com.shterneregen.securelan.chat.discovery;

import java.time.Instant;
import java.util.Objects;

public record DiscoveredPeer(
        String peerId,
        String nickname,
        String host,
        int chatPort,
        int filePort,
        Instant lastSeen
) {
    public DiscoveredPeer {
        Objects.requireNonNull(peerId, "peerId must not be null");
        Objects.requireNonNull(nickname, "nickname must not be null");
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(lastSeen, "lastSeen must not be null");
        if (peerId.isBlank()) {
            throw new IllegalArgumentException("peerId must not be blank");
        }
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("nickname must not be blank");
        }
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        validatePort(chatPort, "chatPort");
        validatePort(filePort, "filePort");
    }

    private static void validatePort(int port, String fieldName) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 65535");
        }
    }
}
