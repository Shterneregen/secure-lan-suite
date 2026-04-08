package com.shterneregen.securelan.chat.event;

public record ChatUserLeftEvent(String nickname) implements ChatCoreEvent {
}
