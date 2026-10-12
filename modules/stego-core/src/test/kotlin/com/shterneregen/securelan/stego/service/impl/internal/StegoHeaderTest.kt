package com.shterneregen.securelan.stego.service.impl.internal

import com.shterneregen.securelan.stego.model.StegoContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StegoHeaderTest {
    @Test
    fun shouldSerializeAndDeserializeCorrectly() {
        val original = StegoHeader(StegoContentType.UTF8_TEXT, true, 42)
        val bytes = original.write()
        val parsed = StegoHeader.read(bytes)

        assertEquals(original, parsed)
        assertEquals(StegoContentType.UTF8_TEXT, parsed.contentType)
        assertTrue(parsed.encrypted)
        assertEquals(42, parsed.payloadLength)
    }

    @Test
    fun shouldSupportUnencryptedBinaryHeader() {
        val original = StegoHeader(StegoContentType.BINARY, false, 1024)
        val bytes = original.write()
        val parsed = StegoHeader.read(bytes)

        assertEquals(original, parsed)
        assertEquals(StegoContentType.BINARY, parsed.contentType)
        assertFalse(parsed.encrypted)
        assertEquals(1024, parsed.payloadLength)
    }

    @Test
    fun shouldRejectInvalidHeaderLength() {
        assertThrows(IllegalArgumentException::class.java) {
            StegoHeader.read(ByteArray(11))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StegoHeader.read(ByteArray(13))
        }
    }

    @Test
    fun shouldRejectInvalidMagic() {
        val bytes = StegoHeader(StegoContentType.BINARY, false, 0).write()
        bytes[0] = 0 // corrupt magic
        assertThrows(IllegalArgumentException::class.java) {
            StegoHeader.read(bytes)
        }
    }

    @Test
    fun shouldRejectUnsupportedVersion() {
        val bytes = StegoHeader(StegoContentType.BINARY, false, 0).write()
        bytes[4] = 2 // version 2
        assertThrows(IllegalArgumentException::class.java) {
            StegoHeader.read(bytes)
        }
    }

    @Test
    fun shouldRejectNegativePayloadLength() {
        assertThrows(IllegalArgumentException::class.java) {
            StegoHeader(StegoContentType.BINARY, false, -1)
        }
    }
}
