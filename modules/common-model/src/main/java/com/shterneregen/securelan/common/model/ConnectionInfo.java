package com.shterneregen.securelan.common.model;

import java.util.Objects;

public record ConnectionInfo(
        String host,
        int port,
        boolean secure
) {
    public ConnectionInfo {
        Objects.requireNonNull(host, "host must not be null");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }
}
