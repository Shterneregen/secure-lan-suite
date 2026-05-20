package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.event.ChatConnectedEvent
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent
import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.SecureHandshakeService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.chat.transport.ClientReceiveLoop
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.common.net.transport.ClientSocketFactory
import com.shterneregen.securelan.common.net.transport.TransportEndpoint
import java.io.IOException
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean

class DefaultChatClientService @JvmOverloads constructor(
    eventPublisher: ChatEventPublisher,
    clientSocketFactory: ClientSocketFactory = ClientSocketFactory.systemDefault(),
) : ChatClientService {
    private val eventPublisher: ChatEventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher")
    private val handshakeService: SecureHandshakeService = SimpleHandshakeService()
    private val clientSocketFactory: ClientSocketFactory = Objects.requireNonNull(clientSocketFactory, "clientSocketFactory")
    private val connected = AtomicBoolean(false)

    private var session: ChatSocketSession? = null
    private var receiverThread: Thread? = null
    private var nickname: String? = null

    override fun connect(request: ChatClientConnectRequest): Boolean {
        if (connected.get()) {
            return true
        }
        try {
            val activeSession = ChatSocketSession(clientSocketFactory.connect(TransportEndpoint.of(request.host, request.port)))
            session = activeSession
            val response = handshakeService.performClientHandshake(activeSession, HandshakeRequest(request.nickname, request.sessionPassword))
            if (!response.accepted()) {
                eventPublisher.publish(ChatErrorEvent(response.reason(), null))
                disconnect()
                return false
            }
            nickname = response.nickname()
            connected.set(true)
            eventPublisher.publish(ChatConnectedEvent(nickname, activeSession.remoteAddress()))
            receiverThread = Thread(ClientReceiveLoop(activeSession, nickname, connected, eventPublisher), "chat-client-receive-loop").also { it.start() }
            return true
        } catch (e: IOException) {
            eventPublisher.publish(ChatErrorEvent("Unable to connect to chat server", e))
            disconnect()
            return false
        }
    }

    override fun disconnect() {
        val wasConnected = connected.getAndSet(false)
        val activeSession = session
        if (activeSession != null) {
            try {
                activeSession.writeMessage(WireMessage(WireMessageType.DISCONNECT, nickname ?: "", ""))
            } catch (_: IOException) {
            }
            try {
                activeSession.close()
            } catch (_: IOException) {
            }
            session = null
        }
        if (wasConnected || nickname != null) {
            eventPublisher.publish(ChatDisconnectedEvent(nickname ?: "", "Client disconnected"))
        }
    }

    override fun sendMessage(text: String?) {
        val activeSession = session
        if (!connected.get() || text.isNullOrBlank() || activeSession == null) {
            return
        }
        try {
            activeSession.writeMessage(WireMessage(WireMessageType.CHAT, nickname, text))
            eventPublisher.publish(ChatMessageSentEvent(nickname, text))
        } catch (e: IOException) {
            eventPublisher.publish(ChatErrorEvent("Unable to send message", e))
        }
    }

    override fun sendSignal(signal: RtcSignalEnvelope?) {
        val activeSession = session
        if (!connected.get() || signal == null || activeSession == null) {
            return
        }
        try {
            val outboundSignal = signal.withSender(nickname ?: signal.fromPeer())
            activeSession.writeMessage(WireMessage(WireMessageType.SIGNAL, outboundSignal.fromPeer(), RtcSignalCodec.serialize(outboundSignal)))
        } catch (e: IOException) {
            eventPublisher.publish(ChatErrorEvent("Unable to send realtime signal", e))
        }
    }

    override fun isConnected(): Boolean = connected.get()
}
