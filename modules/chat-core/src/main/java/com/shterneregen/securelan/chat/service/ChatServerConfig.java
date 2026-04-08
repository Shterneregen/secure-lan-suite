package com.shterneregen.securelan.chat.service;

import java.util.Objects;

public record ChatServerConfig(int port, String sessionPassword) {
    public ChatServerConfig {
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }
}
