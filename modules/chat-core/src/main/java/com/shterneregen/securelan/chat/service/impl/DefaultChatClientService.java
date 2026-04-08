package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.event.ChatConnectedEvent;
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent;
import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent;
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent;
import com.shterneregen.securelan.chat.model.ChatClientConnectRequest;
import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.service.ChatClientService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.SecureHandshakeService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChatClientService implements ChatClientService {
    private final ChatEventPublisher eventPublisher;
    private final SecureHandshakeService handshakeService;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread receiverThread;
    private String nickname;

    public DefaultChatClientService(ChatEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.handshakeService = new SimpleHandshakeService();
    }

    @Override
    public boolean connect(ChatClientConnectRequest request) {
        if (connected.get()) {
            return true;
        }
        try {
            socket = new Socket(request.host(), request.port());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            nickname = handshakeService.performClientHandshake(reader, writer, request.nickname(), request.sessionPassword());
            connected.set(true);
            eventPublisher.publish(new ChatConnectedEvent(nickname, socket.getRemoteSocketAddress().toString()));
            receiverThread = new Thread(this::receiveLoop, "chat-client-receive-loop");
            receiverThread.start();
            return true;
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Unable to connect to chat server", e));
            disconnect();
            return false;
        }
    }

    @Override
    public void disconnect() {
        connected.set(false);
        if (writer != null) {
            try {
                synchronized (writer) {
                    writer.write(new WireMessage(WireMessageType.DISCONNECT, nickname == null ? "" : nickname, "").serialize());
                    writer.newLine();
                    writer.flush();
                }
            } catch (IOException ignored) {
            }
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        eventPublisher.publish(new ChatDisconnectedEvent(nickname == null ? "" : nickname, "Client disconnected"));
    }

    @Override
    public void sendMessage(String text) {
        if (!connected.get() || text == null || text.isBlank()) {
            return;
        }
        try {
            synchronized (writer) {
                writer.write(new WireMessage(WireMessageType.CHAT, nickname, text).serialize());
                writer.newLine();
                writer.flush();
            }
            eventPublisher.publish(new ChatMessageSentEvent(nickname, text));
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Unable to send message", e));
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    private void receiveLoop() {
        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                WireMessage message = WireMessage.deserialize(line);
                if (message.type() == WireMessageType.CHAT) {
                    eventPublisher.publish(new ChatMessageReceivedEvent(message.sender(), message.payload()));
                } else if (message.type() == WireMessageType.SYSTEM) {
                    eventPublisher.publish(new ChatMessageReceivedEvent("system", message.payload()));
                }
            }
        } catch (IOException e) {
            if (connected.get()) {
                eventPublisher.publish(new ChatErrorEvent("Connection lost", e));
            }
        } finally {
            if (connected.get()) {
                disconnect();
            }
        }
    }
}
