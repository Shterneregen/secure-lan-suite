package com.shterneregen.securelan.chat.model;

public record ChatClientIdentity(
        String nickname,
        String sessionPassword
) {
}
