package com.shterneregen.securelan.chat.event

/**
 * Event fired when the client has been disconnected from the chat server.
 *
 * The [reason] field contains a human-readable explanation of why the disconnection occurred.
 * Predefined reason strings are available in [ChatDisconnectReasons].
 *
 * @property nickname The nickname that was active during the session, or `null` if the session
 *                    was never fully established.
 * @property reason   A human-readable disconnection reason (see [ChatDisconnectReasons]), or
 *                    `null` if the reason is unknown.
 */
@JvmRecord
data class ChatDisconnectedEvent(val nickname: String?, val reason: String?) : ChatCoreEvent
