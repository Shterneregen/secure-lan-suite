package com.shterneregen.securelan.chat.event;

public record ChatMessageReceivedEvent(String senderNickname, String text) implements ChatCoreEvent {
}
