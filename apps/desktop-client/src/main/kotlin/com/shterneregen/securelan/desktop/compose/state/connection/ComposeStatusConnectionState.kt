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
) {
    fun withClientDisconnected(): ComposeStatusConnectionState = copy(
        clientConnected = false,
        connectionStatus = "Connection idle",
    )

    val serverChatPort: Int? = parsePort(serverChatPortText)
    val serverFilePort: Int? = parsePort(serverFilePortText)
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
    val discoverableLabel: String = if (discoverable) "Discoverable" else "Hidden"

    private companion object {
        const val CLIENT_FILE_PORT_OFFSET: Int = 1000

        fun parsePort(value: String): Int? = value.trim().toIntOrNull()?.takeIf { it in 1..65_535 }
    }
}
