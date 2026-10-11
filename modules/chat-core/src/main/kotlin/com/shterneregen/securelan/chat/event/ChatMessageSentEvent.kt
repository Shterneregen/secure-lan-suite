package com.shterneregen.securelan.chat.event

/**
 * Event fired on the **client side** after a chat message has been successfully written to the
 * network socket (i.e. delivery to the server was attempted).
 *
 * This event is a best-effort echo of the sent message and can be used to display the local
 * user's own messages in the chat UI without waiting for a broadcast echo from the server.
 *
 * @property senderNickname The nickname of the local user who sent the message.
 * @property text           The text of the sent message.
 */
@JvmRecord
data class ChatMessageSentEvent(val senderNickname: String?, val text: String?) : ChatCoreEvent
