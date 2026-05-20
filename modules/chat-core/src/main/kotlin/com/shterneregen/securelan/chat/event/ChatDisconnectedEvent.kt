package com.shterneregen.securelan.chat.event

@JvmRecord
data class ChatDisconnectedEvent(val nickname: String?, val reason: String?) : ChatCoreEvent
