package com.shterneregen.securelan.chat.service;

import java.util.Objects;

public record ChatClientConnectRequest(String host, int port, String nickname, String sessionPassword) {
    public ChatClientConnectRequest {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(nickname, "nickname must not be null");
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }
}
