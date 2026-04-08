package com.shterneregen.securelan.common.model;

import java.util.Objects;

public record ChatEnvelope(
        ChatMessage message,
        DeliveryStatus status
) {
    public ChatEnvelope {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
