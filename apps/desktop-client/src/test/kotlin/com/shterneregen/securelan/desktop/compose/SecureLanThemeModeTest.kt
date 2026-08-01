package com.shterneregen.securelan.desktop.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SecureLanThemeModeTest {
    @Test
    fun shouldCycleThroughIntermediateThemeBetweenDarkAndLight() {
        assertEquals(SecureLanThemeMode.INTERMEDIATE, SecureLanThemeMode.DARK.next())
        assertEquals(SecureLanThemeMode.LIGHT, SecureLanThemeMode.INTERMEDIATE.next())
        assertEquals(SecureLanThemeMode.DARK, SecureLanThemeMode.LIGHT.next())
    }

    @Test
    fun shouldProvideDedicatedTokensForIntermediateTheme() {
        val intermediate = SecureLanThemeTokens.forMode(SecureLanThemeMode.INTERMEDIATE)

        assertEquals(SecureLanThemeTokens.Intermediate, intermediate)
        assertNotEquals(SecureLanThemeTokens.Dark.colors.background, intermediate.colors.background)
        assertNotEquals(SecureLanThemeTokens.Light.colors.background, intermediate.colors.background)
    }
}
