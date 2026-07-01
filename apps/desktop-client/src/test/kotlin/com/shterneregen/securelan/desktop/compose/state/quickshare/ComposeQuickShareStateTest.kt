package com.shterneregen.securelan.desktop.compose.state.quickshare

import com.shterneregen.securelan.common.net.NetworkConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeQuickShareStateTest {
    @Test
    fun shouldUseDefaultPortUntilCustomPortIsEnabled() {
        val state = ComposeQuickShareState(portText = "invalid", useCustomPort = false)

        assertEquals(NetworkConstants.DEFAULT_QUICK_SHARE_PORT, state.port)
        assertNull(state.portValidationMessage)
        assertTrue(state.canStartServer)
    }

    @Test
    fun shouldSupportUntilStoppedAndUnlimitedPolicies() {
        val state = ComposeQuickShareState(
            selectedFilePath = "demo.txt",
            noExpiration = true,
            unlimitedAccess = true,
        )

        assertTrue(state.canCreateFileShare)
        assertNull(state.effectiveExpirationMinutes)
        assertNull(state.effectiveAccessLimit)
        assertEquals("Link remains available until stopped, with unlimited downloads.", state.policySentence)
    }

    @Test
    fun shouldReportFieldSpecificPolicyErrors() {
        val state = ComposeQuickShareState(
            expirationMinutesText = "0",
            accessLimitText = "not-a-number",
            portText = "70000",
            useCustomPort = true,
        )

        assertFalse(state.canStartServer)
        assertEquals("Enter a port from 1 to 65535.", state.portValidationMessage)
        assertEquals("Enter at least 1 minute.", state.expirationValidationMessage)
        assertEquals("Enter at least 1 download.", state.accessValidationMessage)
    }
}
