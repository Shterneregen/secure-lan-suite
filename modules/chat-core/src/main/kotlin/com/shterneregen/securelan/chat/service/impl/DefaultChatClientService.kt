package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.event.ChatConnectedEvent
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.event.ChatDisconnectReasons
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

/**
 * Default production implementation of [ChatClientService].
 *
 * ## Connection flow
 * 1. Opens a TCP connection using [ClientSocketFactory].
 * 2. Performs the mutually authenticated V2 handshake via [SimpleHandshakeService] (timeout controlled by
 *    [handshakeTimeoutMillis]).
 * 3. On success, publishes [ChatConnectedEvent] and starts a background [ClientReceiveLoop] thread
 *    that forwards incoming [com.shterneregen.securelan.chat.protocol.WireMessage]s as events.
 * 4. On failure, publishes [ChatErrorEvent] and cleans up.
 *
 * ## Thread safety
 * [connected] is an [AtomicBoolean].  [session] and [nickname] are only mutated from [connect]
 * (caller's thread) before the receive loop starts, so no additional synchronization is needed
 * for reads performed from the receive loop.
 *
 * @param eventPublisher        Callback that receives all [com.shterneregen.securelan.chat.event.ChatCoreEvent]s.
 * @param clientSocketFactory   Factory used to open the TCP connection.  Defaults to [ClientSocketFactory.systemDefault].
 * @param handshakeTimeoutMillis Read timeout (in milliseconds) applied to the session during
 *                               the handshake phase.  Must be positive.  Defaults to 5 000 ms.
 *
 * @throws NullPointerException     if [eventPublisher] or [clientSocketFactory] is `null`.
 * @throws IllegalArgumentException if [handshakeTimeoutMillis] is not positive.
 */
class DefaultChatClientService @JvmOverloads constructor(
    eventPublisher: ChatEventPublisher,
    clientSocketFactory: ClientSocketFactory = ClientSocketFactory.systemDefault(),
    private val handshakeTimeoutMillis: Int = DEFAULT_HANDSHAKE_TIMEOUT_MILLIS,
) : ChatClientService {

    private val eventPublisher: ChatEventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher")
    private val handshakeService: SecureHandshakeService = SimpleHandshakeService()
    private val clientSocketFactory: ClientSocketFactory =
        Objects.requireNonNull(clientSocketFactory, "clientSocketFactory")
    @Volatile private var connected = AtomicBoolean(false)
    private val identity = com.shterneregen.securelan.crypto.protocol.PeerIdentity()
    override fun fileIdentity(): com.shterneregen.securelan.crypto.protocol.PeerIdentity = identity

    private var session: ChatSocketSession? = null
    private var receiverThread: Thread? = null
    private var nickname: String? = null

    init {
        require(handshakeTimeoutMillis > 0) { "Handshake timeout must be positive" }
    }

    /**
     * Connects to the server, performs the handshake, and starts the receive loop.
     *
     * Returns `true` immediately if already connected.
     *
     * @param request Connection parameters.
     * @return `true` on success, `false` on failure.
     */
    override fun connect(request: ChatClientConnectRequest): Boolean {
        if (connected.get()) {
            return true
        }
        val connectionState = AtomicBoolean(false)
        connected = connectionState
        try {
            val activeSession = ChatSocketSession(
                clientSocketFactory.connect(TransportEndpoint.of(request.host, request.port)),
            )
            session = activeSession
            activeSession.setReadTimeout(handshakeTimeoutMillis)
            val response = handshakeService.performClientHandshake(
                activeSession,
                HandshakeRequest(request.nickname, request.sessionPassword, request.capabilities.withIdentity(identity.publicKey)),
            )
            if (!response.accepted()) {
                eventPublisher.publish(ChatErrorEvent(response.reason(), null))
                disconnect()
                return false
            }
            activeSession.setReadTimeout(0)
            nickname = response.nickname()
            connected.set(true)
            eventPublisher.publish(ChatConnectedEvent(nickname, activeSession.remoteAddress(), request.capabilities))
            receiverThread = Thread(
                ClientReceiveLoop(activeSession, nickname, connectionState) { event ->
                    if (session === activeSession) eventPublisher.publish(event)
                },
                "chat-client-receive-loop",
            ).also { it.start() }
            return true
        } catch (e: Exception) {
            eventPublisher.publish(ChatErrorEvent("Unable to connect to chat server", e))
            disconnect()
            return false
        }
    }

    /**
     * Sends a graceful `DISCONNECT` frame, closes the session, and publishes
     * [ChatDisconnectedEvent] if the client was connected or had a nickname.
     */
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
            eventPublisher.publish(ChatDisconnectedEvent(nickname ?: "", ChatDisconnectReasons.CLIENT_REQUEST))
        }
    }

    /**
     * Sends a chat message to the server.
     *
     * Silently ignored if not connected, [text] is `null`, or [text] is blank.
     *
     * @param text The message text to send.
     */
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

    /**
     * Sends a WebRTC signalling envelope to the server.
     *
     * The sender field of [signal] is overwritten with the local [nickname] to prevent spoofing.
     * Silently ignored if not connected or [signal] is `null`.
     *
     * @param signal The [RtcSignalEnvelope] to forward.
     */
    override fun sendSignal(signal: RtcSignalEnvelope?) {
        val activeSession = session
        if (!connected.get() || signal == null || activeSession == null) {
            return
        }
        try {
            val outboundSignal = signal.withSender(nickname ?: signal.fromPeer())
            activeSession.writeMessage(
                WireMessage(WireMessageType.SIGNAL, outboundSignal.fromPeer(), RtcSignalCodec.serialize(outboundSignal)),
            )
        } catch (e: IOException) {
            eventPublisher.publish(ChatErrorEvent("Unable to send realtime signal", e))
        }
    }

    /** Returns `true` if an authenticated session is currently active. */
    override fun isConnected(): Boolean = connected.get()

    companion object {
        /** Default read timeout (in milliseconds) applied during the handshake phase. */
        private const val DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 5_000
    }
}
