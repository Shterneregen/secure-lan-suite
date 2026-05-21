package com.shterneregen.securelan.chat.service.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.random.RandomGenerator

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

    private class FixedBoundRandomGenerator(private val index: Int) : RandomGenerator {
        override fun nextLong(): Long = index.toLong()

        override fun nextInt(bound: Int): Int = index
    }
}
