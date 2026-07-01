package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState

data class ComposeAppShellState(
    val productState: ComposeProductScreenState,
    val statusState: ComposeStatusConnectionState,
    val peerStatus: String = "No one selected",
    val voiceStatus: String = "Calls idle",
    val transferStatus: String = "Files idle",
    val warningVisible: Boolean = false,
    val workspaceState: ComposeWorkspaceState = ComposeWorkspaceState(),
) {
    val topBarHeightMin: Int = 48
    val topBarHeightMax: Int = 56
    val workspaceTitle: String = workspaceState.title
    val workspaceSubtitle: String = workspaceState.subtitle
    val currentContextLabel: String = when (productState.appMode) {
        AppMode.WELCOME -> ComposeShellMetadata.APP_NAME
        AppMode.HOST_SETUP -> "Host a secure room"
        AppMode.JOIN_SETUP -> "Join a secure room"
        AppMode.MESSENGER -> statusState.nickname.ifBlank { "Secure room" }
        AppMode.SETTINGS -> "Settings"
    }
    val rightActions: List<String> = listOf("Theme")
}
