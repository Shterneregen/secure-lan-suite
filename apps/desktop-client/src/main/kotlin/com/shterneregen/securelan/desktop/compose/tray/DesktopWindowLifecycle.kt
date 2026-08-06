package com.shterneregen.securelan.desktop.compose.tray

enum class DesktopCloseOutcome {
    EXIT_APPLICATION,
    HIDE_WINDOW,
}

/** Never hide the last window when the operating system cannot provide a way to restore it. */
fun desktopCloseOutcome(
    keepRunningAfterWindowClose: Boolean,
    traySupported: Boolean,
): DesktopCloseOutcome = if (keepRunningAfterWindowClose && traySupported) {
    DesktopCloseOutcome.HIDE_WINDOW
} else {
    DesktopCloseOutcome.EXIT_APPLICATION
}
