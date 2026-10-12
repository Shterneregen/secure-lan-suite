package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.ChatSession
import com.shterneregen.securelan.androidclient.protocol.CryptoCompat
import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Owns one desktop-compatible encrypted chat connection and any pending handshake.
 * Blocking IO runs on [Dispatchers.IO]. [close] may be called from lifecycle cleanup to
 * unblock readers immediately; a failed or interrupted handshake never retains its socket.
 */
class SecureChatClient : Closeable {
    private val lock = Any()
    @Volatile private var connecting: Socket? = null
    @Volatile
    private var session: ChatSession? = null
    private var nickname: String = ""

    /** Returns the server-assigned nickname after completing the password/key handshake. */
    suspend fun connect(
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
        createdSession.writeMessage(WireMessage(WireMessageType.HELLO, requestedNickname, SECURE_PROTOCOL))
        val serverKeyMessage = requireNotNull(createdSession.readMessage()) { "No handshake response from server" }
        if (serverKeyMessage.type == WireMessageType.REJECTED) {
            error(serverKeyMessage.payload.ifBlank { "Handshake rejected" })
        }
        require(serverKeyMessage.type == WireMessageType.SERVER_KEY) { "Unexpected handshake response: ${serverKeyMessage.type}" }
        val serverPublicKey = CryptoCompat.decodePublicKey(Base64.getDecoder().decode(serverKeyMessage.payload))
        val aesKey = CryptoCompat.generateAesKey()
        val payload = requestedNickname + "\n" + sessionPassword + "\n" +
            Base64.getEncoder().encodeToString(CryptoCompat.encodeSecretKey(aesKey)) + "\n" +
            capabilities.encode()
        val encryptedPayload = CryptoCompat.rsaEncrypt(payload.toByteArray(StandardCharsets.UTF_8), serverPublicKey)
        createdSession.writeMessage(WireMessage(WireMessageType.CLIENT_KEY, requestedNickname, Base64.getEncoder().encodeToString(encryptedPayload)))
        createdSession.enableTransportEncryption(aesKey)
        val response = requireNotNull(createdSession.readMessage()) { "No encrypted handshake response from server" }
        if (response.type != WireMessageType.ACCEPTED) {
            error(response.payload.ifBlank { "Handshake rejected" })
        }
        return response.payload
    }

    suspend fun sendMessage(text: String) = withContext(Dispatchers.IO) {
        checkNotNull(session) { "Chat disconnected" }.writeMessage(WireMessage(WireMessageType.CHAT, nickname, text))
    }

    suspend fun readMessage(): WireMessage? = withContext(Dispatchers.IO) {
        session?.readMessage()
    }

    suspend fun sendSignal(signal: RtcSignalEnvelope) = withContext(Dispatchers.IO) {
        val active = checkNotNull(session) { "Chat disconnected" }
        active.writeMessage(WireMessage(WireMessageType.SIGNAL, nickname, RtcSignalCodec.serialize(signal.withSender(nickname))))
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        val active = session
        try {
            runCatching { active?.writeMessage(WireMessage(WireMessageType.DISCONNECT, nickname, "")) }
        } finally {
            close()
        }
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
        private const val SECURE_PROTOCOL = "SECURE_V1"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val HANDSHAKE_TIMEOUT_MS = 15_000
    }
}
