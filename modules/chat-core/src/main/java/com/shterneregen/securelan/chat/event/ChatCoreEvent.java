package com.shterneregen.securelan.chat.event;

public sealed interface ChatCoreEvent permits
        ChatConnectedEvent,
        ChatDisconnectedEvent,
        ChatMessageReceivedEvent,
        ChatMessageSentEvent,
        ChatUserJoinedEvent,
        ChatUserLeftEvent,
        ChatErrorEvent {
}
