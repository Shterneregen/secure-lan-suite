package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionActionState(
    val openRoomReady: Boolean,
    val stopHostingReady: Boolean,
    val connectReady: Boolean,
    val disconnectReady: Boolean,
    val discoverabilityToggleReady: Boolean,
) {
    val openRoomLabel: String = if (openRoomReady) "Open room ready" else "Open room blocked"
    val stopHostingLabel: String = if (stopHostingReady) "Stop hosting ready" else "Stop hosting blocked"
    val connectLabel: String = if (connectReady) "Connect ready" else "Connect blocked"
    val disconnectLabel: String = if (disconnectReady) "Disconnect ready" else "Disconnect blocked"
    val discoverabilityToggleLabel: String =
        if (discoverabilityToggleReady) "Room visibility ready" else "Room visibility can change only while hosting"
    val diagnosticSummary: String = listOf(
        openRoomLabel,
        stopHostingLabel,
        connectLabel,
        disconnectLabel,
        discoverabilityToggleLabel,
    ).joinToString(" · ")

    companion object {
        fun from(state: ComposeStatusConnectionState): ComposeConnectionActionState = ComposeConnectionActionState(
            openRoomReady = state.canOpenRoom,
            stopHostingReady = state.localServerRunning,
            connectReady = state.canConnect,
            disconnectReady = state.clientConnected,
            discoverabilityToggleReady = state.localServerRunning,
        )
    }
}
