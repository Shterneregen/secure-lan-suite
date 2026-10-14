package com.shterneregen.securelan.chat

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.impl.InMemoryChatBroadcastService
import com.shterneregen.securelan.chat.service.impl.InMemoryChatHistoryService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SlowRecipientTest {
    @Test fun blockedWriterDoesNotDelayAnotherRecipient() {
        val writing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val slowSocket = object : Socket() {
            override fun getInputStream() = ByteArrayInputStream(ByteArray(0))
            override fun getOutputStream() = object : OutputStream() {
                override fun write(value: Int) {
                    writing.countDown()
                    try { if (!release.await(2, TimeUnit.SECONDS)) throw IOException("Slow output") }
                    catch (error: InterruptedException) { throw IOException("Closed output", error) }
                }
            }
            override fun close() { release.countDown(); super.close() }
        }
        val router = InMemoryChatBroadcastService(InMemoryChatHistoryService())
        ServerSocket(0).use { listener -> Socket("127.0.0.1", listener.localPort).use { readerSocket ->
            listener.accept().use { writerSocket ->
                readerSocket.soTimeout = 1000
                val reader = ChatSocketSession(readerSocket)
                router.addClient("slow", ChatSocketSession(slowSocket), PeerCapabilities.unknown())
                router.addClient("fast", ChatSocketSession(writerSocket), PeerCapabilities.unknown())
                try {
                    router.publishMessage("alice", "first")
                    assertTrue(writing.await(1, TimeUnit.SECONDS))
                    assertEquals("first", reader.readMessage()!!.payload())
                    router.publishMessage("alice", "second")
                    assertEquals("second", reader.readMessage()!!.payload())
                    assertEquals(1L, release.count)
                } finally { slowSocket.close(); router.removeClient("slow"); router.removeClient("fast") }
            }
        } }
    }
}
