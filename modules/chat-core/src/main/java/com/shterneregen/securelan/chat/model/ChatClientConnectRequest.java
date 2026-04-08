package com.shterneregen.securelan.chat.model;

public record ChatClientConnectRequest(
        String host,
        int port,
        String nickname,
        String sessionPassword
) {
}
