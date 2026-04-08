package com.shterneregen.securelan.chat.service;

public interface ChatClientService {
    boolean connect(ChatClientConnectRequest request);
    void disconnect();
    void sendMessage(String text);
    boolean isConnected();
}
