package com.shterneregen.securelan.chat

import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChatHealthTest {
    @Test fun unresponsivePeerReleasesIdleRead() {
        ServerSocket(0).use { server -> Socket("127.0.0.1", server.localPort).use { client ->
            server.accept().use { peer ->
                val worker = Executors.newSingleThreadExecutor()
                try {
                    ChatSocketSession(client).use { session ->
                        session.startHealth(25, 100)
                        val read = worker.submit<Boolean> { runCatching { session.readMessage() }.isFailure }
                        assertTrue(read.get(2, TimeUnit.SECONDS))
                        assertTrue(client.isClosed)
                    }
                } finally { worker.shutdownNow() }
            }
        } }
    }

    @Test fun healthyIdlePeerStaysConnectedAndProbesAreHiddenFromChat() {
        ServerSocket(0).use { server -> Socket("127.0.0.1", server.localPort).use { client ->
            server.accept().use { peer ->
                val worker = Executors.newFixedThreadPool(2)
                val left = ChatSocketSession(client)
                val right = ChatSocketSession(peer)
                try {
                    left.startHealth(25, 250)
                    right.startHealth(25, 250)
                    val incoming = worker.submit<WireMessage?> { right.readMessage() }
                    val other = worker.submit<WireMessage?> { left.readMessage() }
                    Thread.sleep(400)
                    assertFalse(client.isClosed)
                    right.writeMessage(WireMessage(WireMessageType.CHAT, "peer", "still here"))
                    assertEquals("still here", (other.get(2, TimeUnit.SECONDS) as WireMessage).payload())
                    assertFalse(incoming.isDone)
                } finally { left.close(); right.close(); worker.shutdownNow() }
            }
        } }
    }
}
