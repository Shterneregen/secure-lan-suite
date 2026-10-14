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
import com.shterneregen.securelan.crypto.protocol.PasswordAuthenticatedHandshake
import java.io.IOException

/** SRP-6a V2 handshake. Version, nickname and capabilities bind mutual authentication. No V1 fallback. */
class SimpleHandshakeService @JvmOverloads constructor(
    @Suppress("UNUSED_PARAMETER") cryptoServices: CryptoServices = CryptoServices.createDefault(),
) : SecureHandshakeService {
    override fun performClientHandshake(session: ChatSocketSession, request: HandshakeRequest): HandshakeResponse = session.handshake {
        val hello = "$PROTOCOL\n${request.capabilities.encode()}"
        session.writeMessage(WireMessage(WireMessageType.HELLO, request.nickname, hello))
        val keys = PasswordAuthenticatedHandshake.client(request.sessionPassword, context(request.nickname, hello),
            { session.writeMessage(WireMessage(WireMessageType.CLIENT_KEY, request.nickname, it)) },
            { receive(session, WireMessageType.SERVER_KEY) })
        session.enableTransportEncryption(keys, true)
        val response = session.readMessage() ?: throw IOException("No authenticated handshake response")
        when (response.type()) {
            WireMessageType.ACCEPTED -> HandshakeResponse(HandshakeStatus.ACCEPTED, response.payload(), "")
            WireMessageType.REJECTED -> HandshakeResponse(HandshakeStatus.REJECTED, request.nickname, response.payload())
            else -> throw IOException("Invalid authenticated handshake response")
        }
    }

    override fun performServerHandshake(session: ChatSocketSession, expectedPassword: String, registry: NicknameRegistryService): HandshakeResponse = session.handshake {
        val hello = session.readMessage() ?: throw IOException("Missing handshake")
        if (hello.type() != WireMessageType.HELLO || !hello.payload().startsWith("$PROTOCOL\n")) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Unsupported secure protocol; update both clients"))
            return@handshake HandshakeResponse(HandshakeStatus.REJECTED, "", "Unsupported secure protocol")
        }
        val nickname = hello.sender().trim()
        if (nickname.isBlank() || nickname.length > 64 || nickname.any { it.isISOControl() } || nickname.equals("system", true)) throw IOException("Invalid nickname")
        val capabilities = PeerCapabilities.decode(hello.payload().substringAfter('\n'))
        val keys = PasswordAuthenticatedHandshake.server(expectedPassword, context(hello.sender(), hello.payload()),
            { session.writeMessage(WireMessage(WireMessageType.SERVER_KEY, "server", it)) },
            { receive(session, WireMessageType.CLIENT_KEY) })
        session.enableTransportEncryption(keys, false)
        if (!registry.register(nickname)) {
            session.writeMessage(WireMessage(WireMessageType.REJECTED, "server", "Nickname already in use"))
            return@handshake HandshakeResponse(HandshakeStatus.REJECTED, nickname, "Nickname already in use")
        }
        try {
            session.writeMessage(WireMessage(WireMessageType.ACCEPTED, "server", nickname))
        } catch (error: Exception) {
            registry.unregister(nickname)
            throw error
        }
        HandshakeResponse(HandshakeStatus.ACCEPTED, nickname, "", capabilities)
    }

    private fun receive(session: ChatSocketSession, type: WireMessageType): String {
        val message = session.readMessage() ?: throw IOException("Authentication failed")
        if (message.type() != type) throw IOException("Authentication failed or incompatible protocol")
        return message.payload()
    }

    private fun context(nickname: String, hello: String): String = "chat:$nickname\u0000$hello"
    companion object { const val PROTOCOL = "SECURE_V2" }
}
