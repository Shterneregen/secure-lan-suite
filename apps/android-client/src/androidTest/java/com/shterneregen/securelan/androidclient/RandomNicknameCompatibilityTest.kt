package com.shterneregen.securelan.androidclient

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shterneregen.securelan.chat.service.impl.DefaultRandomNicknameService
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RandomNicknameCompatibilityTest {
    @Test
    fun defaultNicknameGeneratorWorksOnDevice() {
        // Run on API 26/29 as well as current Android: desktop JVM tests cannot
        // detect references to Java APIs missing from older Android runtimes.
        val nickname = DefaultRandomNicknameService().generate()

        assertTrue(nickname.isNotBlank())
    }
}
