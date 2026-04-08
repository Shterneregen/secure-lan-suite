package com.shterneregen.securelan.chat.event;

public record ChatDisconnectedEvent(String nickname, String reason) implements ChatCoreEvent {
}
