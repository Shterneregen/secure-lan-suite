package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import java.io.IOException

interface SecureHandshakeService {
    @Throws(IOException::class)
    fun performClientHandshake(session: ChatSocketSession, request: HandshakeRequest): HandshakeResponse

    @Throws(IOException::class)
    fun performServerHandshake(session: ChatSocketSession, expectedPassword: String, registry: NicknameRegistryService): HandshakeResponse
}
