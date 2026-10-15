package com.shterneregen.securelan.chat

import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.impl.InMemoryChatBroadcastService
import com.shterneregen.securelan.chat.service.impl.InMemoryChatHistoryService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.common.model.rtc.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class SignalRoutingTest {
    @Test fun `signals are addressed sender is canonical and invalid signals never broadcast`() {
        val history = InMemoryChatHistoryService()
        val router = InMemoryChatBroadcastService(history)
        ServerSocket(0).use { server ->
            Socket("127.0.0.1", server.localPort).use { bobSocket ->
                server.accept().use { bobServer ->
                    Socket("127.0.0.1", server.localPort).use { carolSocket ->
                        server.accept().use { carolServer ->
                            router.addClient("bob", ChatSocketSession(bobServer), PeerCapabilities.unknown())
                            router.addClient("carol", ChatSocketSession(carolServer), PeerCapabilities.unknown())
                            val bob = ChatSocketSession(bobSocket)
                            val carol = ChatSocketSession(carolSocket)
                            bob.setReadTimeout(500); carol.setReadTimeout(150)
                            val spoofed = RtcSignalEnvelope.offer("mallory", "BOB", RtcSessionMode.AUDIO, "data", "sdp")
                            router.publishSignal("alice", RtcSignalCodec.serialize(spoofed))
                            val received = bob.readMessage()!!
                            assertEquals(WireMessageType.SIGNAL, received.type())
                            assertEquals("alice", received.sender())
                            assertEquals("alice", RtcSignalCodec.deserialize(received.payload()).fromPeer())
                            assertThrows(SocketTimeoutException::class.java) { carol.readMessage() }
                            router.publishSignal("alice", "garbage")
                            router.publishSignal("alice", RtcSignalCodec.serialize(spoofed).replaceFirst("v=1", "v=999"))
                            assertThrows(SocketTimeoutException::class.java) { bob.readMessage() }
                            // An unsupported signal must not poison the room's text path.
                            router.publishMessage("alice", "still connected")
                            assertEquals("still connected", bob.readMessage()!!.payload())
                            router.publishSignal("alice", RtcSignalCodec.serialize(RtcSignalEnvelope.offer("alice", "", RtcSessionMode.AUDIO, "data", "sdp")))
                            router.publishSignal("alice", RtcSignalCodec.serialize(RtcSignalEnvelope.offer("alice", "missing", RtcSessionMode.AUDIO, "data", "sdp")))
                            assertThrows(SocketTimeoutException::class.java) { bob.readMessage() }
                            assertEquals(1, history.getAll().size)
                        }
                    }
                }
            }
        }
    }
}
