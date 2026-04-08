package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent;
import com.shterneregen.securelan.chat.model.ChatServerConfig;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.ChatServerService;
import com.shterneregen.securelan.chat.service.NicknameRegistryService;
import com.shterneregen.securelan.chat.service.SecureHandshakeService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChatServerService implements ChatServerService {
    private final ChatEventPublisher eventPublisher;
    private final SecureHandshakeService handshakeService;
    private final NicknameRegistryService nicknameRegistry;
    private final InMemoryChatBroadcastService broadcastService;
    private final ExecutorService sessionExecutor = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ChatServerConfig config;

    public DefaultChatServerService(ChatEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.handshakeService = new SimpleHandshakeService();
        this.nicknameRegistry = new InMemoryNicknameRegistryService();
        this.broadcastService = new InMemoryChatBroadcastService(new InMemoryChatHistoryService());
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
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                SecureHandshakeService.HandshakeDecision decision =
                        handshakeService.performServerHandshake(reader, writer, config.sessionPassword(), nicknameRegistry);
                if (!decision.accepted()) {
                    socket.close();
                    return;
                }
                String nickname = decision.nickname();
                broadcastService.addClient(nickname, writer);
                broadcastService.publishUserJoined(nickname);
                eventPublisher.publish(new ChatUserJoinedEvent(nickname));
                new ServerChatSessionHandler(socket, nickname, reader, broadcastService, nicknameRegistry, eventPublisher).run();
            } catch (IOException e) {
                eventPublisher.publish(new ChatErrorEvent("Error while handling client", e));
                try {
                    socket.close();
                } catch (IOException ignored) {
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
