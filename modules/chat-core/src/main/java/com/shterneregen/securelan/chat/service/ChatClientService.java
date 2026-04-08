package com.shterneregen.securelan.chat.service;

import com.shterneregen.securelan.chat.model.ChatClientConnectRequest;

public interface ChatClientService {
    boolean connect(ChatClientConnectRequest request);
    void disconnect();
    void sendMessage(String text);
    boolean isConnected();
}
