package com.shterneregen.securelan.chat

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.impl.InMemoryNicknameRegistryService
import com.shterneregen.securelan.chat.service.impl.SimpleHandshakeService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.crypto.protocol.PeerIdentity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChatHealthCompatibilityTest {
    @Test fun healthNegotiationPreservesIdentityAndMessagesWithLegacyPeersInBothDirections() {
        for ((clientHealth, serverHealth) in listOf(true to true, false to true, true to false)) {
            val identity = PeerIdentity()
            val caps = PeerCapabilities.android("test", 6051).withIdentity(identity.publicKey)
            // The old compact parser ignores the additional eighth token and keeps the full identity.
            assertEquals(identity.publicKey, PeerCapabilities.decode(caps.encode() + "|health:v1").identityKey())
            ServerSocket(0).use { listener ->
                val worker = Executors.newSingleThreadExecutor()
                try {
                    val received = worker.submit<String> {
                        listener.accept().use { socket -> ChatSocketSession(socket).use { session ->
                            val response = SimpleHandshakeService(healthEnabled = serverHealth).performServerHandshake(session, "password", InMemoryNicknameRegistryService())
                            assertTrue(response.accepted())
                            assertEquals(identity.publicKey, response.capabilities().identityKey())
                            val text = session.readMessage()!!.payload()
                            session.writeMessage(WireMessage(WireMessageType.CHAT, "server", text))
                            text
                        } }
                    }
                    Socket("127.0.0.1", listener.localPort).use { socket -> ChatSocketSession(socket).use { session ->
                        assertTrue(SimpleHandshakeService(healthEnabled = clientHealth).performClientHandshake(session,
                            HandshakeRequest("alice", "password", caps)).accepted())
                        session.writeMessage(WireMessage(WireMessageType.CHAT, "alice", "encrypted chat"))
                        assertEquals("encrypted chat", session.readMessage()!!.payload())
                    } }
                    assertEquals("encrypted chat", received.get(5, TimeUnit.SECONDS))
                } finally { worker.shutdownNow() }
            }
        }
    }
}
