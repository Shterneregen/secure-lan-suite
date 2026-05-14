package com.shterneregen.securelan.chat.event;

public record ChatUserJoinedEvent(String nickname, String remoteAddress) implements ChatCoreEvent {
    public ChatUserJoinedEvent(String nickname) {
        this(nickname, "");
    }
}
