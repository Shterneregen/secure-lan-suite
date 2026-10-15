package com.shterneregen.securelan.common.model

import java.time.Instant

/**
 * Describes an application event.
 *
 * @property type kind of event.
 * @property message human-readable description; must not be blank.
 * @property createdAt time at which the event was created.
 * @throws IllegalArgumentException if [message] is blank.
 */
@JvmRecord
@Deprecated("Historical model; use current wire or platform presentation models")
data class AppEvent(
    val type: AppEventType,
    val message: String,
    val createdAt: Instant,
) {
    init {
        require(message.isNotBlank()) { "message must not be blank" }
    }
}
