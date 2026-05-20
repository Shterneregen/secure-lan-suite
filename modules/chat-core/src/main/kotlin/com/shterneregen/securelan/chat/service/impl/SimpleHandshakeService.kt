package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeStatus
import com.shterneregen.securelan.chat.service.NicknameRegistryService
import com.shterneregen.securelan.chat.service.SecureHandshakeService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.crypto.CryptoServices
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64

class SimpleHandshakeService @JvmOverloads constructor(
    private val cryptoServices: CryptoServices = CryptoServices.createDefault(),
) : SecureHandshakeService {
    @Throws(IOException::class)
    override fun performClientHandshake(session: ChatSocketSession, request: HandshakeRequest): HandshakeResponse {
        session.writeMessage(WireMessage(WireMessageType.HELLO, request.nickname, SECURE_PROTOCOL))

        val serverKeyMessage = session.readMessage() ?: throw IOException("No handshake response from server")
        if (serverKeyMessage.type() == WireMessageType.REJECTED) {
            return HandshakeResponse(HandshakeStatus.REJECTED, request.nickname, normalizeReason(serverKeyMessage.payload(), "Handshake rejected"))
        }
        if (serverKeyMessage.type() != WireMessageType.SERVER_KEY) {
            throw IOException("Unexpected handshake response: ${serverKeyMessage.type()}")
        }

        val serverPublicKey = cryptoServices.keyEncodingService().decodePublicKey(Base64.getDecoder().decode(serverKeyMessage.payload()))
        val sessionKey = cryptoServices.keyGenerationService().generateAesKey()
        val clientPayload = request.nickname + FIELD_SEPARATOR +
            request.sessionPassword + FIELD_SEPARATOR +
            Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodeSecretKey(sessionKey))
        val encryptedPayload = cryptoServices.rsaCryptoService().encrypt(clientPayload.toByteArray(StandardCharsets.UTF_8), serverPublicKey)
        session.writeMessage(WireMessage(WireMessageType.CLIENT_KEY, request.nickname, Base64.getEncoder().encodeToString(encryptedPayload)))

        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService())
        val response = session.readMessage() ?: throw IOException("No encrypted handshake response from server")
        return if (response.type() == WireMessageType.ACCEPTED) {
            HandshakeResponse(HandshakeStatus.ACCEPTED, response.payload(), "")
        } else {
            HandshakeResponse(HandshakeStatus.REJECTED, request.nickname, normalizeReason(response.payload(), "Handshake rejected"))
        }
    }

    @Throws(IOException::class)
    override fun performServerHandshake(session: ChatSocketSession, expectedPassword: String, registry: NicknameRegistryService): HandshakeResponse {
        val hello = session.readMessage()
        if (hello == null || hello.type() != WireMessageType.HELLO) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Invalid handshake"))
            return HandshakeResponse(HandshakeStatus.REJECTED, "", "Invalid handshake")
        }
        if (SECURE_PROTOCOL != hello.payload()) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Unsupported secure handshake protocol"))
            return HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Unsupported secure handshake protocol")
        }

        val serverKeyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()
        session.writeMessage(
            WireMessage(
                WireMessageType.SERVER_KEY,
                "server",
                Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodePublicKey(serverKeyPair.public)),
            ),
        )

        val clientKeyMessage = session.readMessage()
        if (clientKeyMessage == null || clientKeyMessage.type() != WireMessageType.CLIENT_KEY) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Invalid secure key exchange"))
            return HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Invalid secure key exchange")
        }

        val decryptedPayload = String(
            cryptoServices.rsaCryptoService().decrypt(Base64.getDecoder().decode(clientKeyMessage.payload()), serverKeyPair.private),
            StandardCharsets.UTF_8,
        )
        val parts = decryptedPayload.split(FIELD_SEPARATOR, limit = 3).toTypedArray()
        if (parts.size != 3) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Malformed secure handshake payload"))
            return HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Malformed secure handshake payload")
        }

        val nickname = parts[0].trim()
        val password = parts[1]
        val sessionKey = cryptoServices.keyEncodingService().decodeAesKey(Base64.getDecoder().decode(parts[2]))
        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService())

        if (nickname.isBlank()) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Nickname must not be blank"))
            return HandshakeResponse(HandshakeStatus.REJECTED, nickname, "Nickname must not be blank")
        }
        if (expectedPassword != password) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Wrong session password"))
            return HandshakeResponse(HandshakeStatus.REJECTED, nickname, "Wrong session password")
        }
        if (!registry.register(nickname)) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Nickname already in use"))
            return HandshakeResponse(HandshakeStatus.REJECTED, nickname, "Nickname already in use")
        }

        session.writeMessage(WireMessage(WireMessageType.ACCEPTED, "server", nickname))
        return HandshakeResponse(HandshakeStatus.ACCEPTED, nickname, "")
    }

    private fun normalizeReason(value: String?, fallback: String): String = if (value.isNullOrBlank()) fallback else value

    companion object {
        private const val SECURE_PROTOCOL = "SECURE_V1"
        private const val FIELD_SEPARATOR = "\n"
    }
}
