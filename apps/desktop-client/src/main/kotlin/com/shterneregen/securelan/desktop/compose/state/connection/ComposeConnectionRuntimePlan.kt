package com.shterneregen.securelan.desktop.compose.state.connection

import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers

data class ComposeConnectionRuntimePlan(
    val chatServerConfig: ChatServerConfig?,
    val localHostConnectRequest: ChatClientConnectRequest?,
    val manualConnectRequest: ChatClientConnectRequest?,
    val hostingDiscoveryConfig: PeerDiscoveryConfig?,
    val listenOnlyDiscoveryConfig: PeerDiscoveryConfig,
    val localFileListenerPort: Int?,
    val disabledReasons: List<String>,
) {
    val hostingReady: Boolean =
        chatServerConfig != null && localHostConnectRequest != null && hostingDiscoveryConfig != null
    val manualConnectionReady: Boolean = manualConnectRequest != null && localFileListenerPort != null
    val discoveryAnnouncement: String = hostingDiscoveryConfig?.let(DesktopMainViewHelpers::discoveryStartedMessage)
        ?: DesktopMainViewHelpers.discoveryListeningMessage(listenOnlyDiscoveryConfig.discoveryPort)
    val hostingSummary: String = if (hostingReady) {
        "Host chat on ${chatServerConfig?.port}, files on ${hostingDiscoveryConfig?.filePort} as ${hostingDiscoveryConfig?.nickname}"
    } else {
        "Hosting plan unavailable until room inputs are valid and hosting is stopped."
    }
    val manualConnectionSummary: String = if (manualConnectionReady) {
        "Join ${manualConnectRequest?.nickname} at ${manualConnectRequest?.host}:${manualConnectRequest?.port}; local files on $localFileListenerPort"
    } else {
        "Manual connection plan unavailable until address, ports, and connection state are valid."
    }
    val diagnosticsSummary: String = listOf(
        hostingSummary,
        manualConnectionSummary,
        discoveryAnnouncement,
    ).joinToString(" · ")

    companion object {
        private const val PREVIEW_PEER_ID: String = "compose-preview-peer"

        fun from(state: ComposeStatusConnectionState): ComposeConnectionRuntimePlan {
            val trimmedNickname = state.nickname.trim()
            val roomPassword = state.roomPasswordPlaceholder
            val serverChatPort = state.serverChatPort
            val serverFilePort = state.serverFilePort
            val clientChatPort = state.clientChatPort
            val clientFilePort = state.clientFilePort
            val localFilePort = state.resolvedLocalFilePort
            val canBuildHostingPlan = state.canOpenRoom && serverChatPort != null && serverFilePort != null
            val canBuildManualPlan =
                state.canConnect && clientChatPort != null && clientFilePort != null && localFilePort != null
            val listenOnlyDiscoveryConfig =
                PeerDiscoveryConfig.listenOnly(PREVIEW_PEER_ID, trimmedNickname.ifBlank { "Compose Preview" })
            val hostingDiscoveryConfig = if (canBuildHostingPlan) {
                PeerDiscoveryConfig.defaults(
                    PREVIEW_PEER_ID,
                    trimmedNickname,
                    serverChatPort,
                    serverFilePort,
                    state.discoverable,
                )
            } else {
                null
            }

            return ComposeConnectionRuntimePlan(
                chatServerConfig = if (canBuildHostingPlan) ChatServerConfig(serverChatPort, roomPassword) else null,
                localHostConnectRequest = if (canBuildHostingPlan) {
                    ChatClientConnectRequest("127.0.0.1", serverChatPort, trimmedNickname, roomPassword, PeerCapabilities.desktop("0.5.0", serverFilePort))
                } else {
                    null
                },
                manualConnectRequest = if (canBuildManualPlan) {
                    ChatClientConnectRequest(state.manualHost.trim(), clientChatPort, trimmedNickname, roomPassword, PeerCapabilities.desktop("0.5.0", localFilePort))
                } else {
                    null
                },
                hostingDiscoveryConfig = hostingDiscoveryConfig,
                listenOnlyDiscoveryConfig = listenOnlyDiscoveryConfig,
                localFileListenerPort = localFilePort,
                disabledReasons = disabledReasonsFor(state, canBuildHostingPlan, canBuildManualPlan),
            )
        }

        private fun disabledReasonsFor(
            state: ComposeStatusConnectionState,
            canBuildHostingPlan: Boolean,
            canBuildManualPlan: Boolean,
        ): List<String> = buildList {
            if (!canBuildHostingPlan) {
                add("Can't open room: ${state.validationSummary}")
            }
            if (!canBuildManualPlan) {
                add("Can't join room: ${state.validationSummary}")
            }
            if (state.localServerRunning) {
                add("Open room is blocked while a room is already hosted.")
            }
            if (state.clientConnected) {
                add("Join room is blocked while already connected.")
            }
        }
    }
}
