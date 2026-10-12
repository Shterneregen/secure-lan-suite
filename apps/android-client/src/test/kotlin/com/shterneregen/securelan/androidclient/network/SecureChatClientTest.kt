package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.ChatSession
import com.shterneregen.securelan.androidclient.protocol.CryptoCompat
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
import java.nio.charset.StandardCharsets
import java.util.Base64

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
                        val hello = session.readMessage()!!
                        assertEquals("SECURE_V1", hello.payload)
                        val keys = CryptoCompat.generateRsaKeyPair()
                        session.writeMessage(WireMessage(WireMessageType.SERVER_KEY, "server", Base64.getEncoder().encodeToString(keys.public.encoded)))
                        val clientKey = session.readMessage()!!
                        val payload = String(CryptoCompat.rsaDecrypt(Base64.getDecoder().decode(clientKey.payload), keys.private), StandardCharsets.UTF_8).split('\n')
                        assertEquals("password", payload[1])
                        session.enableTransportEncryption(CryptoCompat.decodeSecretKey(Base64.getDecoder().decode(payload[2])))
                        session.writeMessage(WireMessage(WireMessageType.ACCEPTED, "server", "assigned"))
                        assertEquals("hello", session.readMessage()!!.payload)
                        assertEquals(WireMessageType.DISCONNECT, session.readMessage()!!.type)
                        assertEquals(null, session.readMessage())
                    }
                }
            }
            SecureChatClient().use { client ->
                assertEquals("assigned", connect(client, server.localPort))
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
