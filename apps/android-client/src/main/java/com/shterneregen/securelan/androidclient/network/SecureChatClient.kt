package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.ChatSession
import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Owns one desktop-compatible encrypted chat connection and any pending handshake.
 * Blocking IO runs on [Dispatchers.IO]. [close] may be called from lifecycle cleanup to
 * unblock readers immediately; a failed or interrupted handshake never retains its socket.
 */
class SecureChatClient : RoomClient {
    override val fileIdentity = com.shterneregen.securelan.crypto.protocol.PeerIdentity()
    private val lock = Any()
    @Volatile private var connecting: Socket? = null
    @Volatile
    private var session: ChatSession? = null
    private var nickname: String = ""

    /** Returns the server-assigned nickname after completing the password/key handshake. */
    override suspend fun connect(
        host: String,
        port: Int,
        requestedNickname: String,
        sessionPassword: String,
        capabilities: PeerCapabilities,
    ): String = withContext(Dispatchers.IO) {
        val socket = synchronized(lock) {
            check(session == null && connecting == null) { "Chat already connected or connecting" }
            Socket().also { connecting = it }
        }
        var accepted = false
        try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket.soTimeout = HANDSHAKE_TIMEOUT_MS
            val createdSession = ChatSession(socket)
            val acceptedNickname = performHandshake(createdSession, requestedNickname, sessionPassword, capabilities)
            currentCoroutineContext().ensureActive()
            synchronized(lock) {
                check(connecting === socket && !socket.isClosed) { "Chat connection stopped" }
                socket.soTimeout = 0
                nickname = acceptedNickname
                session = createdSession
                connecting = null
                accepted = true
            }
            acceptedNickname
        } finally {
            synchronized(lock) { if (connecting === socket) connecting = null }
            if (!accepted) socket.close()
        }
    }

    private fun performHandshake(
        createdSession: ChatSession,
        requestedNickname: String,
        sessionPassword: String,
        capabilities: PeerCapabilities,
    ): String {
        val response = com.shterneregen.securelan.chat.service.impl.SimpleHandshakeService().performClientHandshake(
            createdSession,
            com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest(
                requestedNickname, sessionPassword, capabilities.withIdentity(fileIdentity.publicKey),
            ),
        )
        check(response.accepted()) { response.reason().ifBlank { "Handshake rejected" } }
        return response.nickname()
    }

    override suspend fun sendMessage(text: String) = withContext(Dispatchers.IO) {
        checkNotNull(session) { "Chat disconnected" }.writeMessage(WireMessage(WireMessageType.CHAT, nickname, text))
    }

    override suspend fun readMessage(): WireMessage? = withContext(Dispatchers.IO) {
        session?.readMessage()
    }

    override suspend fun sendSignal(signal: RtcSignalEnvelope) = withContext(Dispatchers.IO) {
        val active = checkNotNull(session) { "Chat disconnected" }
        active.writeMessage(WireMessage(WireMessageType.SIGNAL, nickname, RtcSignalCodec.serialize(signal.withSender(nickname))))
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        val active = session
        try {
            runCatching { active?.writeMessage(WireMessage(WireMessageType.DISCONNECT, nickname, "")) }
        } finally {
            close()
        }
        Unit
    }

    /** Closes active and pending sockets synchronously, including after the ViewModel scope is canceled. */
    override fun close() {
        val resources = synchronized(lock) {
            val captured = session to connecting
            session = null
            connecting = null
            captured
        }
        runCatching { resources.first?.close() }
        runCatching { resources.second?.close() }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val HANDSHAKE_TIMEOUT_MS = 15_000
    }
}
