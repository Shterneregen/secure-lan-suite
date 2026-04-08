package com.shterneregen.securelan.chat.service;

import com.shterneregen.securelan.chat.model.ChatServerConfig;

public interface ChatServerService {
    void start(ChatServerConfig config);
    void stop();
    boolean isRunning();
    int connectedUsers();
}
