package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TransportExceptionTest {
    @Test
    fun shouldSupportMessageOnlyConstructor() {
        val ex = TransportException("network error")
        assertEquals("network error", ex.message)
    }

    @Test
    fun shouldSupportMessageAndCauseConstructor() {
        val cause = IllegalStateException("underlying cause")
        val ex = TransportException("network error", cause)
        assertEquals("network error", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun shouldSupportCauseOnlyConstructor() {
        val cause = IllegalStateException("underlying cause")
        val ex = TransportException(cause)
        assertSame(cause, ex.cause)
    }
}
