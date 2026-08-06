package com.shterneregen.securelan.desktop.compose.tray

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState

data class DesktopTrayMenuState(
    val windowVisible: Boolean,
    val localServerRunning: Boolean,
    val clientConnected: Boolean,
    val unreadMessageCount: Int = 0,
    val messageNotificationsEnabled: Boolean = true,
    val keepRunningAfterClose: Boolean = true,
) {
    val connectionLabel: String
        get() = when {
            localServerRunning && clientConnected -> "Hosting and connected"
            localServerRunning -> "Hosting a room"
            clientConnected -> "Connected to a room"
            else -> "Idle"
        }

    val openWindowLabel: String
        get() = when {
            unreadMessageCount == 1 -> "Open SecureLanSuite (1 unread)"
            unreadMessageCount > 1 -> "Open SecureLanSuite ($unreadMessageCount unread)"
            windowVisible -> "Bring SecureLanSuite to front"
            else -> "Open SecureLanSuite"
        }

    val tooltip: String
        get() = if (unreadMessageCount > 0) {
            "SecureLanSuite - $unreadMessageCount unread"
        } else {
            "SecureLanSuite - $connectionLabel"
        }
}

/** Small lifecycle-oriented menu. Feature actions can be added without coupling them to Window. */
@Composable
fun ApplicationScope.SecureLanTray(
    icon: Painter,
    trayState: TrayState,
    menuState: DesktopTrayMenuState,
    onShowWindow: () -> Unit,
    onHideWindow: () -> Unit,
    onMessageNotificationsChange: (Boolean) -> Unit,
    onKeepRunningAfterCloseChange: (Boolean) -> Unit,
    onExitApplication: () -> Unit,
) {
    Tray(
        icon = icon,
        state = trayState,
        tooltip = menuState.tooltip,
        onAction = onShowWindow,
    ) {
        Item("Connection: ${menuState.connectionLabel}", enabled = false, onClick = {})
        if (menuState.unreadMessageCount > 0) {
            Item(
                if (menuState.unreadMessageCount == 1) "1 unread message" else "${menuState.unreadMessageCount} unread messages",
                onClick = onShowWindow,
            )
        }
        Separator()
        Item(menuState.openWindowLabel, onClick = onShowWindow)
        Item("Hide main window", enabled = menuState.windowVisible, onClick = onHideWindow)
        Separator()
        CheckboxItem(
            text = "Notify about chat messages",
            checked = menuState.messageNotificationsEnabled,
            onCheckedChange = onMessageNotificationsChange,
        )
        CheckboxItem(
            text = "Keep running after window closes",
            checked = menuState.keepRunningAfterClose,
            onCheckedChange = onKeepRunningAfterCloseChange,
        )
        Separator()
        Item("Exit SecureLanSuite", onClick = onExitApplication)
    }
}
