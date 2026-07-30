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
    val isHostMode: Boolean = mode == ComposeConnectionHubMode.HOST
    val title: String = when (mode) {
        ComposeConnectionHubMode.HOST -> "Host a secure room"
        ComposeConnectionHubMode.JOIN -> "Join a secure room"
    }
    val hostTabLabel: String = "Host secure room"
    val joinTabLabel: String = "Join nearby room"
    val roomNameLabel: String = "Room name"
    val displayNameLabel: String = "Your display name"
    val passwordLabel: String = "Room password"
    val visibilityToggleLabel: String = "Visible to nearby trusted devices"
    val hostChoiceSubtitle: String = if (statusState.localServerRunning) {
        if (statusState.discoverable) "Visible to nearby trusted devices" else "Only people with an invite can join"
    } else {
        "People nearby can join this trusted room."
    }
    val joinChoiceSubtitle: String = when {
        statusState.clientConnected -> "Joined ${statusState.manualHost.trim()}"
        statusState.manualHost.trim().isNotBlank() && statusState.manualHost.trim() != "127.0.0.1" ->
            "Ready for ${statusState.manualHost.trim()}"
        else -> "Choose a nearby room or use Advanced"
    }
    val modeSelectorTooltip: String = "Choose whether to host a room on this computer or join one nearby."
    val nickname: String = statusState.nickname
    val password: String = statusState.roomPasswordPlaceholder
    val roomName: String = "Secure LAN room"
    val credentialFieldsEnabled: Boolean =
        !statusState.localServerRunning && !statusState.clientConnected

    val primaryActionEnabled: Boolean = when (mode) {
        ComposeConnectionHubMode.HOST -> statusState.canOpenRoom
        ComposeConnectionHubMode.JOIN -> statusState.canConnect
    }

    val primaryActionLabel: String = when {
        mode == ComposeConnectionHubMode.HOST -> "Start secure room"
        mode == ComposeConnectionHubMode.JOIN -> "Join Room"
        else -> "Join Room"
    }

    val secondaryActionEnabled: Boolean = when (mode) {
        ComposeConnectionHubMode.HOST -> statusState.localServerRunning
        ComposeConnectionHubMode.JOIN -> statusState.clientConnected
    }

    val secondaryActionLabel: String = when (mode) {
        ComposeConnectionHubMode.HOST -> if (statusState.localServerRunning) "Stop hosting" else "Stop hosting"
        ComposeConnectionHubMode.JOIN -> if (statusState.clientConnected) "Disconnect" else "Disconnect"
    }

    val secondaryActionDestructive: Boolean = true

    val modeHint: String = when (mode) {
        ComposeConnectionHubMode.HOST -> "Start a room for nearby trusted people."
        ComposeConnectionHubMode.JOIN -> "Join a nearby trusted room."
    }

    val joinTargetSummary: String = when {
        mode != ComposeConnectionHubMode.JOIN -> "Nearby trusted peers can see this room when it is open and visible."
        !statusState.manualHostValid -> "Choose a nearby room, or open Advanced connection."
        statusState.clientChatPort != null && statusState.clientFilePort != null ->
            "Join ${statusState.manualHost.trim()}:${statusState.clientChatPortText.trim()} · files ${statusState.clientFilePortText.trim()}"
        else -> "Manual connection is set; enter valid chat and file ports."
    }

    val discoverableCompactLabel: String = "Visible to nearby trusted devices"
    val setupCompactSummary: String = when (mode) {
        ComposeConnectionHubMode.HOST -> "Set your name and password, then start the room."
        ComposeConnectionHubMode.JOIN -> "Set your name and password, then join a nearby or hidden room."
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

    val statusMessage: String? = blockedReason

    val statusMessageTone: ComposeConnectionHubMessageTone = when {
        blockedReason != null -> ComposeConnectionHubMessageTone.ERROR
        else -> ComposeConnectionHubMessageTone.INFO
    }

    val primaryActionBlockedReason: String = blockedReason ?: "Action is not available in this state."
    val discoverableToggleEnabled: Boolean = true
    val discoverableLabel: String = statusState.discoverableLabel
    val showAdvancedSettings: Boolean = true
    val advancedSettingsTitle: String = "Advanced settings"
}
