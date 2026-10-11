package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import java.io.IOException

/**
 * Service that executes the secure RSA+AES handshake between a chat client and server.
 *
 * The handshake protocol (SECURE_V1) proceeds as follows:
 * 1. Client sends a `HELLO` frame with its nickname and the protocol version token.
 * 2. Server responds with a `SERVER_KEY` frame containing its ephemeral RSA public key.
 * 3. Client encrypts a payload (nickname, password, AES session key, capabilities) with the
 *    server's RSA public key and sends it in a `CLIENT_KEY` frame.
 * 4. Both sides enable AES-GCM transport encryption using the negotiated session key.
 * 5. Server sends an `ACCEPTED` or `REJECTED` frame (now encrypted) to complete the handshake.
 *
 * ## Usage
 * - Call [performClientHandshake] on the client side immediately after opening the TCP connection.
 * - Call [performServerHandshake] on the server side for each accepted incoming connection.
 *
 * @see com.shterneregen.securelan.chat.service.impl.SimpleHandshakeService
 */
interface SecureHandshakeService {

    /**
     * Executes the client side of the handshake protocol.
     *
     * Blocking: reads and writes multiple frames from/to [session].
     *
     * @param session The [ChatSocketSession] connected to the server.  Must already have a
     *                read timeout set to avoid blocking indefinitely.
     * @param request The [HandshakeRequest] carrying the client's credentials and capabilities.
     * @return A [HandshakeResponse] whose [HandshakeResponse.accepted] flag indicates success.
     * @throws IOException if a network error occurs during the handshake.
     */
    @Throws(IOException::class)
    fun performClientHandshake(session: ChatSocketSession, request: HandshakeRequest): HandshakeResponse

    /**
     * Executes the server side of the handshake protocol.
     *
     * Blocking: reads and writes multiple frames from/to [session].
     *
     * @param session          The [ChatSocketSession] for the newly accepted client connection.
     * @param expectedPassword The room password that the client must supply.
     * @param registry         The [NicknameRegistryService] used to check and reserve nicknames.
     * @return A [HandshakeResponse] whose [HandshakeResponse.accepted] flag indicates whether
     *         the client was admitted.  Rejection frames are written to [session] before returning.
     * @throws IOException if a network error occurs during the handshake.
     */
    @Throws(IOException::class)
    fun performServerHandshake(
        session: ChatSocketSession,
        expectedPassword: String,
        registry: NicknameRegistryService,
    ): HandshakeResponse
}
