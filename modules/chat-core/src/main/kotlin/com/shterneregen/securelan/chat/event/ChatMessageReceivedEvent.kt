package com.shterneregen.securelan.chat.event

/**
 * Event fired on the **client side** when a chat message is received from the server.
 *
 * This event is raised for both regular chat messages ([com.shterneregen.securelan.chat.protocol.WireMessageType.CHAT])
 * and system announcements ([com.shterneregen.securelan.chat.protocol.WireMessageType.SYSTEM]).
 *
 * @property senderNickname The nickname of the peer who sent the message, or `null` if unknown.
 * @property text           The message text, or `null` if the payload was empty.
 */
@JvmRecord
data class ChatMessageReceivedEvent(val senderNickname: String?, val text: String?) : ChatCoreEvent
