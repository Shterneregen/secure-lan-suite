package com.shterneregen.securelan.chat.discovery;

import com.shterneregen.securelan.common.net.NetworkConstants;

import java.time.Duration;
import java.util.Objects;

public record PeerDiscoveryConfig(
        String peerId,
        String nickname,
        int discoveryPort,
        int chatPort,
        int filePort,
        boolean announceEnabled,
        Duration announceInterval,
        Duration staleTimeout
) {
    public PeerDiscoveryConfig {
        Objects.requireNonNull(peerId, "peerId must not be null");
        Objects.requireNonNull(nickname, "nickname must not be null");
        Objects.requireNonNull(announceInterval, "announceInterval must not be null");
        Objects.requireNonNull(staleTimeout, "staleTimeout must not be null");
        if (peerId.isBlank()) {
            throw new IllegalArgumentException("peerId must not be blank");
        }
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("nickname must not be blank");
        }
        validatePort(discoveryPort, "discoveryPort");
        if (announceEnabled) {
            validatePort(chatPort, "chatPort");
            validatePort(filePort, "filePort");
        }
        if (announceInterval.isZero() || announceInterval.isNegative()) {
            throw new IllegalArgumentException("announceInterval must be positive");
        }
        if (staleTimeout.compareTo(announceInterval) <= 0) {
            throw new IllegalArgumentException("staleTimeout must be greater than announceInterval");
        }
    }

    public static PeerDiscoveryConfig defaults(String peerId, String nickname, int chatPort, int filePort) {
        return defaults(peerId, nickname, chatPort, filePort, true);
    }

    public static PeerDiscoveryConfig defaults(String peerId, String nickname, int chatPort, int filePort, boolean announceEnabled) {
        return new PeerDiscoveryConfig(
                peerId,
                nickname,
                NetworkConstants.DEFAULT_DISCOVERY_PORT,
                chatPort,
                filePort,
                announceEnabled,
                Duration.ofSeconds(10),
                Duration.ofSeconds(45)
        );
    }

    public static PeerDiscoveryConfig listenOnly(String peerId, String nickname) {
        return new PeerDiscoveryConfig(
                peerId,
                nickname,
                NetworkConstants.DEFAULT_DISCOVERY_PORT,
                0,
                0,
                false,
                Duration.ofSeconds(10),
                Duration.ofSeconds(45)
        );
    }

    private static void validatePort(int port, String fieldName) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 65535");
        }
    }
}
