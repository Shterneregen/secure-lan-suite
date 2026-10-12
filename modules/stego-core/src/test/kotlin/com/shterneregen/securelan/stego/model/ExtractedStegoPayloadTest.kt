package com.shterneregen.securelan.stego.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class ExtractedStegoPayloadTest {
    @Test
    fun shouldPreserveImmutabilityViaDefensiveCopies() {
        val originalBytes = "hello".toByteArray(StandardCharsets.UTF_8)
        val payload = ExtractedStegoPayload(StegoContentType.UTF8_TEXT, false, originalBytes)

        originalBytes[0] = 'X'.code.toByte()
        assertEquals("hello", payload.asUtf8String())

        val retrievedBytes = payload.payload()
        retrievedBytes[0] = 'Y'.code.toByte()
        assertEquals("hello", payload.asUtf8String())
    }

    @Test
    fun shouldSupportEqualityAndHashCodeContract() {
        val bytes1 = byteArrayOf(1, 2, 3)
        val bytes2 = byteArrayOf(1, 2, 3)
        val payload1 = ExtractedStegoPayload(StegoContentType.BINARY, true, bytes1)
        val payload2 = ExtractedStegoPayload(StegoContentType.BINARY, true, bytes2)
        val payloadDifferent = ExtractedStegoPayload(StegoContentType.BINARY, false, bytes1)

        assertEquals(payload1, payload2)
        assertEquals(payload1.hashCode(), payload2.hashCode())
        assertNotEquals(payload1, payloadDifferent)
    }

    @Test
    fun shouldDecodeUtf8TextOnlyWhenContentTypeMatches() {
        val textBytes = "test message".toByteArray(StandardCharsets.UTF_8)
        val textPayload = ExtractedStegoPayload(StegoContentType.UTF8_TEXT, false, textBytes)
        val binaryPayload = ExtractedStegoPayload(StegoContentType.BINARY, false, textBytes)

        assertEquals("test message", textPayload.asUtf8String())
        assertThrows(IllegalStateException::class.java) { binaryPayload.asUtf8String() }
    }

    @Test
    fun shouldProduceInformativeToString() {
        val payload = ExtractedStegoPayload(StegoContentType.UTF8_TEXT, true, byteArrayOf(1, 2, 3))
        val str = payload.toString()

        assertTrue(str.contains("UTF8_TEXT"))
        assertTrue(str.contains("encrypted=true"))
        assertTrue(str.contains("3 bytes"))
    }
}
