package com.shterneregen.securelan.webcam.service

import com.shterneregen.securelan.webcam.service.impl.DefaultVideoProfileService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultVideoProfileServiceTest {

    private val service: VideoProfileService = DefaultVideoProfileService()

    @Test
    fun `implements VideoProfileService interface`() {
        assertInstanceOf(VideoProfileService::class.java, service)
    }

    @Test
    fun `returns expected default video profile settings`() {
        val profile = service.defaultProfile()

        assertEquals(1280, profile.width)
        assertEquals(720, profile.height)
        assertEquals(30, profile.framesPerSecond)
        assertTrue(profile.screenShareReady)
    }

    @Test
    fun `returns cached constant instance from companion object`() {
        val first = service.defaultProfile()
        val second = service.defaultProfile()

        assertSame(DefaultVideoProfileService.DEFAULT_PROFILE, first)
        assertSame(first, second)
    }
}
