package com.shterneregen.securelan.chat.event

/**
 * Event fired when a non-fatal error occurs in the chat subsystem.
 *
 * Consumers should log this event and, where appropriate, surface a user-visible error message.
 * The session may still be alive after this event is delivered; check the connection state
 * separately.
 *
 * @property message A human-readable description of the error, or `null` if no message is available.
 * @property cause   The underlying exception that caused the error, or `null` if the cause is
 *                   not available or not applicable.
 */
@JvmRecord
data class ChatErrorEvent(val message: String?, val cause: Throwable?) : ChatCoreEvent
