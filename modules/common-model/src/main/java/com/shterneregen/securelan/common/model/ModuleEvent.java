package com.shterneregen.securelan.common.model;

import java.time.Instant;
import java.util.Objects;

public record ModuleEvent(
        ModuleType module,
        AppEventType type,
        String message,
        Instant createdAt
) {
    public ModuleEvent {
        Objects.requireNonNull(module, "module must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
