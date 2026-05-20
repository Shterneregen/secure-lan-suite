package com.shterneregen.securelan.common.model

import java.time.Instant
import java.util.Objects

@JvmRecord
data class ModuleEvent(
    val module: ModuleType,
    val type: AppEventType,
    val message: String,
    val createdAt: Instant,
) {
    init {
        Objects.requireNonNull(module, "module must not be null")
        Objects.requireNonNull(type, "type must not be null")
        Objects.requireNonNull(message, "message must not be null")
        Objects.requireNonNull(createdAt, "createdAt must not be null")
    }
}
