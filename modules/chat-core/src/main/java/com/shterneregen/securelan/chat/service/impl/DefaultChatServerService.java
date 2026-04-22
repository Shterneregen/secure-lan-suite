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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChatServerService implements ChatServerService {
    private final ChatEventPublisher eventPublisher;
    private final SecureHandshakeService handshakeService;
    private final NicknameRegistryService nicknameRegistry;
    private final ChatHistoryService historyService;
    private final ChatBroadcastService broadcastService;
    private final ExecutorService sessionExecutor = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ChatServerConfig config;

    public DefaultChatServerService(ChatEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.handshakeService = new SimpleHandshakeService();
        this.nicknameRegistry = new InMemoryNicknameRegistryService();
        this.historyService = new InMemoryChatHistoryService();
        this.broadcastService = new InMemoryChatBroadcastService(historyService);
    }

    @Override
    public void start(ChatServerConfig config) {
        if (running.get()) {
            return;
        }
        try {
            this.config = config;
            this.serverSocket = new ServerSocket(config.port());
            running.set(true);
            acceptThread = new Thread(this::acceptLoop, "chat-server-accept-loop");
            acceptThread.start();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start chat server", e);
        }
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                handleConnection(socket);
            } catch (IOException e) {
                if (running.get()) {
                    eventPublisher.publish(new ChatErrorEvent("Error while accepting connection", e));
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        sessionExecutor.submit(() -> {
            ChatSocketSession session = null;
            try {
                session = new ChatSocketSession(socket);
                HandshakeResponse response = handshakeService.performServerHandshake(session, config.sessionPassword(), nicknameRegistry);
                if (!response.accepted()) {
                    session.close();
                    return;
                }
                String nickname = response.nickname();
                broadcastService.syncPeers(session, nickname);
                broadcastService.addClient(nickname, session);
                broadcastService.publishUserJoined(nickname);
                eventPublisher.publish(new ChatUserJoinedEvent(nickname));
                new ServerChatSessionHandler(session, nickname, broadcastService, nicknameRegistry, eventPublisher).run();
            } catch (IOException e) {
                eventPublisher.publish(new ChatErrorEvent("Error while handling client", e));
                if (session != null) {
                    try {
                        session.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
    }

    @Override
    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        sessionExecutor.shutdownNow();
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
