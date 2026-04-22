package com.shterneregen.securelan.chat;

import com.shterneregen.securelan.chat.event.ChatConnectedEvent;
import com.shterneregen.securelan.chat.event.ChatCoreEvent;
import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent;
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent;
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest;
import com.shterneregen.securelan.chat.service.ChatClientService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.ChatServerConfig;
import com.shterneregen.securelan.chat.service.ChatServerService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService;
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureChatIntegrationTest {
    private final List<ChatServerService> servers = new CopyOnWriteArrayList<>();
    private final List<ChatClientService> clients = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        clients.forEach(ChatClientService::disconnect);
        servers.forEach(ChatServerService::stop);
    }

    @Test
    void secureHandshakeAndMessageExchangeShouldSucceed() throws Exception {
        List<ChatCoreEvent> events = new CopyOnWriteArrayList<>();
        ChatEventPublisher publisher = events::add;

        int port = freePort();
        ChatServerService server = track(new DefaultChatServerService(publisher));
        server.start(new ChatServerConfig(port, "chatpass"));

        ChatClientService client = track(new DefaultChatClientService(publisher));
        assertTrue(client.connect(new ChatClientConnectRequest("127.0.0.1", port, "alice", "chatpass")));
        assertTrue(await(events, e -> e instanceof ChatConnectedEvent, 2_000));

        client.sendMessage("hello encrypted chat");
        assertTrue(await(events, e -> e instanceof ChatMessageReceivedEvent message && message.text().equals("hello encrypted chat"), 2_000));
    }

    @Test
    void wrongPasswordShouldBeRejected() throws Exception {
        List<ChatCoreEvent> events = new CopyOnWriteArrayList<>();
        ChatEventPublisher publisher = events::add;

        int port = freePort();
        ChatServerService server = track(new DefaultChatServerService(publisher));
        server.start(new ChatServerConfig(port, "chatpass"));

        ChatClientService client = track(new DefaultChatClientService(publisher));
        assertFalse(client.connect(new ChatClientConnectRequest("127.0.0.1", port, "alice", "wrong")));
        assertTrue(await(events, e -> e instanceof ChatErrorEvent error && error.message().contains("Wrong session password"), 2_000));
    }

    @Test
    void peerPresenceShouldBeSyncedForExistingAndNewClients() throws Exception {
        List<ChatCoreEvent> serverEvents = new CopyOnWriteArrayList<>();
        List<ChatCoreEvent> aliceEvents = new CopyOnWriteArrayList<>();
        List<ChatCoreEvent> bobEvents = new CopyOnWriteArrayList<>();

        int port = freePort();
        ChatServerService server = track(new DefaultChatServerService(serverEvents::add));
        server.start(new ChatServerConfig(port, "chatpass"));

        ChatClientService alice = track(new DefaultChatClientService(aliceEvents::add));
        assertTrue(alice.connect(new ChatClientConnectRequest("127.0.0.1", port, "alice", "chatpass")));
        assertTrue(await(aliceEvents, e -> e instanceof ChatConnectedEvent, 2_000));

        ChatClientService bob = track(new DefaultChatClientService(bobEvents::add));
        assertTrue(bob.connect(new ChatClientConnectRequest("127.0.0.1", port, "bob", "chatpass")));
        assertTrue(await(bobEvents, e -> e instanceof ChatConnectedEvent, 2_000));
        assertTrue(await(bobEvents, e -> e instanceof ChatUserJoinedEvent joined && joined.nickname().equals("alice"), 2_000));
        assertTrue(await(aliceEvents, e -> e instanceof ChatUserJoinedEvent joined && joined.nickname().equals("bob"), 2_000));
    }

    private ChatServerService track(ChatServerService server) {
        servers.add(server);
        return server;
    }

    private ChatClientService track(ChatClientService client) {
        clients.add(client);
        return client;
    }

    private static boolean await(List<ChatCoreEvent> events, Predicate<ChatCoreEvent> predicate, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (events.stream().anyMatch(predicate)) {
                return true;
            }
            Thread.sleep(25L);
        }
        return events.stream().anyMatch(predicate);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
