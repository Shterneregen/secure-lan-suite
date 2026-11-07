package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState

data class ComposeAppShellState(
    val productState: ComposeProductScreenState,
    val statusState: ComposeStatusConnectionState,
    val peerStatus: String = "No one selected",
    val transferStatus: String = "Files idle",
    val workspaceState: ComposeWorkspaceState = ComposeWorkspaceState(),
) {
    val currentContextLabel: String = when (productState.appMode) {
        AppMode.WELCOME -> ComposeShellMetadata.APP_NAME
        AppMode.HOST_SETUP -> "Host a secure room"
        AppMode.JOIN_SETUP -> "Join a secure room"
        AppMode.MESSENGER -> statusState.nickname.ifBlank { "Secure room" }
        AppMode.SETTINGS -> "Settings"
    }
}
