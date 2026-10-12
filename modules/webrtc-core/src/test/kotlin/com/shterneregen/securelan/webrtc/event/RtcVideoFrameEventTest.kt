package com.shterneregen.securelan.webrtc.event

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RtcVideoFrameEventTest {
    @Test
    fun shouldCreateValidVideoFrameEventAndExposeDefensiveCopyOfPixels() {
        val pixels = byteArrayOf(1, 2, 3, 4)
        val event = RtcVideoFrameEvent("session-1", "alice", true, 1, 1, 90, pixels)

        assertEquals("session-1", event.sessionId())
        assertEquals("alice", event.peer())
        assertTrue(event.local())
        assertEquals(1, event.width())
        assertEquals(1, event.height())
        assertEquals(90, event.rotation())
        assertArrayEquals(pixels, event.bgraPixels())

        // Modifying the returned array copy must not affect the event
        val copy = event.bgraPixels()
        copy[0] = 99
        assertEquals(1.toByte(), event.bgraPixels()[0])
    }

    @Test
    fun shouldEnforceContentEqualityAndMatchingHashCodeForIdenticalByteBuffers() {
        val pixels1 = byteArrayOf(10, 20, 30, 40)
        val pixels2 = byteArrayOf(10, 20, 30, 40)
        val event1 = RtcVideoFrameEvent("s1", "bob", false, 1, 1, 0, pixels1)
        val event2 = RtcVideoFrameEvent("s1", "bob", false, 1, 1, 0, pixels2)

        assertEquals(event1, event2)
        assertEquals(event1.hashCode(), event2.hashCode())

        val differentPixels = byteArrayOf(10, 20, 30, 99)
        val eventDifferent = RtcVideoFrameEvent("s1", "bob", false, 1, 1, 0, differentPixels)
        assertNotEquals(event1, eventDifferent)
    }

    @Test
    fun shouldRejectInvalidDimensionsOrMismatchedBufferLengths() {
        assertThrows(IllegalArgumentException::class.java) {
            RtcVideoFrameEvent("s", "p", true, 0, 1, 0, byteArrayOf(1, 2, 3, 4))
        }

        assertThrows(IllegalArgumentException::class.java) {
            // 2x2 frame requires 16 bytes
            RtcVideoFrameEvent("s", "p", true, 2, 2, 0, byteArrayOf(1, 2, 3, 4))
        }
    }

    @Test
    fun shouldFormatToStringWithPixelByteCountInsteadOfArrayReference() {
        val event = RtcVideoFrameEvent("session-1", "alice", true, 1, 1, 0, byteArrayOf(1, 2, 3, 4))
        assertTrue(event.toString().contains("bgraPixels=4B"))
    }
}
