package com.shterneregen.securelan.chat.service;

import com.shterneregen.securelan.chat.event.ChatCoreEvent;

@FunctionalInterface
public interface ChatEventPublisher {
    void publish(ChatCoreEvent event);
}
