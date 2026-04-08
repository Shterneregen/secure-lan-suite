package com.shterneregen.securelan.common.model;

import java.util.Objects;

public record PeerInfo(
        String id,
        String username,
        String host,
        int port
) {
    public PeerInfo {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(host, "host must not be null");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }
}
