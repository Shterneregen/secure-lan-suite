package com.shterneregen.securelan.chat.event;

public record ChatMessageSentEvent(String senderNickname, String text) implements ChatCoreEvent {
}
