package com.shterneregen.securelan.chat.model;

public record ChatServerConfig(
        int port,
        String sessionPassword
) {
}
