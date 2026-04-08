package com.shterneregen.securelan.common.model;

import java.time.Instant;
import java.util.Objects;

public record AppEvent(
        AppEventType type,
        String message,
        Instant createdAt
) {
    public AppEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
