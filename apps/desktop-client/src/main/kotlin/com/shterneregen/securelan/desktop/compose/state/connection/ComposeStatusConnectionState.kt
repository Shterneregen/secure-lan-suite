package com.shterneregen.securelan.desktop.compose.state.connection

import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers

data class ComposeStatusConnectionState(
    val nickname: String = "Compose Preview",
    val roomPasswordPlaceholder: String = "chatpass",
    val manualHost: String = "127.0.0.1",
    val serverChatPortText: String = NetworkConstants.DEFAULT_CHAT_PORT.toString(),
    val serverFilePortText: String = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT.toString(),
    val clientChatPortText: String = NetworkConstants.DEFAULT_CHAT_PORT.toString(),
    val clientFilePortText: String = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT.toString(),
    val discoverable: Boolean = true,
    val serverStatus: String = "Room closed",
    val connectionStatus: String = "Connection idle",
    val discoveryStatus: String = "Room discovery not started",
    val localServerRunning: Boolean = false,
    val clientConnected: Boolean = false,
    val javaFxFallbackAvailable: Boolean = true,
) {
    val serverChatPort: Int? = parsePort(serverChatPortText)
    val serverFilePort: Int? = parsePort(serverFilePortText)
    val chatPort: Int = serverChatPort ?: NetworkConstants.DEFAULT_CHAT_PORT
    val filePort: Int = serverFilePort ?: NetworkConstants.DEFAULT_FILE_TRANSFER_PORT
    val clientChatPort: Int? = parsePort(clientChatPortText)
    val clientFilePort: Int? = parsePort(clientFilePortText)
    val nicknameValid: Boolean = nickname.trim().isNotEmpty()
    val manualHostValid: Boolean = manualHost.trim().isNotEmpty()
    val canOpenRoom: Boolean = !localServerRunning && nicknameValid && serverChatPort != null && serverFilePort != null
    val canConnect: Boolean =
        !clientConnected && nicknameValid && manualHostValid && clientChatPort != null && clientFilePort != null
    val resolvedLocalFilePort: Int? = if (serverFilePort != null && clientFilePort != null) {
        DesktopMainViewHelpers.resolveLocalFilePort(
            serverRunning = localServerRunning,
            serverFilePortText = serverFilePortText.trim(),
            clientFilePortText = clientFilePortText.trim(),
            defaultFileTransferPort = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
            clientFilePortOffset = CLIENT_FILE_PORT_OFFSET,
        )
    } else {
        null
    }
    val portSummary: String =
        "Room chat ${serverChatPortText.trim()} · Room files ${serverFilePortText.trim()} · Join chat ${clientChatPortText.trim()} · Join files ${clientFilePortText.trim()}"
    val validationSummary: String = when {
        !nicknameValid -> "Enter a name before opening or joining a room."
        serverChatPort == null || serverFilePort == null -> "Room chat and file ports must be numbers from 1 to 65535."
        !manualHostValid -> "Enter a room address before connecting manually."
        clientChatPort == null || clientFilePort == null -> "Connection ports must be numbers from 1 to 65535."
        localServerRunning && clientConnected -> "Room is open and this client is already connected."
        localServerRunning -> "Room is open; stop hosting before opening another room."
        clientConnected -> "Already connected to a room; disconnect first."
        else -> "Ready to open or join a room."
    }
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX fallback available" else "JavaFX fallback unavailable"
    val discoverableLabel: String = if (discoverable) "Discoverable" else "Hidden"
    val actionState: ComposeConnectionActionState = ComposeConnectionActionState.from(this)
    val runtimePlan: ComposeConnectionRuntimePlan = ComposeConnectionRuntimePlan.from(this)
    val eventPreview: ComposeConnectionEventPreview = ComposeConnectionEventPreview.from(this)
    val controlPlan: ComposeConnectionControlPlan = ComposeConnectionControlPlan.from(this)
    val lifecyclePlan: ComposeConnectionLifecyclePlan = ComposeConnectionLifecyclePlan.from(this, runtimePlan)
    val transitionPlan: ComposeConnectionTransitionPlan =
        ComposeConnectionTransitionPlan.from(this, lifecyclePlan, controlPlan)
    val adapterEventRouting: ComposeAdapterEventRouting =
        ComposeAdapterEventRouting.from(this, lifecyclePlan, runtimePlan)

    private companion object {
        const val CLIENT_FILE_PORT_OFFSET: Int = 1000

        fun parsePort(value: String): Int? = value.trim().toIntOrNull()?.takeIf { it in 1..65_535 }
    }
}
