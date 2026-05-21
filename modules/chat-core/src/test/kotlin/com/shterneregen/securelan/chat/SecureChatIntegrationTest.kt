package com.shterneregen.securelan.chat

import com.shterneregen.securelan.chat.event.ChatConnectedEvent
import com.shterneregen.securelan.chat.event.ChatCoreEvent
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList

class SecureChatIntegrationTest {
    private val servers = CopyOnWriteArrayList<ChatServerService>()
    private val clients = CopyOnWriteArrayList<ChatClientService>()

    @AfterEach
    fun tearDown() {
        clients.forEach { it.disconnect() }
        servers.forEach { it.stop() }
    }

    @Test
    fun secureHandshakeAndMessageExchangeShouldSucceed() {
        val events = CopyOnWriteArrayList<ChatCoreEvent>()
        val publisher = ChatEventPublisher { events.add(it) }

        val port = freePort()
        val server = track(DefaultChatServerService(publisher))
        server.start(ChatServerConfig(port, "chatpass"))

        val client = track(DefaultChatClientService(publisher))
        assertTrue(client.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "chatpass")))
        assertTrue(await(events, { it is ChatConnectedEvent }, 2_000))

        client.sendMessage("hello encrypted chat")
        assertTrue(await(events, { it is ChatMessageReceivedEvent && it.text == "hello encrypted chat" }, 2_000))
    }

    @Test
    fun wrongPasswordShouldBeRejected() {
        val events = CopyOnWriteArrayList<ChatCoreEvent>()
        val publisher = ChatEventPublisher { events.add(it) }

        val port = freePort()
        val server = track(DefaultChatServerService(publisher))
        server.start(ChatServerConfig(port, "chatpass"))

        val client = track(DefaultChatClientService(publisher))
        assertFalse(client.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "wrong")))
        assertTrue(await(events, { it is ChatErrorEvent && it.message?.contains("Wrong session password") == true }, 2_000))
    }

    @Test
    fun peerPresenceShouldBeSyncedForExistingAndNewClients() {
        val serverEvents = CopyOnWriteArrayList<ChatCoreEvent>()
        val aliceEvents = CopyOnWriteArrayList<ChatCoreEvent>()
        val bobEvents = CopyOnWriteArrayList<ChatCoreEvent>()

        val port = freePort()
        val server = track(DefaultChatServerService(ChatEventPublisher { serverEvents.add(it) }))
        server.start(ChatServerConfig(port, "chatpass"))

        val alice = track(DefaultChatClientService(ChatEventPublisher { aliceEvents.add(it) }))
        assertTrue(alice.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "chatpass")))
        assertTrue(await(aliceEvents, { it is ChatConnectedEvent }, 2_000))

        val bob = track(DefaultChatClientService(ChatEventPublisher { bobEvents.add(it) }))
        assertTrue(bob.connect(ChatClientConnectRequest("127.0.0.1", port, "bob", "chatpass")))
        assertTrue(await(bobEvents, { it is ChatConnectedEvent }, 2_000))
        assertTrue(await(bobEvents, { it is ChatUserJoinedEvent && it.nickname == "alice" }, 2_000))
        assertTrue(await(aliceEvents, { it is ChatUserJoinedEvent && it.nickname == "bob" }, 2_000))
    }

    @Test
    fun stopShouldDisconnectClientsAndPreventFurtherChat() {
        val aliceEvents = CopyOnWriteArrayList<ChatCoreEvent>()
        val bobEvents = CopyOnWriteArrayList<ChatCoreEvent>()

        val port = freePort()
        val server = track(DefaultChatServerService(ChatEventPublisher { }))
        server.start(ChatServerConfig(port, "chatpass"))

        val alice = track(DefaultChatClientService(ChatEventPublisher { aliceEvents.add(it) }))
        assertTrue(alice.connect(ChatClientConnectRequest("127.0.0.1", port, "alice", "chatpass")))
        assertTrue(await(aliceEvents, { it is ChatConnectedEvent }, 2_000))

        val bob = track(DefaultChatClientService(ChatEventPublisher { bobEvents.add(it) }))
        assertTrue(bob.connect(ChatClientConnectRequest("127.0.0.1", port, "bob", "chatpass")))
        assertTrue(await(bobEvents, { it is ChatConnectedEvent }, 2_000))

        server.stop()

        assertFalse(server.isRunning())
        assertTrue(await(aliceEvents, { it is ChatDisconnectedEvent }, 2_000))
        assertTrue(await(bobEvents, { it is ChatDisconnectedEvent }, 2_000))
        assertFalse(alice.isConnected())
        assertFalse(bob.isConnected())

        alice.sendMessage("message after stop")
        assertFalse(await(bobEvents, { it is ChatMessageReceivedEvent && it.text == "message after stop" }, 300))
    }

    private fun track(server: ChatServerService): ChatServerService {
        servers.add(server)
        return server
    }

    private fun track(client: ChatClientService): ChatClientService {
        clients.add(client)
        return client
    }

    private fun await(events: List<ChatCoreEvent>, predicate: (ChatCoreEvent) -> Boolean, timeoutMillis: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (events.any(predicate)) {
                return true
            }
            Thread.sleep(25L)
        }
        return events.any(predicate)
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }
}
