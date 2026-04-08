package com.shterneregen.securelan.chat.service;

public interface ChatBroadcastService {
    void publishUserJoined(String nickname);
    void publishUserLeft(String nickname);
    void publishMessage(String senderNickname, String text);
}
