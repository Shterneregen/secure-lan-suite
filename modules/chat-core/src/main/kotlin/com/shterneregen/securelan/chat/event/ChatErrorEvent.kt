package com.shterneregen.securelan.chat.event

@JvmRecord
data class ChatErrorEvent(val message: String?, val cause: Throwable?) : ChatCoreEvent
