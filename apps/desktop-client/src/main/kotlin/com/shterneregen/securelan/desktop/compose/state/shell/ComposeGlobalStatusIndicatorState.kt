package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState

data class ComposeGlobalStatusIndicatorState(
    val statusState: ComposeStatusConnectionState,
    val peerStatus: String = "Peer not selected",
    val voiceStatus: String = "Voice idle",
    val transferStatus: String = "Transfers idle",
) {
    val label: String = when {
        statusState.serverStatus.contains("error", ignoreCase = true) ||
            statusState.serverStatus.contains("failed", ignoreCase = true) ||
            statusState.connectionStatus.contains("error", ignoreCase = true) ||
            statusState.connectionStatus.contains("failed", ignoreCase = true) -> "Connection issue"
        voiceStatus.contains("call", ignoreCase = true) && !voiceStatus.contains("idle", ignoreCase = true) -> "In call"
        transferStatus.contains("active", ignoreCase = true) || transferStatus.contains("progress", ignoreCase = true) -> "File transfer active"
        statusState.clientConnected && statusState.localServerRunning && peerStatus == "Peer not selected" -> "Waiting for peers"
        statusState.clientConnected -> "Connected to secure room"
        statusState.localServerRunning -> "Hosting secure room"
        else -> "Offline"
    }
    val detailText: String = listOf(statusState.serverStatus, statusState.connectionStatus, peerStatus, voiceStatus, transferStatus)
        .joinToString(" · ")
}
