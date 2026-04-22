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
    public void syncPeers(ChatSocketSession session, String excludeNickname) {
        clients.keySet().stream()
                .filter(nickname -> excludeNickname == null || !nickname.equalsIgnoreCase(excludeNickname))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(nickname -> writeQuietly(session, new WireMessage(WireMessageType.USER_JOINED, nickname, "")));
    }

    @Override
    public void publishUserJoined(String nickname) {
        String line = "[system] " + nickname + " joined the chat";
        historyService.append(line);
        broadcast(new WireMessage(WireMessageType.USER_JOINED, nickname, ""));
        broadcast(new WireMessage(WireMessageType.SYSTEM, "system", line));
    }

    @Override
    public void publishUserLeft(String nickname) {
        String line = "[system] " + nickname + " left the chat";
        historyService.append(line);
        broadcast(new WireMessage(WireMessageType.USER_LEFT, nickname, ""));
        broadcast(new WireMessage(WireMessageType.SYSTEM, "system", line));
    }

    @Override
    public void publishMessage(String senderNickname, String text) {
        String line = senderNickname + ": " + text;
        historyService.append(line);
        broadcast(new WireMessage(WireMessageType.CHAT, senderNickname, text));
    }

    @Override
    public void publishSignal(String senderNickname, String payload) {
        broadcast(new WireMessage(WireMessageType.SIGNAL, senderNickname, payload));
    }

    private void broadcast(WireMessage message) {
        clients.forEach((nickname, session) -> writeQuietly(session, message));
    }

    private void writeQuietly(ChatSocketSession session, WireMessage message) {
        try {
            session.writeMessage(message);
        } catch (IOException ignored) {
        }
    }
}
