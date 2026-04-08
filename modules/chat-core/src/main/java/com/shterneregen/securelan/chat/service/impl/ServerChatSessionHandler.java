package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent;
import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.NicknameRegistryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class ServerChatSessionHandler implements Runnable {
    private final Socket socket;
    private final String nickname;
    private final BufferedReader reader;
    private final InMemoryChatBroadcastService broadcastService;
    private final NicknameRegistryService nicknameRegistry;
    private final ChatEventPublisher eventPublisher;

    public ServerChatSessionHandler(
            Socket socket,
            String nickname,
            BufferedReader reader,
            InMemoryChatBroadcastService broadcastService,
            NicknameRegistryService nicknameRegistry,
            ChatEventPublisher eventPublisher
    ) {
        this.socket = socket;
        this.nickname = nickname;
        this.reader = reader;
        this.broadcastService = broadcastService;
        this.nicknameRegistry = nicknameRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                WireMessage message = WireMessage.deserialize(line);
                if (message.type() == WireMessageType.DISCONNECT) {
                    break;
                }
                if (message.type() == WireMessageType.CHAT) {
                    broadcastService.publishMessage(nickname, message.payload());
                }
            }
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Server session error for " + nickname, e));
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        broadcastService.removeClient(nickname);
        nicknameRegistry.unregister(nickname);
        broadcastService.publishUserLeft(nickname);
        eventPublisher.publish(new ChatUserLeftEvent(nickname));
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
