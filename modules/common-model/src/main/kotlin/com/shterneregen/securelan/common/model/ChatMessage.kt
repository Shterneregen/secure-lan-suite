package com.shterneregen.securelan.common.model

import java.time.Instant

/**
 * A single chat message.
 *
 * @property id identifier for this message.
 * @property senderId identifier of the sending peer.
 * @property recipientId identifier of the intended recipient.
 * @property content text body; must not be blank.
 * @property createdAt time at which the message was created.
 * @throws IllegalArgumentException if [content] is blank.
 */
@JvmRecord
data class ChatMessage(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val createdAt: Instant,
) {
    init {
        require(content.isNotBlank()) { "content must not be blank" }
    }
}
