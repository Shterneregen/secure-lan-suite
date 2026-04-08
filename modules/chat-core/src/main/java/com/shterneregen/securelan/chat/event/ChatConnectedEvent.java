package com.shterneregen.securelan.chat.event;

public record ChatConnectedEvent(String nickname, String remoteAddress) implements ChatCoreEvent {
}
