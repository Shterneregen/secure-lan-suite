package com.shterneregen.securelan.common.model

/**
 * Pairs a [ChatMessage] with its current [DeliveryStatus].
 *
 * @property message chat message being tracked.
 * @property status current delivery status of the message.
 */
@JvmRecord
@Deprecated("Historical model; use current wire or platform presentation models")
data class ChatEnvelope(
    val message: ChatMessage,
    val status: DeliveryStatus,
)
