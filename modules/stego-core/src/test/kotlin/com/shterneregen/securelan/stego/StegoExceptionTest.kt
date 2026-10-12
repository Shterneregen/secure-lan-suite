package com.shterneregen.securelan.stego

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class StegoExceptionTest {
    @Test
    fun shouldSupportMessageOnlyConstructor() {
        val ex = StegoException("stego error")
        assertEquals("stego error", ex.message)
    }

    @Test
    fun shouldSupportMessageAndCauseConstructor() {
        val cause = IllegalArgumentException("invalid header")
        val ex = StegoException("stego error", cause)
        assertEquals("stego error", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun shouldSupportCauseOnlyConstructor() {
        val cause = IllegalArgumentException("invalid header")
        val ex = StegoException(cause)
        assertSame(cause, ex.cause)
    }
}
