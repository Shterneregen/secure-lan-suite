package com.shterneregen.securelan.chat.transport

import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent
import com.shterneregen.securelan.chat.event.ChatSignalReceivedEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class ClientReceiveLoop(
    private val session: ChatSocketSession,
    private val nickname: String?,
    private val connected: AtomicBoolean,
    private val eventPublisher: ChatEventPublisher,
) : Runnable {
    override fun run() {
        try {
            while (connected.get()) {
                val message = session.readMessage() ?: break
                when (message.type()) {
                    WireMessageType.CHAT, WireMessageType.SYSTEM -> eventPublisher.publish(ChatMessageReceivedEvent(message.sender(), message.payload()))
                    WireMessageType.USER_JOINED -> eventPublisher.publish(ChatUserJoinedEvent(message.sender()))
                    WireMessageType.USER_LEFT -> eventPublisher.publish(ChatUserLeftEvent(message.sender()))
                    WireMessageType.SIGNAL -> eventPublisher.publish(ChatSignalReceivedEvent(RtcSignalCodec.deserialize(message.payload())))
                    else -> Unit
                }
            }
        } catch (e: IOException) {
            if (connected.get()) {
                eventPublisher.publish(ChatErrorEvent("Connection lost", e))
            }
        } finally {
            if (connected.getAndSet(false)) {
                eventPublisher.publish(ChatDisconnectedEvent(nickname, "Client disconnected"))
                try {
                    session.close()
                } catch (_: IOException) {
                }
            }
        }
    }
}
