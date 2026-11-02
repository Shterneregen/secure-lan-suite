package com.shterneregen.securelan.lantools.model

/** Safe application-facing outcomes. Cancellation is propagated as CancellationException. */
sealed interface LanResult<out T> {
    /** Operation-specific receipt; success never implies more than that receipt's documented scope. */
    data class Success<T>(val value: T) : LanResult<T>
    /** Stable error category and optional input-field key for application-owned localization. */
    data class Failure(val code: LanError, val field: String? = null) : LanResult<Nothing>
}

/**
 * Recoverable failure categories, without raw exceptions, packets or OS diagnostic strings.
 *
 * INTERFACE_SELECTION_REQUIRED asks the caller for a source binding, whereas missing/unsuitable
 * interfaces require refreshing or correcting that selection. NETWORK_CHANGED forbids a silent
 * retry on another route. TIMEOUT is the service deadline; caller cancellation is never an error code.
 */
enum class LanError {
    INVALID_INPUT,
    NETWORK_UNAVAILABLE,
    NETWORK_CHANGED,
    INTERFACE_UNAVAILABLE,
    INTERFACE_UNSUITABLE,
    INTERFACE_SELECTION_REQUIRED,
    PERMISSION_DENIED,
    UNSUPPORTED,
    TIMEOUT,
    RESOURCE_LIMIT,
    IO,
}
