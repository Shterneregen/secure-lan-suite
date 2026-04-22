package com.shterneregen.securelan.chat.service;

import com.shterneregen.securelan.chat.transport.ChatSocketSession;

public interface ChatBroadcastService {
    void addClient(String nickname, ChatSocketSession session);
    void removeClient(String nickname);
    void syncPeers(ChatSocketSession session, String excludeNickname);
    void publishUserJoined(String nickname);
    void publishUserLeft(String nickname);
    void publishMessage(String senderNickname, String text);
    void publishSignal(String senderNickname, String payload);
}
