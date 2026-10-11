package com.shterneregen.securelan.chat.service.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Random

class DefaultRandomNicknameServiceTest {
    @Test
    fun shouldUseInjectedRandomGenerator() {
        val service = DefaultRandomNicknameService(FixedBoundRandomGenerator(0))

        assertEquals("Alice", service.generate())
    }

    @Test
    fun shouldPreserveFullNicknameList() {
        val service = DefaultRandomNicknameService(FixedBoundRandomGenerator(31))

        assertEquals("Atlas", service.generate())
    }

    @Test
    fun shouldRejectNullRandomGeneratorWithStableMessage() {
        val exception = assertThrows(NullPointerException::class.java) {
            DefaultRandomNicknameService(null)
        }

        assertEquals("randomGenerator must not be null", exception.message)
    }

    private class FixedBoundRandomGenerator(private val index: Int) : Random(0L) {
        override fun nextInt(bound: Int): Int {
            assertEquals(32, bound)
            return index
        }
    }
}
