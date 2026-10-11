package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeStatus
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.NicknameRegistryService
import com.shterneregen.securelan.chat.service.SecureHandshakeService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.crypto.CryptoServices
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Production implementation of [SecureHandshakeService] using a custom RSA+AES-GCM protocol
 * (protocol version token: `SECURE_V1`).
 *
 * ## Protocol overview
 * ```
 * Client                                    Server
 *   |-- HELLO (nickname, "SECURE_V1") ------->|
 *   |<-- SERVER_KEY (RSA pub key, b64) --------|
 *   |-- CLIENT_KEY (RSA-encrypted payload) -->|
 *   |       payload = nickname \n              |
 *   |                password \n              |
 *   |                AES-key-b64 \n           |
 *   |                caps-string             |
 *   |       (both sides enable AES-GCM)       |
 *   |<-- ACCEPTED (nickname) / REJECTED (reason)
 * ```
 *
 * The server generates an ephemeral RSA key pair per connection; the AES session key is chosen
 * by the client and transmitted inside the RSA-encrypted payload.  All subsequent frames are
 * protected by AES-GCM using that session key.
 *
 * @param cryptoServices The crypto service provider to use.  Defaults to [CryptoServices.createDefault].
 */
class SimpleHandshakeService @JvmOverloads constructor(
    private val cryptoServices: CryptoServices = CryptoServices.createDefault(),
) : SecureHandshakeService {

    /**
     * Executes the client side of the SECURE_V1 handshake.
     *
     * Steps performed:
     * 1. Send `HELLO`.
     * 2. Receive `SERVER_KEY` (or `REJECTED`).
     * 3. Encrypt and send `CLIENT_KEY`.
     * 4. Enable AES-GCM transport encryption.
     * 5. Receive `ACCEPTED` or `REJECTED`.
     *
     * @param session The connected [ChatSocketSession]; must have a read timeout set.
     * @param request The credentials and capabilities to transmit.
     * @return The server's [HandshakeResponse].
     * @throws IOException if any frame read or write fails.
     */
    @Throws(IOException::class)
    override fun performClientHandshake(session: ChatSocketSession, request: HandshakeRequest): HandshakeResponse {
        session.writeMessage(WireMessage(WireMessageType.HELLO, request.nickname, SECURE_PROTOCOL))

        val serverKeyMessage = session.readMessage()
            ?: throw IOException("No handshake response from server")

        if (serverKeyMessage.type() == WireMessageType.REJECTED) {
            return HandshakeResponse(
                HandshakeStatus.REJECTED,
                request.nickname,
                normalizeReason(serverKeyMessage.payload(), "Handshake rejected"),
            )
        }
        if (serverKeyMessage.type() != WireMessageType.SERVER_KEY) {
            throw IOException("Unexpected handshake response: ${serverKeyMessage.type()}")
        }

        val serverPublicKey = cryptoServices.keyEncodingService()
            .decodePublicKey(Base64.getDecoder().decode(serverKeyMessage.payload()))
        val sessionKey = cryptoServices.keyGenerationService().generateAesKey()
        val clientPayload = request.nickname + FIELD_SEPARATOR +
            request.sessionPassword + FIELD_SEPARATOR +
            Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodeSecretKey(sessionKey)) +
            FIELD_SEPARATOR +
            request.capabilities.encode()
        val encryptedPayload = cryptoServices.rsaCryptoService()
            .encrypt(clientPayload.toByteArray(StandardCharsets.UTF_8), serverPublicKey)
        session.writeMessage(
            WireMessage(
                WireMessageType.CLIENT_KEY,
                request.nickname,
                Base64.getEncoder().encodeToString(encryptedPayload),
            ),
        )

        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService())
        val response = session.readMessage()
            ?: throw IOException("No encrypted handshake response from server")
        return if (response.type() == WireMessageType.ACCEPTED) {
            HandshakeResponse(HandshakeStatus.ACCEPTED, response.payload(), "")
        } else {
            HandshakeResponse(
                HandshakeStatus.REJECTED,
                request.nickname,
                normalizeReason(response.payload(), "Handshake rejected"),
            )
        }
    }

    /**
     * Executes the server side of the SECURE_V1 handshake.
     *
     * Steps performed:
     * 1. Receive and validate `HELLO`.
     * 2. Generate an ephemeral RSA key pair and send `SERVER_KEY`.
     * 3. Receive and decrypt `CLIENT_KEY`.
     * 4. Enable AES-GCM transport encryption.
     * 5. Validate nickname uniqueness and password, then send `ACCEPTED` or `REJECTED`.
     *
     * @param session          The accepted [ChatSocketSession]; must have a read timeout set.
     * @param expectedPassword The room password.
     * @param registry         Used to reserve the client's nickname atomically.
     * @return The handshake outcome.
     * @throws IOException if any frame read or write fails.
     */
    @Throws(IOException::class)
    override fun performServerHandshake(
        session: ChatSocketSession,
        expectedPassword: String,
        registry: NicknameRegistryService,
    ): HandshakeResponse {
        val hello = session.readMessage()
        if (hello == null || hello.type() != WireMessageType.HELLO) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Invalid handshake"))
            return HandshakeResponse(HandshakeStatus.REJECTED, "", "Invalid handshake")
        }
        if (SECURE_PROTOCOL != hello.payload()) {
            session.writeMessage(
                WireMessage(WireMessageType.REJECTED, "server", "Unsupported secure handshake protocol"),
            )
            return HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Unsupported secure handshake protocol")
        }

        val serverKeyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()
        session.writeMessage(
            WireMessage(
                WireMessageType.SERVER_KEY,
                "server",
                Base64.getEncoder().encodeToString(
                    cryptoServices.keyEncodingService().encodePublicKey(serverKeyPair.public),
                ),
            ),
        )

        val clientKeyMessage = session.readMessage()
        if (clientKeyMessage == null || clientKeyMessage.type() != WireMessageType.CLIENT_KEY) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Invalid secure key exchange"))
            return HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Invalid secure key exchange")
        }

        val decryptedPayload = String(
            cryptoServices.rsaCryptoService()
                .decrypt(Base64.getDecoder().decode(clientKeyMessage.payload()), serverKeyPair.private),
            StandardCharsets.UTF_8,
        )
        val parts = decryptedPayload.split(FIELD_SEPARATOR, limit = 4).toTypedArray()
        if (parts.size !in 3..4) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Malformed secure handshake payload"))
            return HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Malformed secure handshake payload")
        }

        val nickname = parts[0].trim()
        val password = parts[1]
        val sessionKey = cryptoServices.keyEncodingService()
            .decodeAesKey(Base64.getDecoder().decode(parts[2]))
        val capabilities = if (parts.size >= 4) PeerCapabilities.decode(parts[3]) else PeerCapabilities.unknown()
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
        return HandshakeResponse(HandshakeStatus.ACCEPTED, nickname, "", capabilities)
    }

    /**
     * Returns [value] if it is non-null and non-blank, otherwise returns [fallback].
     */
    private fun normalizeReason(value: String?, fallback: String): String =
        if (value.isNullOrBlank()) fallback else value

    companion object {
        /** Protocol version token sent in the `HELLO` frame. */
        private const val SECURE_PROTOCOL = "SECURE_V1"

        /**
         * Delimiter used to separate fields within the RSA-encrypted client payload.
         * A literal newline character is used because it cannot appear in Base64 strings or
         * plain-text nicknames/passwords, making splitting unambiguous.
         */
        private const val FIELD_SEPARATOR = "\n"
    }
}
