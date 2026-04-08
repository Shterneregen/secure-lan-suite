package com.shterneregen.securelan.common.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonModelSmokeTest {
    @Test
    void shouldCreateChatMessage() {
        ChatMessage message = new ChatMessage("msg-1", "alice", "bob", "hello", Instant.now());
        assertEquals("hello", message.content());
    }
}
