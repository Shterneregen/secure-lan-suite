package com.shterneregen.securelan.common.model

import java.time.Instant

/**
 * An application event associated with a specific [module].
 *
 * @property module module that emitted the event.
 * @property type kind of event.
 * @property message event description; an empty string is allowed.
 * @property createdAt time at which the event was created.
 */
@JvmRecord
@Deprecated("Historical model; use current wire or platform presentation models")
data class ModuleEvent(
    val module: ModuleType,
    val type: AppEventType,
    val message: String,
    val createdAt: Instant,
)
