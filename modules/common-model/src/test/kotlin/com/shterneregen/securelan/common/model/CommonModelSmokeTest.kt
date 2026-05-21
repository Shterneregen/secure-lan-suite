package com.shterneregen.securelan.common.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CommonModelSmokeTest {
    @Test
    fun shouldCreateChatMessage() {
        val message = ChatMessage("msg-1", "alice", "bob", "hello", Instant.now())

        assertEquals("hello", message.content)
    }
}
