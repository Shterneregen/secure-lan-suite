package com.shterneregen.securelan.androidclient.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class WireMessageTest {
    @Test
    fun serializeAndDeserializeEscapesSpecialCharactersLikeDesktopProtocol() {
        val message = WireMessage(WireMessageType.CHAT, "Alice|Bob", "line 1\nline 2 \\ ok")

        val decoded = WireMessage.deserialize(message.serialize())

        assertEquals(message, decoded)
    }
}
