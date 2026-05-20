package com.shterneregen.securelan.common.model

import java.time.Instant
import java.util.Objects

@JvmRecord
data class AppEvent(
    val type: AppEventType,
    val message: String,
    val createdAt: Instant,
) {
    init {
        Objects.requireNonNull(type, "type must not be null")
        Objects.requireNonNull(message, "message must not be null")
        Objects.requireNonNull(createdAt, "createdAt must not be null")
        require(message.isNotBlank()) { "message must not be blank" }
    }
}
