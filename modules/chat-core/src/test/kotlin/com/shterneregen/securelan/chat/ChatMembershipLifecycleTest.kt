package com.shterneregen.securelan.chat

import com.shterneregen.securelan.chat.event.*
import com.shterneregen.securelan.chat.service.*
import com.shterneregen.securelan.chat.service.impl.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ChatMembershipLifecycleTest {
    @Test fun disconnectDoesNotWaitForConcurrentBlockedWriter() {
        val writing = CountDownLatch(1)
        val socketClosed = CountDownLatch(1)
        val blockWrites = AtomicBoolean()
        val port = ServerSocket(0).use { it.localPort }
        val server = DefaultChatServerService({})
        val factory = com.shterneregen.securelan.common.net.transport.ClientSocketFactory { endpoint ->
            object : java.net.Socket(endpoint.host(), endpoint.port()) {
                override fun close() { super.close(); socketClosed.countDown() }
                override fun getOutputStream(): java.io.OutputStream {
                    val output = super.getOutputStream()
                    return object : java.io.OutputStream() {
                        override fun write(value: Int) = output.write(value)
                        override fun write(bytes: ByteArray, offset: Int, length: Int) {
                            if (blockWrites.get()) {
                                writing.countDown()
                                check(socketClosed.await(3, TimeUnit.SECONDS))
                                throw java.io.IOException("closed while writing")
                            }
                            output.write(bytes, offset, length)
                        }
                        override fun flush() = output.flush()
                    }
                }
            }
        }
        val client = DefaultChatClientService({}, factory)
        val workers = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            server.start(ChatServerConfig(port, "password"))
            assertTrue(client.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "password")))
            blockWrites.set(true)
            val send = workers.submit<ChatSendOutcome> { client.sendMessage("blocked") }
            assertTrue(writing.await(2, TimeUnit.SECONDS))
            workers.submit { client.disconnect() }.get(1, TimeUnit.SECONDS)
            assertFalse(client.isConnected())
            assertEquals(ChatSendOutcome.FAILED, send.get(1, TimeUnit.SECONDS))
        } finally { socketClosed.countDown(); client.disconnect(); server.stop(); workers.shutdownNow() }
    }

    @Test fun delayedLeaveCannotFollowTheReplacementJoin() {
        val firstJoined = CountDownLatch(1)
        val leaving = CountDownLatch(1)
        val release = CountDownLatch(1)
        val ordered = java.util.concurrent.CopyOnWriteArrayList<String>()
        val server = DefaultChatServerService { event ->
            if (event is ChatUserJoinedEvent) { ordered += "JOIN"; firstJoined.countDown() }
            if (event is ChatUserLeftEvent) {
                leaving.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                ordered += "LEFT"
            }
        }
        val port = ServerSocket(0).use { it.localPort }
        val first = DefaultChatClientService({})
        val second = DefaultChatClientService({})
        val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            server.start(ChatServerConfig(port, "password"))
            assertTrue(first.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "password")))
            assertTrue(firstJoined.await(3, TimeUnit.SECONDS))
            first.disconnect()
            assertTrue(leaving.await(3, TimeUnit.SECONDS))
            val replacement = worker.submit<Boolean> { second.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "password")) }
            release.countDown()
            assertTrue(replacement.get(5, TimeUnit.SECONDS))
            val limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (ordered.size < 3 && System.nanoTime() < limit) Thread.yield()
            assertEquals(listOf("JOIN", "LEFT", "JOIN"), ordered.toList())
        } finally { release.countDown(); first.disconnect(); second.disconnect(); server.stop(); worker.shutdownNow() }
    }

    @Test fun joinPublicationFailureReleasesRegistrationAndServiceCanRehost() {
        val throwOnce = AtomicBoolean(true)
        val left = CountDownLatch(1)
        val server = DefaultChatServerService { event ->
            if (event is ChatUserJoinedEvent && throwOnce.getAndSet(false)) throw IllegalStateException("injected join publisher failure")
            if (event is ChatUserLeftEvent) left.countDown()
        }
        val port = ServerSocket(0).use { it.localPort }
        val first = DefaultChatClientService({})
        val second = DefaultChatClientService({})
        try {
            server.start(ChatServerConfig(port, "password"))
            first.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "password"))
            assertTrue(left.await(3, TimeUnit.SECONDS))
            // Handshake retry must work even on the same run, then after rehost.
            assertTrue(second.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "password")))
            server.stop()
            second.disconnect()
            server.start(ChatServerConfig(port, "password"))
            assertTrue(second.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "password")))
        } finally { first.disconnect(); second.disconnect(); server.stop() }
    }
}
