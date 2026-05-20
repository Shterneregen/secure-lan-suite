package com.shterneregen.securelan.common.model

import java.util.Objects

@JvmRecord
data class ChatEnvelope(
    val message: ChatMessage,
    val status: DeliveryStatus,
) {
    init {
        Objects.requireNonNull(message, "message must not be null")
        Objects.requireNonNull(status, "status must not be null")
    }
}
