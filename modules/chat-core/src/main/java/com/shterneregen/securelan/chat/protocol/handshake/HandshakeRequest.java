package com.shterneregen.securelan.chat.protocol.handshake;

import java.util.Objects;

public record HandshakeRequest(String nickname, String sessionPassword) {
    public HandshakeRequest {
        Objects.requireNonNull(nickname, "nickname must not be null");
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null");
    }
}
