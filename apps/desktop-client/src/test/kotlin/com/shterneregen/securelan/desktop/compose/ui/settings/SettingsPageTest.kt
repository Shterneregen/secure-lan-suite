package com.shterneregen.securelan.desktop.compose.ui.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SettingsPageTest {
    @Test
    fun shouldExposeDedicatedSettingsDestinations() {
        assertEquals(
            listOf("General", "Audio & video", "Notifications", "Files & transfers", "Network"),
            SettingsPage.entries.map(SettingsPage::title),
        )
        assertFalse(SettingsPage.entries.any { it.title.contains("All settings", ignoreCase = true) })
    }
}
