package com.shterneregen.securelan.common.model

import java.time.Instant
import java.util.Objects

@JvmRecord
data class ChatMessage(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val createdAt: Instant,
) {
    init {
        Objects.requireNonNull(id, "id must not be null")
        Objects.requireNonNull(senderId, "senderId must not be null")
        Objects.requireNonNull(recipientId, "recipientId must not be null")
        Objects.requireNonNull(content, "content must not be null")
        Objects.requireNonNull(createdAt, "createdAt must not be null")
        require(content.isNotBlank()) { "content must not be blank" }
    }
}
