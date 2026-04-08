package com.shterneregen.securelan.chat.protocol.handshake;

import java.util.Objects;

public record HandshakeResponse(HandshakeStatus status, String nickname, String reason) {
    public HandshakeResponse {
        Objects.requireNonNull(status, "status must not be null");
        nickname = nickname == null ? "" : nickname;
        reason = reason == null ? "" : reason;
    }

    public boolean accepted() {
        return status == HandshakeStatus.ACCEPTED;
    }
}
