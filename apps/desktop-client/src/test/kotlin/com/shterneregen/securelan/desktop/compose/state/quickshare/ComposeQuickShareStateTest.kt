package com.shterneregen.securelan.desktop.compose.state.quickshare

import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.ui.QuickShareEntry
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeQuickShareStateTest {
    @Test
    fun shouldUseShellHostForIndexAndFileAndTextRowsWithoutDiscardingAlternatives() {
        val landingUrls = listOf("http://192.168.56.1:5053/", "http://10.0.0.42:5053/")
        val entries = QuickShareType.entries.map { type ->
            QuickShareEntry(QuickShareSnapshot(
                type.name, type, "demo", "demo.txt", 42, Instant.EPOCH,
                null, null, 0, QuickShareStatus.ACTIVE, landingUrls.map { "${it}s/demo" },
            ))
        }
        val state = ComposeQuickShareState(entries = entries, landingUrls = landingUrls, preferredHost = "10.0.0.42")

        assertEquals(landingUrls[1], state.primaryLandingUrl)
        assertEquals(landingUrls, state.landingUrls)
        state.shareRowsDetailed.forEach { row ->
            assertEquals("${landingUrls[1]}s/demo", row.url)
            assertTrue(row.canShowQrCode)
            assertFalse(row.copy(url = "").canShowQrCode)
        }
        QuickShareStatus.entries.filter { it != QuickShareStatus.ACTIVE }.forEach { status ->
            entries.forEach { entry ->
                val snapshot = entry.snapshot()
                val inactive = QuickShareEntry(QuickShareSnapshot(
                    snapshot.id(), snapshot.type(), "demo", "demo.txt", 42, Instant.EPOCH,
                    null, null, 0, status, snapshot.urls(),
                ))
                assertFalse(ComposeQuickShareRow.from(inactive, "10.0.0.42").canShowQrCode)
            }
        }
        assertEquals("${landingUrls[0]}s/demo", state.copy(preferredHost = "10.0.0.99").shareRowsDetailed.first().url)
    }

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
