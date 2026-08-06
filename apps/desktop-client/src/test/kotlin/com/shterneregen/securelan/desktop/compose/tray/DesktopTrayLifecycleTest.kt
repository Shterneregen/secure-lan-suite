package com.shterneregen.securelan.desktop.compose.tray

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DesktopTrayLifecycleTest {
    @Test
    fun shouldHideWindowWhenConfiguredAndTrayIsSupported() {
        val outcome = desktopCloseOutcome(keepRunningAfterWindowClose = true, traySupported = true)

        assertEquals(DesktopCloseOutcome.HIDE_WINDOW, outcome)
    }

    @Test
    fun shouldExitWhenTrayIsUnavailableInsteadOfLeavingInvisibleProcess() {
        val outcome = desktopCloseOutcome(keepRunningAfterWindowClose = true, traySupported = false)

        assertEquals(DesktopCloseOutcome.EXIT_APPLICATION, outcome)
    }

    @Test
    fun shouldExitWhenExplicitlyConfigured() {
        val outcome = desktopCloseOutcome(keepRunningAfterWindowClose = false, traySupported = true)

        assertEquals(DesktopCloseOutcome.EXIT_APPLICATION, outcome)
    }

    @Test
    fun shouldDescribeCombinedTrayStatus() {
        assertEquals(
            "Hosting and connected",
            DesktopTrayMenuState(
                windowVisible = false,
                localServerRunning = true,
                clientConnected = true,
            ).connectionLabel,
        )
    }

    @Test
    fun shouldShowUnreadCountInOpenActionAndTooltip() {
        val state = DesktopTrayMenuState(
            windowVisible = false,
            localServerRunning = false,
            clientConnected = true,
            unreadMessageCount = 3,
        )

        assertEquals("Open SecureLanSuite (3 unread)", state.openWindowLabel)
        assertEquals("SecureLanSuite - 3 unread", state.tooltip)
    }

    @Test
    fun shouldBuildCompactMessagePreview() {
        val notification = DesktopChatTrayNotification.create(
            id = 7,
            senderNickname = "  Alice   Smith ",
            text = " First line\n second line ",
        )

        assertEquals("Message from Alice Smith", notification.title)
        assertEquals("First line second line", notification.message)
    }

    @Test
    fun shouldNotifyOnlyForRemoteUserMessagesWhenEnabled() {
        assertEquals(
            true,
            shouldPublishChatTrayNotification(
                systemLikeMessage = false,
                localSender = false,
                notificationsEnabled = true,
                messageNotificationsEnabled = true,
            ),
        )
        assertEquals(
            false,
            shouldPublishChatTrayNotification(
                systemLikeMessage = true,
                localSender = false,
                notificationsEnabled = true,
                messageNotificationsEnabled = true,
            ),
        )
        assertEquals(
            false,
            shouldPublishChatTrayNotification(
                systemLikeMessage = false,
                localSender = true,
                notificationsEnabled = true,
                messageNotificationsEnabled = true,
            ),
        )
    }
}
