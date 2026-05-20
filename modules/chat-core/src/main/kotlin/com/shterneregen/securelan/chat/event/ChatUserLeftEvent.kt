package com.shterneregen.securelan.chat.event

@JvmRecord
data class ChatUserLeftEvent(val nickname: String?) : ChatCoreEvent
