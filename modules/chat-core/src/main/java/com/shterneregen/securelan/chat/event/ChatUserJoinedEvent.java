package com.shterneregen.securelan.chat.event;

public record ChatUserJoinedEvent(String nickname) implements ChatCoreEvent {
}
