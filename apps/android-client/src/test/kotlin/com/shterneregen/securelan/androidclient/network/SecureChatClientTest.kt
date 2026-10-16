package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.ChatSession
import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    @Test(timeout = 10_000)
    fun eofReleasesSessionAndSameClientCanReconnect() = runBlocking {
        ServerSocket(0).use { server ->
            val peer = async(Dispatchers.IO) {
                repeat(2) {
                    server.accept().use { socket ->
                        ChatSession(socket).use { session ->
                            assertTrue(com.shterneregen.securelan.chat.service.impl.SimpleHandshakeService().performServerHandshake(
                                session, "password", com.shterneregen.securelan.chat.service.impl.InMemoryNicknameRegistryService()).accepted())
                        }
                    }
                }
            }
            SecureChatClient().use { client ->
                repeat(2) {
                    assertEquals("alice", connect(client, server.localPort))
                    assertEquals(null, client.readMessage())
                }
            }
            peer.await()
        }
    }

    @Test(timeout = 10_000)
    fun cancellationClosesBlockingSignalingWriteAndReleasesTransport(): Unit = runBlocking {
        val blockWrites = java.util.concurrent.atomic.AtomicBoolean()
        val entered = java.util.concurrent.CountDownLatch(1)
        val closed = java.util.concurrent.CountDownLatch(1)
        ServerSocket(0).use { server ->
            val peer = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    ChatSession(socket).use { session ->
                        com.shterneregen.securelan.chat.service.impl.SimpleHandshakeService().performServerHandshake(
                            session, "password", com.shterneregen.securelan.chat.service.impl.InMemoryNicknameRegistryService())
                        runCatching { while (session.readMessage() != null) { } }
                        Unit
                    }
                }
            }
            val client = SecureChatClient {
                object : java.net.Socket() {
                    override fun close() { super.close(); closed.countDown() }
                    override fun getOutputStream(): java.io.OutputStream {
                        val output = super.getOutputStream()
                        return object : java.io.OutputStream() {
                            override fun write(value: Int) = output.write(value)
                            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                                if (blockWrites.get()) {
                                    entered.countDown()
                                    check(closed.await(3, java.util.concurrent.TimeUnit.SECONDS))
                                    throw java.io.IOException("canceled stream")
                                }
                                output.write(bytes, offset, length)
                            }
                            override fun flush() = output.flush()
                        }
                    }
                }
            }
            client.use {
                connect(client, server.localPort)
                blockWrites.set(true)
                val sending = async { client.sendSignal(com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope.offer("alice", "bob",
                    com.shterneregen.securelan.common.model.rtc.RtcSessionMode.AUDIO, "data", "sdp")) }
                withContext(kotlinx.coroutines.Dispatchers.IO) { assertTrue(entered.await(3, java.util.concurrent.TimeUnit.SECONDS)) }
                sending.cancel()
                withTimeout(500) { sending.join() }
                assertTrue(closed.count == 0L)
                assertTrue(runCatching { client.sendMessage("must be disconnected") }.isFailure)
            }
            peer.await()
        }
    }

    private suspend fun connect(client: SecureChatClient, port: Int): String =
        client.connect("127.0.0.1", port, "alice", "password", PeerCapabilities.unknown())
}
