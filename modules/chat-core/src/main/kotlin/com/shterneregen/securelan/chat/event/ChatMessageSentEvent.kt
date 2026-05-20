package com.shterneregen.securelan.chat.event

@JvmRecord
data class ChatMessageSentEvent(val senderNickname: String?, val text: String?) : ChatCoreEvent
