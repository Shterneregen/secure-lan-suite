package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import java.io.IOException

/**
 * Service that executes the mutually password-authenticated V2 handshake.
 *
 * SECURE_V2 binds nickname/capabilities to SRP-6a, verifies both evidence proofs, and
 * enables ordered AES-GCM records with separate direction keys. Passwords and traffic
 * keys are never transmitted. Old protocol versions are rejected without fallback.
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
     * @param expectedPassword The room password whose knowledge both peers must prove.
     * @param registry         The [NicknameRegistryService] used to check and reserve nicknames.
     * @return A [HandshakeResponse] whose [HandshakeResponse.accepted] flag indicates whether
     *         the client was admitted. Version/nickname rejection can return a response;
     *         cryptographic authentication failure closes the attempt through an exception.
     * @throws IOException on network, parsing or authentication failure.
     */
    @Throws(IOException::class)
    fun performServerHandshake(
        session: ChatSocketSession,
        expectedPassword: String,
        registry: NicknameRegistryService,
    ): HandshakeResponse
}
