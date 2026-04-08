package com.shterneregen.securelan.chat.event;

public record ChatErrorEvent(String message, Throwable cause) implements ChatCoreEvent {
}
