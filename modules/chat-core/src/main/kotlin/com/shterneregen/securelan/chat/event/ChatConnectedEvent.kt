package com.shterneregen.securelan.chat.event

@JvmRecord
data class ChatConnectedEvent(val nickname: String?, val remoteAddress: String?) : ChatCoreEvent
