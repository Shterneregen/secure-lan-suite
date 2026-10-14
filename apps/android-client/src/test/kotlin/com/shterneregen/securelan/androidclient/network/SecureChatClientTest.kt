package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.ChatSession
import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket

class SecureChatClientTest {
    @Test(timeout = 10_000)
    fun malformedServerKeyClosesHandshakeSocket() = runBlocking {
        ServerSocket(0).use { server ->
            val peer = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    socket.soTimeout = 3_000
                    ChatSession(socket).use { session ->
                        assertEquals(WireMessageType.HELLO, session.readMessage()!!.type)
                        session.writeMessage(WireMessage(WireMessageType.SERVER_KEY, "server", "invalid-key"))
                        assertEquals(null, session.readMessage())
                    }
                }
            }
            SecureChatClient().use { client ->
                val result = runCatching { connect(client, server.localPort) }
                assertTrue(result.isFailure)
            }
            peer.await()
        }
    }

    @Test(timeout = 10_000)
    fun closeUnblocksPendingHandshake() = runBlocking {
        ServerSocket(0).use { server ->
            val hello = CompletableDeferred<Unit>()
            val peer = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    socket.soTimeout = 3_000
                    ChatSession(socket).use { session ->
                        session.readMessage()
                        hello.complete(Unit)
                        assertEquals(null, session.readMessage())
                    }
                }
            }
            SecureChatClient().use { client ->
                val attempt = async { runCatching { connect(client, server.localPort) } }
                withTimeout(3_000) { hello.await() }
                client.close()
                assertTrue(attempt.await().isFailure)
            }
            peer.await()
        }
    }

    @Test(timeout = 10_000)
    fun encryptedHandshakeAndDisconnectRemainCompatible() = runBlocking {
        ServerSocket(0).use { server ->
            val peer = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    socket.soTimeout = 3_000
                    ChatSession(socket).use { session ->
                        val response = com.shterneregen.securelan.chat.service.impl.SimpleHandshakeService().performServerHandshake(
                            session, "password", com.shterneregen.securelan.chat.service.impl.InMemoryNicknameRegistryService(),
                        )
                        assertTrue(response.accepted())
                        assertTrue(response.capabilities().identityKey().isNotBlank())
                        assertEquals("hello", session.readMessage()!!.payload)
                        assertEquals(WireMessageType.DISCONNECT, session.readMessage()!!.type)
                        assertEquals(null, session.readMessage())
                    }
                }
            }
            SecureChatClient().use { client ->
                assertEquals("alice", connect(client, server.localPort))
                client.sendMessage("hello")
                client.disconnect()
            }
            peer.await()
        }
    }

    @Test
    fun sendingWithoutConnectionFails() = runBlocking {
        SecureChatClient().use { client ->
            assertTrue(runCatching { client.sendMessage("hello") }.isFailure)
        }
    }

    private suspend fun connect(client: SecureChatClient, port: Int): String =
        client.connect("127.0.0.1", port, "alice", "password", PeerCapabilities.unknown())
}
