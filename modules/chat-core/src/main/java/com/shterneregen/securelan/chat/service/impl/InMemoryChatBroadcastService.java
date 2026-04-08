package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.service.ChatBroadcastService;
import com.shterneregen.securelan.chat.service.ChatHistoryService;
import com.shterneregen.securelan.chat.transport.ChatSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChatBroadcastService implements ChatBroadcastService {
    private final Map<String, ChatSocketSession> clients = new ConcurrentHashMap<>();
    private final ChatHistoryService historyService;

    public InMemoryChatBroadcastService(ChatHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public void addClient(String nickname, ChatSocketSession session) {
        clients.put(nickname, session);
    }

    @Override
    public void removeClient(String nickname) {
        clients.remove(nickname);
    }

    @Override
    public void publishUserJoined(String nickname) {
        String line = "[system] " + nickname + " joined the chat";
        historyService.append(line);
        broadcast(new WireMessage(WireMessageType.SYSTEM, "system", line));
    }

    @Override
    public void publishUserLeft(String nickname) {
        String line = "[system] " + nickname + " left the chat";
        historyService.append(line);
        broadcast(new WireMessage(WireMessageType.SYSTEM, "system", line));
    }

    @Override
    public void publishMessage(String senderNickname, String text) {
        String line = senderNickname + ": " + text;
        historyService.append(line);
        broadcast(new WireMessage(WireMessageType.CHAT, senderNickname, text));
    }

    private void broadcast(WireMessage message) {
        clients.forEach((nickname, session) -> {
            try {
                session.writeMessage(message);
            } catch (IOException ignored) {
            }
        });
    }
}
