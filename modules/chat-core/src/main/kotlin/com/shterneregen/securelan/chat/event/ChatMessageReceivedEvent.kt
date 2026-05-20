package com.shterneregen.securelan.chat.event

@JvmRecord
data class ChatMessageReceivedEvent(val senderNickname: String?, val text: String?) : ChatCoreEvent
