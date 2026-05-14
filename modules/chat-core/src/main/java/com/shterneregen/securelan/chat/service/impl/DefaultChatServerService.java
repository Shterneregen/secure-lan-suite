package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse;
import com.shterneregen.securelan.chat.service.ChatBroadcastService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.ChatHistoryService;
import com.shterneregen.securelan.chat.service.ChatServerConfig;
import com.shterneregen.securelan.chat.service.ChatServerService;
import com.shterneregen.securelan.chat.service.NicknameRegistryService;
import com.shterneregen.securelan.chat.service.SecureHandshakeService;
import com.shterneregen.securelan.chat.transport.ChatSocketSession;
import com.shterneregen.securelan.chat.transport.ServerChatSessionHandler;
import com.shterneregen.securelan.common.net.transport.SocketClose;
import com.shterneregen.securelan.common.net.transport.TcpServer;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChatServerService implements ChatServerService {
    private final ChatEventPublisher eventPublisher;
    private final SecureHandshakeService handshakeService;
    private final NicknameRegistryService nicknameRegistry;
    private final ChatHistoryService historyService;
    private final ChatBroadcastService broadcastService;
    private final Set<ChatSocketSession> activeSessions = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final TcpServer tcpServer;

    private ChatServerConfig config;

    public DefaultChatServerService(ChatEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.handshakeService = new SimpleHandshakeService();
        this.nicknameRegistry = new InMemoryNicknameRegistryService();
        this.historyService = new InMemoryChatHistoryService();
        this.broadcastService = new InMemoryChatBroadcastService(historyService);
        this.tcpServer = new TcpServer("chat-server");
    }

    @Override
    public void start(ChatServerConfig config) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            this.config = config;
            tcpServer.start(config.port(), this::handleConnection, this::publishAcceptError);
        } catch (IOException | RuntimeException e) {
            running.set(false);
            throw new IllegalStateException("Unable to start chat server", e);
        }
    }

    private void publishAcceptError(String message, Throwable cause) {
        if (running.get()) {
            eventPublisher.publish(new ChatErrorEvent(message, cause));
        }
    }

    private void handleConnection(Socket socket) {
        if (!running.get()) {
            SocketClose.closeQuietly(socket);
            return;
        }
        ChatSocketSession session = null;
        try {
            session = new ChatSocketSession(socket);
            activeSessions.add(session);
            if (!running.get()) {
                session.close();
                return;
            }
            HandshakeResponse response = handshakeService.performServerHandshake(session, config.sessionPassword(), nicknameRegistry);
            if (!response.accepted()) {
                session.close();
                return;
            }
            String nickname = response.nickname();
            broadcastService.syncPeers(session, nickname);
            broadcastService.addClient(nickname, session);
            broadcastService.publishUserJoined(nickname);
            eventPublisher.publish(new ChatUserJoinedEvent(nickname, session.remoteAddress()));
            new ServerChatSessionHandler(session, nickname, broadcastService, nicknameRegistry, eventPublisher).run();
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Error while handling client", e));
            SocketClose.closeQuietly(session);
        } finally {
            if (session != null) {
                activeSessions.remove(session);
            }
        }
    }

    @Override
    public void stop() {
        running.set(false);
        tcpServer.close();
        activeSessions.forEach(session -> {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        });
        activeSessions.clear();
        config = null;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int connectedUsers() {
        return nicknameRegistry.getActiveNicknames().size();
    }
}
