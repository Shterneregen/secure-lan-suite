package com.shterneregen.securelan.chat.event

/**
 * Event fired when a peer leaves the chat room.
 *
 * This event is emitted both on the **server side** (when the server detects a client
 * disconnection) and propagated to all remaining **client sides** via a broadcast.
 *
 * @property nickname The nickname of the peer who left, or `null` if unknown.
 */
@JvmRecord
data class ChatUserLeftEvent(val nickname: String?) : ChatCoreEvent
