package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.ChatSession
import com.shterneregen.securelan.androidclient.protocol.CryptoCompat
import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64

class SecureChatClient {
    private var session: ChatSession? = null
    private var nickname: String = ""

    suspend fun connect(host: String, port: Int, requestedNickname: String, sessionPassword: String): String = withContext(Dispatchers.IO) {
        val createdSession = ChatSession(Socket(host, port))
        createdSession.writeMessage(WireMessage(WireMessageType.HELLO, requestedNickname, SECURE_PROTOCOL))
        val serverKeyMessage = requireNotNull(createdSession.readMessage()) { "No handshake response from server" }
        if (serverKeyMessage.type == WireMessageType.REJECTED) {
            createdSession.close()
            error(serverKeyMessage.payload.ifBlank { "Handshake rejected" })
        }
        require(serverKeyMessage.type == WireMessageType.SERVER_KEY) { "Unexpected handshake response: ${serverKeyMessage.type}" }
        val serverPublicKey = CryptoCompat.decodePublicKey(Base64.getDecoder().decode(serverKeyMessage.payload))
        val aesKey = CryptoCompat.generateAesKey()
        val payload = requestedNickname + "\n" + sessionPassword + "\n" +
            Base64.getEncoder().encodeToString(CryptoCompat.encodeSecretKey(aesKey))
        val encryptedPayload = CryptoCompat.rsaEncrypt(payload.toByteArray(StandardCharsets.UTF_8), serverPublicKey)
        createdSession.writeMessage(WireMessage(WireMessageType.CLIENT_KEY, requestedNickname, Base64.getEncoder().encodeToString(encryptedPayload)))
        createdSession.enableTransportEncryption(aesKey)
        val response = requireNotNull(createdSession.readMessage()) { "No encrypted handshake response from server" }
        if (response.type != WireMessageType.ACCEPTED) {
            createdSession.close()
            error(response.payload.ifBlank { "Handshake rejected" })
        }
        nickname = response.payload
        session = createdSession
        nickname
    }

    suspend fun sendMessage(text: String) = withContext(Dispatchers.IO) {
        session?.writeMessage(WireMessage(WireMessageType.CHAT, nickname, text))
    }

    suspend fun readMessage(): WireMessage? = withContext(Dispatchers.IO) {
        session?.readMessage()
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        val active = session ?: return@withContext
        runCatching { active.writeMessage(WireMessage(WireMessageType.DISCONNECT, nickname, "")) }
        runCatching { active.close() }
        session = null
    }

    companion object {
        private const val SECURE_PROTOCOL = "SECURE_V1"
    }
}
