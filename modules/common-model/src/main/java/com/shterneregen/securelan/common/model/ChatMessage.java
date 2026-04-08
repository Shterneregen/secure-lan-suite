package com.shterneregen.securelan.common.model;

import java.time.Instant;
import java.util.Objects;

public record ChatMessage(
        String id,
        String senderId,
        String recipientId,
        String content,
        Instant createdAt
) {
    public ChatMessage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(senderId, "senderId must not be null");
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
