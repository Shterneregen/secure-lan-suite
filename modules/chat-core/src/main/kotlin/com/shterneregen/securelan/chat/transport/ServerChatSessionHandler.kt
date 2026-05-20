package com.shterneregen.securelan.chat.transport

import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.service.ChatBroadcastService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.NicknameRegistryService
import java.io.IOException

class ServerChatSessionHandler(
    private val session: ChatSocketSession,
    private val nickname: String,
    private val broadcastService: ChatBroadcastService,
    private val nicknameRegistry: NicknameRegistryService,
    private val eventPublisher: ChatEventPublisher,
) : Runnable {
    override fun run() {
        try {
            while (true) {
                val message = session.readMessage() ?: break
                if (message.type() == WireMessageType.DISCONNECT) {
                    break
                }
                if (message.type() == WireMessageType.CHAT && message.payload().isNotBlank()) {
                    broadcastService.publishMessage(nickname, message.payload())
                } else if (message.type() == WireMessageType.SIGNAL && message.payload().isNotBlank()) {
                    broadcastService.publishSignal(nickname, message.payload())
                }
            }
        } catch (e: IOException) {
            eventPublisher.publish(ChatErrorEvent("Server session error for $nickname", e))
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        broadcastService.removeClient(nickname)
        nicknameRegistry.unregister(nickname)
        broadcastService.publishUserLeft(nickname)
        eventPublisher.publish(ChatUserLeftEvent(nickname))
        try {
            session.close()
        } catch (_: IOException) {
        }
    }
}
