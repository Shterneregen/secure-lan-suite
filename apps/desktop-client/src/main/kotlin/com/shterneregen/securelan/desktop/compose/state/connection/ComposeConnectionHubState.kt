package com.shterneregen.securelan.desktop.compose.state.connection

/**
 * Unified connection hub state that merges the legacy "My profile" and "Manual connection" cards
 * into a single room-connection surface. Nickname and password are shared across host/join modes;
 * mode-specific ports and addresses are kept inside [statusState].
 */
data class ComposeConnectionHubState(
    val statusState: ComposeStatusConnectionState,
    val mode: ComposeConnectionHubMode = ComposeConnectionHubMode.HOST,
    val localNetworkInfo: String = "",
) {
    val title: String = when (mode) {
        ComposeConnectionHubMode.HOST -> "Host a secure room"
        ComposeConnectionHubMode.JOIN -> "Join a secure room"
    }
    val nickname: String = statusState.nickname
    val password: String = statusState.roomPasswordPlaceholder
    val credentialFieldsEnabled: Boolean =
        !statusState.localServerRunning && !statusState.clientConnected

    val primaryActionEnabled: Boolean = when (mode) {
        ComposeConnectionHubMode.HOST -> statusState.canOpenRoom
        ComposeConnectionHubMode.JOIN -> statusState.canConnect
    }

    val primaryActionLabel: String = when (mode) {
        ComposeConnectionHubMode.HOST -> "Start secure room"
        ComposeConnectionHubMode.JOIN -> "Join Room"
    }

    val secondaryActionEnabled: Boolean = when (mode) {
        ComposeConnectionHubMode.HOST -> statusState.localServerRunning
        ComposeConnectionHubMode.JOIN -> statusState.clientConnected
    }

    val secondaryActionLabel: String = when (mode) {
        ComposeConnectionHubMode.HOST -> "Stop hosting"
        ComposeConnectionHubMode.JOIN -> "Disconnect"
    }

    val blockedReason: String? = when {
        !statusState.nicknameValid -> "Enter your name before opening or joining a room."
        mode == ComposeConnectionHubMode.HOST && statusState.serverChatPort == null -> "Room chat port must be a number from 1 to 65535."
        mode == ComposeConnectionHubMode.HOST && statusState.serverFilePort == null -> "Room file port must be a number from 1 to 65535."
        mode == ComposeConnectionHubMode.JOIN && !statusState.manualHostValid -> "Enter the address of the room you want to join."
        mode == ComposeConnectionHubMode.JOIN && statusState.clientChatPort == null -> "Chat port must be a number from 1 to 65535."
        mode == ComposeConnectionHubMode.JOIN && statusState.clientFilePort == null -> "File port must be a number from 1 to 65535."
        else -> null
    }

    val advancedSettingsTitle: String = "Advanced settings"
}
