package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.event.ChatCoreEvent

fun interface ChatEventPublisher {
    fun publish(event: ChatCoreEvent)
}
