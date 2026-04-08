package com.shterneregen.securelan.chat.service;

public interface ChatServerService {
    void start(ChatServerConfig config);
    void stop();
    boolean isRunning();
    int connectedUsers();
}
