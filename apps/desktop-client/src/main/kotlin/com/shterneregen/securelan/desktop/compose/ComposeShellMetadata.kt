package com.shterneregen.securelan.desktop.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.desktop.ui.DesktopPeerFormatters
import com.shterneregen.securelan.desktop.ui.DesktopQuickShareFormatters
import com.shterneregen.securelan.desktop.ui.DesktopRealtimeFormatters
import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice
import com.shterneregen.securelan.desktop.ui.PeerPresence
import com.shterneregen.securelan.desktop.ui.QuickShareEntry
import com.shterneregen.securelan.desktop.ui.TransferEntry
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import com.shterneregen.securelan.stego.model.BmpCapacity
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

object ComposeShellMetadata {
    const val WINDOW_TITLE: String = "SecureLanSuite Chat"
    const val APP_NAME: String = "SecureLanSuite"
    const val STATUS_TEXT: String = "Compose Multiplatform shell is available. JavaFX remains the production fallback."
    const val FALLBACK_TEXT: String =
        "Use the standard desktop launcher for the stable JavaFX client while Compose screens are migrated. Compose now reuses the packaged app icon resource."
    val DEFAULT_WINDOW_WIDTH: Dp = 1360.dp
    val DEFAULT_WINDOW_HEIGHT: Dp = 860.dp
    val DEFAULT_STATUS_ADAPTER_STATE: ComposeStatusConnectionState = ComposeStatusConnectionState()
    val DEFAULT_PEER_LIST_STATE: ComposePeerListState = ComposePeerListState()
    val DEFAULT_CHAT_WORKSPACE_STATE: ComposeChatWorkspaceState = ComposeChatWorkspaceState(
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
        peerListState = DEFAULT_PEER_LIST_STATE,
    )
    val DEFAULT_DIAGNOSTICS_STATE: ComposeDiagnosticsState = ComposeDiagnosticsState(
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
        peerListState = DEFAULT_PEER_LIST_STATE,
    )
    val DEFAULT_STEGO_STATE: ComposeSteganographyState = ComposeSteganographyState()
    val DEFAULT_MEDIA_VOICE_STATE: ComposeMediaVoiceState = ComposeMediaVoiceState(
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
        peerListState = DEFAULT_PEER_LIST_STATE,
    )
    val DEFAULT_VIDEO_STATE: ComposeExperimentalVideoState = ComposeExperimentalVideoState(
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
        peerListState = DEFAULT_PEER_LIST_STATE,
    )
    val DEFAULT_SELECTED_PEER_QUICK_ACTIONS_STATE: ComposeSelectedPeerQuickActionsState =
        ComposeSelectedPeerQuickActionsState(
            peerListState = DEFAULT_PEER_LIST_STATE,
            clientConnected = DEFAULT_STATUS_ADAPTER_STATE.clientConnected,
        )
    val DEFAULT_FILE_TRANSFER_STATE: ComposeFileTransferState = ComposeFileTransferState(
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
        peerListState = DEFAULT_PEER_LIST_STATE,
    )
    val DEFAULT_QUICK_SHARE_STATE: ComposeQuickShareState = ComposeQuickShareState()
    val DEFAULT_REGRESSION_STATE: ComposeRegressionReadinessState = ComposeRegressionReadinessState(
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
        peerListState = DEFAULT_PEER_LIST_STATE,
        chatState = DEFAULT_CHAT_WORKSPACE_STATE,
        fileTransferState = DEFAULT_FILE_TRANSFER_STATE,
        quickShareState = DEFAULT_QUICK_SHARE_STATE,
        steganographyState = DEFAULT_STEGO_STATE,
        mediaVoiceState = DEFAULT_MEDIA_VOICE_STATE,
        experimentalVideoState = DEFAULT_VIDEO_STATE,
    )
    val DEFAULT_PACKAGING_STATE: ComposePackagingReadinessState = ComposePackagingReadinessState()
    val DEFAULT_WORKSPACE_PARITY_STATE: ComposeJavaFxWorkspaceParityState = ComposeJavaFxWorkspaceParityState()
    val DEFAULT_ACTIONS_PRESENTATION_STATE: ComposeActionsColumnPresentationState = ComposeActionsColumnPresentationState()
}

enum class ComposeJavaFxWorkspaceRegion {
    STATUS_BAR,
    CONNECTION_HEADER,
    PEERS_COLUMN,
    CONVERSATION_COLUMN,
    ACTIONS_COLUMN,
}

data class ComposeJavaFxWorkspaceColumn(
    val region: ComposeJavaFxWorkspaceRegion,
    val title: String,
    val weight: Float,
    val javaFxSource: String,
    val composeScope: String,
) {
    val displayText: String = "$title ($weight): $composeScope"
}

data class ComposeJavaFxWorkspaceParityState(
    val statusChips: List<String> = listOf("Server", "Connection", "Peer", "Voice", "Transfers", "Theme"),
    val headerCards: List<String> = listOf("My profile", "Manual connection"),
    val workspaceColumns: List<ComposeJavaFxWorkspaceColumn> = listOf(
        ComposeJavaFxWorkspaceColumn(
            region = ComposeJavaFxWorkspaceRegion.PEERS_COLUMN,
            title = "Peers",
            weight = 0.20f,
            javaFxSource = "buildPeersColumn",
            composeScope = "LivePeerListCard or PeerListPreviewCard",
        ),
        ComposeJavaFxWorkspaceColumn(
            region = ComposeJavaFxWorkspaceRegion.CONVERSATION_COLUMN,
            title = "Chat",
            weight = 0.52f,
            javaFxSource = "buildConversationColumn",
            composeScope = "LiveChatWorkspaceCard, video stage, and chat preview surfaces",
        ),
        ComposeJavaFxWorkspaceColumn(
            region = ComposeJavaFxWorkspaceRegion.ACTIONS_COLUMN,
            title = "Actions",
            weight = 0.28f,
            javaFxSource = "buildActionsColumn",
            composeScope = "Transfers, quick share, stego, media, diagnostics, regression, and packaging cards",
        ),
    ),
    val actionSections: List<String> = listOf(
        "Selected peer",
        "Transfers",
        "LAN browser quick share",
        "Steganography",
        "Audio / Video devices",
        "Runtime / Diagnostics",
    ),
    val quickActions: List<String> = listOf("Attach", "Voice call", "Video call", "End call"),
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "JavaFX workspace parity layout"
    val dividerPositions: List<Double> = listOf(0.20, 0.72)
    val splitSummary: String = "Peers 20% · Chat 52% · Actions 28%"
    val statusSummary: String = "Status chips: ${statusChips.joinToString(" · ")}"
    val headerSummary: String = "Connection header cards: ${headerCards.joinToString(" · ")}"
    val actionSectionSummary: String = "Actions column sections: ${actionSections.joinToString(" · ")}"
    val quickActionSummary: String = "Chat quick actions: ${quickActions.joinToString(" · ")}"
    val javaFxMappingSummary: String = workspaceColumns.joinToString(" · ") { "${it.javaFxSource} → ${it.composeScope}" }
    val fallbackLabel: String = if (javaFxFallbackAvailable) {
        "JavaFX fallback remains available while Compose mirrors the workspace layout."
    } else {
        "JavaFX fallback unavailable; do not promote this Compose layout."
    }
    val parityReady: Boolean = javaFxFallbackAvailable &&
        workspaceColumns.map(ComposeJavaFxWorkspaceColumn::region) == listOf(
            ComposeJavaFxWorkspaceRegion.PEERS_COLUMN,
            ComposeJavaFxWorkspaceRegion.CONVERSATION_COLUMN,
            ComposeJavaFxWorkspaceRegion.ACTIONS_COLUMN,
        ) &&
        actionSections.containsAll(listOf("Transfers", "LAN browser quick share", "Steganography", "Audio / Video devices", "Runtime / Diagnostics"))
}

enum class ComposeActionsSectionKind {
    SELECTED_PEER,
    TRANSFERS,
    QUICK_SHARE,
    STEGANOGRAPHY,
    MEDIA_DEVICES,
    RUNTIME_DIAGNOSTICS,
}

data class ComposeActionsSectionPresentation(
    val kind: ComposeActionsSectionKind,
    val title: String,
    val javaFxSource: String,
    val composeScope: String,
    val expandedByDefault: Boolean,
) {
    val displayText: String = "$title ${if (expandedByDefault) "expanded" else "collapsed"}: $composeScope"
}

data class ComposeActionsColumnPresentationState(
    val sections: List<ComposeActionsSectionPresentation> = listOf(
        ComposeActionsSectionPresentation(
            kind = ComposeActionsSectionKind.SELECTED_PEER,
            title = "Selected peer",
            javaFxSource = "selectedPeerTitleValue / selectedPeerMetaValue",
            composeScope = "SelectedPeerQuickActions",
            expandedByDefault = true,
        ),
        ComposeActionsSectionPresentation(
            kind = ComposeActionsSectionKind.TRANSFERS,
            title = "Transfers",
            javaFxSource = "createSectionCard(\"Transfers\", transfersBlock)",
            composeScope = "LiveFileTransferCard or transfer preview copy",
            expandedByDefault = true,
        ),
        ComposeActionsSectionPresentation(
            kind = ComposeActionsSectionKind.QUICK_SHARE,
            title = "LAN browser quick share",
            javaFxSource = "TitledPane(\"LAN browser quick share\")",
            composeScope = "LiveQuickShareCard or quick-share preview copy",
            expandedByDefault = false,
        ),
        ComposeActionsSectionPresentation(
            kind = ComposeActionsSectionKind.STEGANOGRAPHY,
            title = "Steganography",
            javaFxSource = "TitledPane(\"Steganography\")",
            composeScope = "LiveSteganographyCard or SteganographyPreviewCard",
            expandedByDefault = false,
        ),
        ComposeActionsSectionPresentation(
            kind = ComposeActionsSectionKind.MEDIA_DEVICES,
            title = "Audio / Video devices",
            javaFxSource = "TitledPane(\"Audio / Video devices\")",
            composeScope = "MediaVoiceCardContent and ExperimentalVideoCardContent",
            expandedByDefault = false,
        ),
        ComposeActionsSectionPresentation(
            kind = ComposeActionsSectionKind.RUNTIME_DIAGNOSTICS,
            title = "Runtime / Diagnostics",
            javaFxSource = "TitledPane(\"Runtime / Diagnostics\")",
            composeScope = "ComposeDiagnosticsState, ComposeRegressionReadinessState, and ComposePackagingReadinessState summaries",
            expandedByDefault = false,
        ),
    ),
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Actions column parity presentation"
    val expandedSections: List<ComposeActionsSectionPresentation> = sections.filter { it.expandedByDefault }
    val collapsedSections: List<ComposeActionsSectionPresentation> = sections.filterNot { it.expandedByDefault }
    val expandedSectionTitles: List<String> = expandedSections.map { it.title }
    val collapsedSectionTitles: List<String> = collapsedSections.map { it.title }
    val javaFxMappingSummary: String = sections.joinToString(" · ") { "${it.javaFxSource} → ${it.composeScope}" }
    val sectionOrderSummary: String = sections.joinToString(" → ") { it.title }
    val visualNoiseSummary: String = if (collapsedSections.isEmpty()) {
        "All action sections are expanded; Compose no longer matches the JavaFX TitledPane density."
    } else {
        "Collapsed by default to match JavaFX desktop density: ${collapsedSectionTitles.joinToString(" · ")}."
    }
    val fallbackLabel: String = if (javaFxFallbackAvailable) {
        "JavaFX actions column remains the fallback while Compose uses matching collapsible sections."
    } else {
        "JavaFX actions fallback unavailable; keep action-section promotion blocked."
    }
    val parityReady: Boolean = javaFxFallbackAvailable &&
        sections.map(ComposeActionsSectionPresentation::kind) == listOf(
            ComposeActionsSectionKind.SELECTED_PEER,
            ComposeActionsSectionKind.TRANSFERS,
            ComposeActionsSectionKind.QUICK_SHARE,
            ComposeActionsSectionKind.STEGANOGRAPHY,
            ComposeActionsSectionKind.MEDIA_DEVICES,
            ComposeActionsSectionKind.RUNTIME_DIAGNOSTICS,
        ) &&
        expandedSectionTitles == listOf("Selected peer", "Transfers") &&
        collapsedSectionTitles == listOf("LAN browser quick share", "Steganography", "Audio / Video devices", "Runtime / Diagnostics")

    fun section(kind: ComposeActionsSectionKind): ComposeActionsSectionPresentation =
        sections.first { it.kind == kind }
}

data class ComposeStatusConnectionState(
    val nickname: String = "Compose Preview",
    val roomPasswordPlaceholder: String = "chatpass",
    val manualHost: String = "127.0.0.1",
    val serverChatPortText: String = NetworkConstants.DEFAULT_CHAT_PORT.toString(),
    val serverFilePortText: String = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT.toString(),
    val clientChatPortText: String = NetworkConstants.DEFAULT_CHAT_PORT.toString(),
    val clientFilePortText: String = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT.toString(),
    val discoverable: Boolean = true,
    val serverStatus: String = "Server stopped",
    val connectionStatus: String = "Connection idle",
    val discoveryStatus: String = "Discovery listener not started in Compose shell",
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
        "Room chat ${serverChatPortText.trim()} · Room files ${serverFilePortText.trim()} · Client chat ${clientChatPortText.trim()} · Client files ${clientFilePortText.trim()}"
    val validationSummary: String = when {
        !nicknameValid -> "Enter a name before opening or joining a room."
        serverChatPort == null || serverFilePort == null -> "Room ports must be valid TCP ports from 1 to 65535."
        !manualHostValid -> "Enter a host address before connecting manually."
        clientChatPort == null || clientFilePort == null -> "Manual connection ports must be valid TCP ports from 1 to 65535."
        localServerRunning && clientConnected -> "Room is open and this client is already connected."
        localServerRunning -> "Room is open; stop hosting before opening another room."
        clientConnected -> "Client is connected; disconnect before starting another manual connection."
        else -> "Inputs are valid for the next runtime status/connection wiring slice."
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

enum class ComposeConnectionEventKind {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

enum class ComposeConnectionCommandKind {
    OPEN_ROOM,
    STOP_HOSTING,
    CONNECT,
    DISCONNECT,
    SET_DISCOVERABLE,
}

data class ComposeConnectionCommand(
    val kind: ComposeConnectionCommandKind,
    val label: String,
    val enabled: Boolean,
    val summary: String,
    val blockedReason: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val statusText: String = if (enabled) summary else blockedReason
    val queuedEvent: ComposeConnectionEvent = ComposeConnectionEvent(
        ComposeConnectionEventKind.INFO,
        "Queued ${
            kind.name.lowercase().replace('_', '-')
        } command for the future live status/connection boundary: $summary",
    )
}

data class ComposeConnectionControlPlan(
    val commands: List<ComposeConnectionCommand>,
) {
    val title: String = "Status/connection control boundary"
    val enabledCommands: List<ComposeConnectionCommand> = commands.filter(ComposeConnectionCommand::enabled)
    val disabledCommands: List<ComposeConnectionCommand> = commands.filterNot(ComposeConnectionCommand::enabled)
    val enabledSummary: String = if (enabledCommands.isEmpty()) {
        "No status/connection controls are ready for Compose-side command dispatch."
    } else {
        "Ready controls: ${enabledCommands.joinToString { it.label }}"
    }
    val disabledSummary: String = if (disabledCommands.isEmpty()) {
        "No status/connection controls are blocked in this state."
    } else {
        "Blocked controls: ${disabledCommands.joinToString { it.label }}"
    }

    fun command(kind: ComposeConnectionCommandKind): ComposeConnectionCommand = commands.first { it.kind == kind }

    companion object {
        fun from(state: ComposeStatusConnectionState): ComposeConnectionControlPlan {
            val plan = state.runtimePlan
            val fallbackBlock = "JavaFX fallback is unavailable; keep live Compose status/connection dispatch disabled."
            fun blocked(defaultReason: String): String =
                if (!state.javaFxFallbackAvailable) fallbackBlock else defaultReason

            return ComposeConnectionControlPlan(
                listOf(
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.OPEN_ROOM,
                        label = "Open room",
                        enabled = state.javaFxFallbackAvailable && state.actionState.openRoomReady && plan.hostingReady,
                        summary = plan.hostingSummary,
                        blockedReason = blocked(plan.disabledReasons.firstOrNull { it.startsWith("Hosting command") }
                            ?: plan.hostingSummary),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.STOP_HOSTING,
                        label = "Stop hosting",
                        enabled = state.javaFxFallbackAvailable && state.actionState.stopHostingReady,
                        summary = "Stop the hosted room, stop discovery announcement, and preserve shutdown cleanup ordering.",
                        blockedReason = blocked("Stop hosting is blocked until a room is currently hosted."),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.CONNECT,
                        label = "Connect",
                        enabled = state.javaFxFallbackAvailable && state.actionState.connectReady && plan.manualConnectionReady,
                        summary = plan.manualConnectionSummary,
                        blockedReason = blocked(plan.disabledReasons.firstOrNull { it.startsWith("Manual connection command") }
                            ?: plan.manualConnectionSummary),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.DISCONNECT,
                        label = "Disconnect",
                        enabled = state.javaFxFallbackAvailable && state.actionState.disconnectReady,
                        summary = "Disconnect the chat client and keep the local file listener only when hosting remains active.",
                        blockedReason = blocked("Disconnect is blocked until the chat client is connected."),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.SET_DISCOVERABLE,
                        label = if (state.discoverable) "Hide room" else "Make discoverable",
                        enabled = state.javaFxFallbackAvailable && state.actionState.discoverabilityToggleReady,
                        summary = if (state.discoverable) {
                            "Switch the hosted room to hidden discovery mode while continuing UDP listen-only updates."
                        } else {
                            "Switch the hosted room back to LAN discovery broadcasts."
                        },
                        blockedReason = blocked("Discovery visibility can change only while a room is hosted."),
                    ),
                ),
            )
        }
    }
}

data class ComposeConnectionEvent(
    val kind: ComposeConnectionEventKind,
    val message: String,
) {
    val displayText: String = "${kind.name.lowercase()}: $message"
}

data class ComposeConnectionEventPreview(
    val events: List<ComposeConnectionEvent>,
) {
    val title: String = "Status/connection event preview"
    val latestEvent: ComposeConnectionEvent? = events.lastOrNull()
    val latestMessage: String = latestEvent?.message ?: "No status or connection events are available yet."
    val hasErrors: Boolean = events.any { it.kind == ComposeConnectionEventKind.ERROR }
    val hasWarnings: Boolean = events.any { it.kind == ComposeConnectionEventKind.WARNING }
    val summary: String = when {
        hasErrors -> "Blocked status/connection actions require attention before live Compose wiring."
        hasWarnings -> "Status/connection actions are partially ready; review warnings before live Compose wiring."
        else -> "Status/connection actions are ready for the next side-effect wiring boundary."
    }

    companion object {
        fun from(state: ComposeStatusConnectionState): ComposeConnectionEventPreview {
            val plan = state.runtimePlan
            return ComposeConnectionEventPreview(
                buildList {
                    add(ComposeConnectionEvent(ComposeConnectionEventKind.INFO, state.serverStatus))
                    add(ComposeConnectionEvent(ComposeConnectionEventKind.INFO, state.connectionStatus))
                    add(ComposeConnectionEvent(ComposeConnectionEventKind.INFO, plan.discoveryAnnouncement))
                    if (plan.hostingReady) {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.SUCCESS, plan.hostingSummary))
                    } else {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.WARNING, plan.hostingSummary))
                    }
                    if (plan.manualConnectionReady) {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.SUCCESS, plan.manualConnectionSummary))
                    } else {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.WARNING, plan.manualConnectionSummary))
                    }
                    plan.disabledReasons.forEach { reason ->
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.ERROR, reason))
                    }
                    if (!state.javaFxFallbackAvailable) {
                        add(
                            ComposeConnectionEvent(
                                ComposeConnectionEventKind.ERROR,
                                "JavaFX fallback is unavailable; live Compose wiring must stay disabled."
                            )
                        )
                    }
                },
            )
        }
    }
}

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
        "Host ${chatServerConfig?.port} with files on ${hostingDiscoveryConfig?.filePort} as ${hostingDiscoveryConfig?.nickname}"
    } else {
        "Hosting plan unavailable until room inputs are valid and hosting is stopped."
    }
    val manualConnectionSummary: String = if (manualConnectionReady) {
        "Connect to ${manualConnectRequest?.host}:${manualConnectRequest?.port} as ${manualConnectRequest?.nickname}; local files on $localFileListenerPort"
    } else {
        "Manual connection plan unavailable until host, ports, and connection state are valid."
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
                add("Hosting command is blocked: ${state.validationSummary}")
            }
            if (!canBuildManualPlan) {
                add("Manual connection command is blocked: ${state.validationSummary}")
            }
            if (state.localServerRunning) {
                add("Open-room command must remain disabled while a room is already hosted.")
            }
            if (state.clientConnected) {
                add("Manual connect command must remain disabled while the client is already connected.")
            }
        }
    }
}

enum class ComposeConnectionLifecycleState {
    IDLE,
    HOSTING_READY,
    HOSTED,
    CONNECTING_READY,
    CONNECTED,
    BLOCKED_ERROR,
}

data class ComposeConnectionLifecycleStep(
    val state: ComposeConnectionLifecycleState,
    val ready: Boolean,
    val label: String,
    val sideEffectContract: String,
) {
    val displayText: String = if (ready) "$label ready" else "$label blocked"
}

data class ComposeConnectionLifecyclePlan(
    val currentState: ComposeConnectionLifecycleState,
    val steps: List<ComposeConnectionLifecycleStep>,
    val blockedReasons: List<String>,
    val fallbackAvailable: Boolean,
    val rollbackFallbackRequired: Boolean,
    val cleanupOrder: List<String>,
) {
    val title: String = "Live status/connection binding contract"
    val stateLabel: String = currentState.name.lowercase().replace('_', '/')
    val readySteps: List<ComposeConnectionLifecycleStep> = steps.filter(ComposeConnectionLifecycleStep::ready)
    val readinessSummary: String = if (readySteps.isEmpty()) {
        "No live lifecycle steps are ready for Compose wiring in the current preview state."
    } else {
        "Ready lifecycle steps: ${readySteps.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedReasons.isEmpty()) {
        "No lifecycle blockers; service calls still remain intentionally deferred to a later slice."
    } else {
        blockedReasons.joinToString(" · ")
    }
    val fallbackStatus: String = if (fallbackAvailable) {
        "JavaFX fallback available for rollback"
    } else {
        "JavaFX fallback unavailable; live Compose binding must remain blocked"
    }
    val cleanupOrderSummary: String = cleanupOrder.joinToString(" → ")
    val sideEffectContractSummary: String = steps.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    fun step(state: ComposeConnectionLifecycleState): ComposeConnectionLifecycleStep = steps.first { it.state == state }

    companion object {
        fun from(
            state: ComposeStatusConnectionState,
            runtimePlan: ComposeConnectionRuntimePlan = state.runtimePlan,
        ): ComposeConnectionLifecyclePlan {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val blockers = blockedReasonsFor(state, fallbackAvailable)
            val currentState = when {
                blockers.isNotEmpty() -> ComposeConnectionLifecycleState.BLOCKED_ERROR
                state.clientConnected -> ComposeConnectionLifecycleState.CONNECTED
                state.localServerRunning -> ComposeConnectionLifecycleState.HOSTED
                else -> ComposeConnectionLifecycleState.IDLE
            }
            val steps = listOf(
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.IDLE,
                    ready = currentState == ComposeConnectionLifecycleState.IDLE && blockers.isEmpty(),
                    label = "Idle",
                    sideEffectContract = "observe validated preview state only; do not start sockets or subscribe to discovery",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.HOSTING_READY,
                    ready = fallbackAvailable && runtimePlan.hostingReady,
                    label = "Hosting-ready",
                    sideEffectContract = "prepare host, local self-connect, file listener, and discovery announcement inputs without invoking services",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.HOSTED,
                    ready = fallbackAvailable && state.localServerRunning,
                    label = "Hosted",
                    sideEffectContract = "reflect hosted-room status and discovery visibility while JavaFX remains runtime owner",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.CONNECTING_READY,
                    ready = fallbackAvailable && runtimePlan.manualConnectionReady,
                    label = "Connecting-ready",
                    sideEffectContract = "prepare manual connect and client file-listener inputs without opening sockets",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.CONNECTED,
                    ready = fallbackAvailable && state.clientConnected,
                    label = "Connected",
                    sideEffectContract = "reflect connected-client status while chat, file-transfer, and RTC signaling stay JavaFX-owned",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.BLOCKED_ERROR,
                    ready = blockers.isNotEmpty(),
                    label = "Blocked/error",
                    sideEffectContract = "surface validation or fallback blockers before any live binding can run",
                ),
            )

            return ComposeConnectionLifecyclePlan(
                currentState = currentState,
                steps = steps,
                blockedReasons = blockers,
                fallbackAvailable = fallbackAvailable,
                rollbackFallbackRequired = true,
                cleanupOrder = cleanupOrderFor(currentState, state.localServerRunning),
            )
        }

        private fun blockedReasonsFor(
            state: ComposeStatusConnectionState,
            fallbackAvailable: Boolean,
        ): List<String> = buildList {
            if (!fallbackAvailable) {
                add("JavaFX fallback is unavailable; rollback safety is required before live Compose binding.")
            }
            if (!state.nicknameValid) {
                add("Nickname is blank; host/connect lifecycle must remain blocked.")
            }
            if (state.serverChatPort == null || state.serverFilePort == null) {
                add("Room ports are invalid; hosted lifecycle cannot be prepared.")
            }
            if (!state.manualHostValid) {
                add("Manual host is blank; connecting lifecycle cannot be prepared.")
            }
            if (state.clientChatPort == null || state.clientFilePort == null) {
                add("Manual connection ports are invalid; connected lifecycle cannot be prepared.")
            }
        }

        private fun cleanupOrderFor(
            state: ComposeConnectionLifecycleState,
            hostingActive: Boolean,
        ): List<String> = when (state) {
            ComposeConnectionLifecycleState.IDLE,
            ComposeConnectionLifecycleState.HOSTING_READY,
            ComposeConnectionLifecycleState.CONNECTING_READY,
                -> listOf("No runtime cleanup is planned while Compose stays side-effect free")

            ComposeConnectionLifecycleState.HOSTED -> listOf(
                "Stop discovery announcement",
                "Disconnect local self-client if attached",
                "Stop hosted chat server",
                "Stop hosted file listener",
                "Return to listen-only discovery",
            )

            ComposeConnectionLifecycleState.CONNECTED -> buildList {
                add("Disconnect chat client")
                add("Stop client-only local file listener")
                if (hostingActive) {
                    add("Keep hosted room running until Stop hosting is requested")
                }
                add("Return to listen-only discovery")
            }

            ComposeConnectionLifecycleState.BLOCKED_ERROR -> listOf(
                "Do not invoke runtime services",
                "Keep JavaFX fallback path active",
                "Fix blockers before retrying live Compose binding",
            )
        }
    }
}

enum class ComposeConnectionTransitionKind {
    START_HOSTING,
    STOP_HOSTING,
    START_MANUAL_CONNECT,
    DISCONNECT_CLIENT,
    CHANGE_DISCOVERY_VISIBILITY,
}

data class ComposeConnectionTransitionIntent(
    val kind: ComposeConnectionTransitionKind,
    val label: String,
    val sourceState: ComposeConnectionLifecycleState,
    val targetState: ComposeConnectionLifecycleState,
    val enabled: Boolean,
    val guardSummary: String,
    val blockedReason: String,
    val cleanupPreview: String,
    val sideEffectContract: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val statusText: String = if (enabled) guardSummary else blockedReason
    val routeSummary: String = "${sourceState.name.lowercase()} -> ${targetState.name.lowercase()}"
    val queuedEvent: ComposeConnectionEvent = ComposeConnectionEvent(
        ComposeConnectionEventKind.INFO,
        "Queued ${
            kind.name.lowercase().replace('_', '-')
        } transition intent for the future live status/connection boundary: $routeSummary; $guardSummary",
    )
}

data class ComposeConnectionTransitionPlan(
    val transitions: List<ComposeConnectionTransitionIntent>,
) {
    val title: String = "Status/connection transition intents"
    val enabledTransitions: List<ComposeConnectionTransitionIntent> =
        transitions.filter(ComposeConnectionTransitionIntent::enabled)
    val blockedTransitions: List<ComposeConnectionTransitionIntent> =
        transitions.filterNot(ComposeConnectionTransitionIntent::enabled)
    val enabledSummary: String = if (enabledTransitions.isEmpty()) {
        "No status/connection transitions are ready for future live Compose wiring."
    } else {
        "Ready transitions: ${enabledTransitions.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedTransitions.isEmpty()) {
        "No status/connection transitions are blocked in this preview state."
    } else {
        "Blocked transitions: ${blockedTransitions.joinToString { it.label }}"
    }
    val cleanupSummary: String = transitions.joinToString(" · ") { "${it.label}: ${it.cleanupPreview}" }
    val sideEffectSummary: String = transitions.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    fun transition(kind: ComposeConnectionTransitionKind): ComposeConnectionTransitionIntent =
        transitions.first { it.kind == kind }

    companion object {
        fun from(
            state: ComposeStatusConnectionState,
            lifecyclePlan: ComposeConnectionLifecyclePlan = state.lifecyclePlan,
            controlPlan: ComposeConnectionControlPlan = state.controlPlan,
        ): ComposeConnectionTransitionPlan {
            val source = lifecyclePlan.currentState
            val fallbackBlock = "JavaFX fallback is unavailable; transition intents must remain local and blocked."
            fun blocked(command: ComposeConnectionCommand): String =
                if (!lifecyclePlan.fallbackAvailable) fallbackBlock else command.blockedReason

            fun cleanupPreviewFor(target: ComposeConnectionLifecycleState): String = when (target) {
                ComposeConnectionLifecycleState.HOSTED -> listOf(
                    "Stop discovery announcement",
                    "Disconnect local self-client if attached",
                    "Stop hosted chat server",
                    "Stop hosted file listener",
                    "Return to listen-only discovery",
                ).joinToString(" → ")

                ComposeConnectionLifecycleState.CONNECTED -> buildList {
                    add("Disconnect chat client")
                    add("Stop client-only local file listener")
                    if (state.localServerRunning) {
                        add("Keep hosted room running until Stop hosting is requested")
                    }
                    add("Return to listen-only discovery")
                }.joinToString(" → ")

                ComposeConnectionLifecycleState.BLOCKED_ERROR -> listOf(
                    "Do not invoke runtime services",
                    "Keep JavaFX fallback path active",
                    "Fix blockers before retrying live Compose binding",
                ).joinToString(" → ")

                ComposeConnectionLifecycleState.IDLE,
                ComposeConnectionLifecycleState.HOSTING_READY,
                ComposeConnectionLifecycleState.CONNECTING_READY,
                    -> "No runtime cleanup is planned while Compose stays side-effect free"
            }

            val openRoom = controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM)
            val stopHosting = controlPlan.command(ComposeConnectionCommandKind.STOP_HOSTING)
            val connect = controlPlan.command(ComposeConnectionCommandKind.CONNECT)
            val disconnect = controlPlan.command(ComposeConnectionCommandKind.DISCONNECT)
            val discoverability = controlPlan.command(ComposeConnectionCommandKind.SET_DISCOVERABLE)

            return ComposeConnectionTransitionPlan(
                listOf(
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.START_HOSTING,
                        label = "Start hosting transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.HOSTED,
                        enabled = source == ComposeConnectionLifecycleState.IDLE && openRoom.enabled && lifecyclePlan.step(
                            ComposeConnectionLifecycleState.HOSTING_READY
                        ).ready,
                        guardSummary = "Host inputs, local self-connect, file listener, and discovery config are prepared for a future live host transition.",
                        blockedReason = blocked(openRoom),
                        cleanupPreview = cleanupPreviewFor(ComposeConnectionLifecycleState.HOSTED),
                        sideEffectContract = "future implementation may start host services only after this local intent is accepted by a runtime adapter",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.STOP_HOSTING,
                        label = "Stop hosting transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.IDLE,
                        enabled = stopHosting.enabled,
                        guardSummary = "Hosted-room cleanup order is explicit before any future runtime stop call is allowed.",
                        blockedReason = blocked(stopHosting),
                        cleanupPreview = lifecyclePlan.cleanupOrderSummary,
                        sideEffectContract = "future implementation may stop discovery, local self-client, server, and file listener in the documented order",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.START_MANUAL_CONNECT,
                        label = "Manual connect transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.CONNECTED,
                        enabled = source == ComposeConnectionLifecycleState.IDLE && connect.enabled && lifecyclePlan.step(
                            ComposeConnectionLifecycleState.CONNECTING_READY
                        ).ready,
                        guardSummary = "Manual host, chat port, nickname, password, and local file listener are prepared for a future live connect transition.",
                        blockedReason = blocked(connect),
                        cleanupPreview = cleanupPreviewFor(ComposeConnectionLifecycleState.CONNECTED),
                        sideEffectContract = "future implementation may connect only through chat-core service boundaries after this intent is accepted",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.DISCONNECT_CLIENT,
                        label = "Disconnect transition",
                        sourceState = source,
                        targetState = if (state.localServerRunning) ComposeConnectionLifecycleState.HOSTED else ComposeConnectionLifecycleState.IDLE,
                        enabled = disconnect.enabled,
                        guardSummary = "Client disconnect cleanup is separated from hosted-room cleanup when hosting remains active.",
                        blockedReason = blocked(disconnect),
                        cleanupPreview = lifecyclePlan.cleanupOrderSummary,
                        sideEffectContract = "future implementation may disconnect the chat client without changing hosted-room ownership unless requested separately",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.CHANGE_DISCOVERY_VISIBILITY,
                        label = "Discovery visibility transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.HOSTED,
                        enabled = discoverability.enabled,
                        guardSummary = "Discovery visibility change is local-intent only and keeps UDP payload format unchanged.",
                        blockedReason = blocked(discoverability),
                        cleanupPreview = "No cleanup; future runtime wiring may only switch announcement visibility while keeping listen-only discovery available.",
                        sideEffectContract = "future implementation may update discovery announcement visibility without changing ports or payload format",
                    ),
                ),
            )
        }
    }
}

enum class ComposeAdapterEventKind {
    HOST_STARTED,
    HOST_STOPPED,
    CONNECT_STARTED,
    CONNECTED,
    CONNECT_FAILED,
    DISCONNECTED,
    DISCOVERY_VISIBILITY_CHANGED,
    RUNTIME_ERROR,
    CLEANUP_STARTED,
    CLEANUP_COMPLETED,
}

data class ComposeAdapterEventContract(
    val kind: ComposeAdapterEventKind,
    val label: String,
    val ready: Boolean,
    val guarded: Boolean,
    val description: String,
    val prerequisites: List<String>,
    val blockedReason: String,
    val cleanupAfter: List<ComposeAdapterEventKind>,
) {
    val routeTag: String = kind.name.lowercase().replace('_', '-')
    val readinessLabel: String = when {
        !ready && guarded -> "$label blocked (guarded)"
        !ready -> "$label blocked"
        else -> "$label ready"
    }
    val eventOrderNote: String = if (cleanupAfter.isEmpty()) "standalone" else "after ${
        cleanupAfter.joinToString {
            it.name.lowercase().replace('_', '-')
        }
    }"
}

data class ComposeAdapterEventRouting(
    val contracts: List<ComposeAdapterEventContract>,
    val summary: String,
    val fallbackAvailable: Boolean,
    val readyEvents: List<ComposeAdapterEventContract>,
    val blockedEvents: List<ComposeAdapterEventContract>,
    val cleanupOrderSummary: String,
) {
    val title: String = "Host runtime adapter event contract"
    val subtitle: String =
        "Side-effect-free event contract for future live status/connection integration; JavaFX still owns all runtime services."
    val readyCount: Int = readyEvents.size
    val blockedCount: Int = blockedEvents.size
    val totalCount: Int = contracts.size
    val readinessSummary: String = when {
        readyCount == totalCount -> "All $totalCount adapter events are ready for future live wiring."
        blockedCount == totalCount -> "All $totalCount adapter events are blocked; JavaFX fallback must remain active."
        else -> "$readyCount of $totalCount adapter events ready; $blockedCount blocked."
    }
    val fallbackStatus: String =
        if (fallbackAvailable) "JavaFX fallback available; adapter event routing is speculative." else "JavaFX fallback unavailable; live adapter event routing must remain blocked."
    val blockedSummary: String = if (blockedEvents.isEmpty()) {
        "No adapter events are blocked; all event contracts are ready for the future runtime adapter boundary."
    } else {
        blockedEvents.joinToString(" · ") { "${it.readinessLabel}: ${it.blockedReason}" }
    }
    val eventOrderLabel: String = contracts.joinToString(" → ") { it.routeTag }

    companion object {
        fun from(
            state: ComposeStatusConnectionState,
            lifecyclePlan: ComposeConnectionLifecyclePlan = state.lifecyclePlan,
            runtimePlan: ComposeConnectionRuntimePlan = state.runtimePlan,
        ): ComposeAdapterEventRouting {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val fallbackBlock = "JavaFX fallback is unavailable; live adapter event routing must remain blocked."
            fun block(reason: String): String = if (!fallbackAvailable) fallbackBlock else reason

            val hostingReady =
                fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.HOSTING_READY).ready
            val hosted = fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.HOSTED).ready
            val connectingReady =
                fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.CONNECTING_READY).ready
            val connected = fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.CONNECTED).ready

            val hostStartedReady = hostingReady && !hosted
            val hostStartedBlocked = if (!fallbackAvailable) fallbackBlock
            else if (!state.nicknameValid) "Nickname is blank; host-started event cannot be received."
            else if (state.serverChatPort == null || state.serverFilePort == null) "Room ports are invalid; host-started event cannot be received."
            else if (state.localServerRunning) "Room is already hosted; host-started event is redundant while hosted-event is in effect."
            else "Host-started event prerequisites are not met."

            val hostStoppedReady = hosted
            val hostStoppedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Room is not currently hosted; host-stopped event can only fire after a hosted state."

            val connectStartedReady = connectingReady && !state.clientConnected
            val connectStartedBlocked = if (!fallbackAvailable) fallbackBlock
            else if (!state.nicknameValid) "Nickname is blank; connect-started event cannot be received."
            else if (!state.manualHostValid) "Manual host is blank; connect-started event cannot be received."
            else if (state.clientChatPort == null || state.clientFilePort == null) "Manual connection ports are invalid; connect-started event cannot be received."
            else if (state.clientConnected) "Client is already connected; connect-started event is redundant while connected-event is in effect."
            else "Connect-started event prerequisites are not met."

            val connectedReady = connected
            val connectedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Client is not connected; connected event can only fire after a live manual connection is established."

            val connectFailedReady = connectingReady
            val connectFailedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Manual connection inputs are not ready; connect-failed event cannot fire without a valid connect target."

            val disconnectedReady = connected
            val disconnectedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Client is not connected; disconnected event can only fire after a client disconnect occurs."

            val discoveryVisibilityReady = hosted
            val discoveryVisibilityBlocked = if (!fallbackAvailable) fallbackBlock
            else "Room is not hosted; discovery visibility change events can only fire while a room is hosted."

            val runtimeErrorBlocked = if (!fallbackAvailable) fallbackBlock
            else "No runtime errors are expected in this preview state; runtime-error event is guarded and fires only on actual errors."

            val cleanupStartedReady = hosted || connected
            val cleanupStartedBlocked = if (!fallbackAvailable) fallbackBlock
            else "No active runtime state requires cleanup; cleanup-started event is guarded until hosting or connection is active."

            val cleanupCompletedReady = hosted || connected
            val cleanupCompletedBlocked = if (!fallbackAvailable) fallbackBlock
            else "No active runtime state requires cleanup completion; cleanup-completed event is guarded until cleanup-started fires."

            val contracts = listOf(
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.HOST_STARTED,
                    label = "Host started",
                    ready = hostStartedReady,
                    guarded = !hostStartedReady && block(hostStartedBlocked) == hostStartedBlocked && fallbackAvailable,
                    description = "Future runtime adapter fires this when the hosted chat server, file listener, and discovery announcement are live.",
                    prerequisites = listOf("valid nickname", "valid room ports", "not already hosted"),
                    blockedReason = block(hostStartedBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.HOST_STOPPED,
                    label = "Host stopped",
                    ready = hostStoppedReady,
                    guarded = !hostStoppedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this after discovery, self-client, chat server, and file listener are stopped.",
                    prerequisites = listOf("room currently hosted"),
                    blockedReason = block(hostStoppedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.HOST_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CONNECT_STARTED,
                    label = "Connect started",
                    ready = connectStartedReady,
                    guarded = !connectStartedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when a manual chat-client connection attempt begins.",
                    prerequisites = listOf("valid manual host", "valid manual ports", "not already connected"),
                    blockedReason = block(connectStartedBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CONNECTED,
                    label = "Connected",
                    ready = connectedReady,
                    guarded = !connectedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when the chat client successfully connects to a remote peer.",
                    prerequisites = listOf("client connection active"),
                    blockedReason = block(connectedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CONNECT_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CONNECT_FAILED,
                    label = "Connect failed",
                    ready = connectFailedReady,
                    guarded = true,
                    description = "Future runtime adapter fires this when a manual connection attempt fails; it is guarded and only fires on actual failures.",
                    prerequisites = listOf("valid manual connection inputs ready"),
                    blockedReason = block(connectFailedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CONNECT_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.DISCONNECTED,
                    label = "Disconnected",
                    ready = disconnectedReady,
                    guarded = !disconnectedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this after the chat client disconnects and cleanup order is observed.",
                    prerequisites = listOf("client was connected"),
                    blockedReason = block(disconnectedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CONNECTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED,
                    label = "Discovery visibility changed",
                    ready = discoveryVisibilityReady,
                    guarded = !discoveryVisibilityReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when discovery announcement visibility switches without changing ports or payload format.",
                    prerequisites = listOf("room currently hosted"),
                    blockedReason = block(discoveryVisibilityBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.HOST_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.RUNTIME_ERROR,
                    label = "Runtime error",
                    ready = false,
                    guarded = true,
                    description = "Future runtime adapter fires this when an unexpected runtime error occurs; it is always guarded and fires only on exceptional conditions.",
                    prerequisites = listOf("actual runtime error occurs"),
                    blockedReason = block(runtimeErrorBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CLEANUP_STARTED,
                    label = "Cleanup started",
                    ready = cleanupStartedReady,
                    guarded = !cleanupStartedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when the documented cleanup order begins executing.",
                    prerequisites = listOf("active runtime state (hosted or connected)"),
                    blockedReason = block(cleanupStartedBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CLEANUP_COMPLETED,
                    label = "Cleanup completed",
                    ready = cleanupCompletedReady,
                    guarded = !cleanupCompletedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this after the cleanup order is fully executed and resources are released.",
                    prerequisites = listOf("cleanup was started"),
                    blockedReason = block(cleanupCompletedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CLEANUP_STARTED),
                ),
            )

            val readyEvents = contracts.filter { it.ready }
            val blockedEvents = contracts.filterNot { it.ready }
            val cleanupOrderSummary =
                if (contracts.none { it.ready && it.kind == ComposeAdapterEventKind.CLEANUP_COMPLETED }) {
                    "Cleanup event order is not yet applicable; no active runtime state requires cleanup."
                } else {
                    "Cleanup events fire in deterministic order: cleanup-started → cleanup-completed after hosted/connected events are resolved."
                }

            return ComposeAdapterEventRouting(
                contracts = contracts,
                summary = lifecyclePlan.sideEffectContractSummary,
                fallbackAvailable = fallbackAvailable,
                readyEvents = readyEvents,
                blockedEvents = blockedEvents,
                cleanupOrderSummary = cleanupOrderSummary,
            )
        }
    }
}

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
        if (discoverabilityToggleReady) "Discovery toggle ready" else "Discovery toggle waits for hosting"
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

data class ComposePeerListState(
    val peers: List<ComposePeerListItem> = ComposePeerListItem.defaultPreviewItems(clientConnected = false),
    val selectedPeerIndex: Int = 0,
    val selectedPeerNickname: String? = null,
    val selectedTargetKind: ComposePeerTargetCommandKind? = null,
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Contacts / Peers"
    val hint: String =
        "Discovered LAN peers appear here. Select one to connect before sending files or starting a call."
    val visiblePeers: List<ComposePeerListItem> = peers.sortedWith(
        compareByDescending<ComposePeerListItem> { it.online }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nickname },
    )
    val resolvedSelectedPeerIndex: Int = resolveSelectedPeerIndex()
    val selectedPeer: ComposePeerListItem? = visiblePeers.getOrNull(resolvedSelectedPeerIndex)
    val selectedPeerTitle: String = selectedPeer?.nickname ?: "No peer selected"
    val selectedPeerMeta: String =
        selectedPeer?.selectedMeta ?: "Choose an online chat peer to send files or start a voice/video session."
    val peerStatus: String =
        selectedPeer?.let { if (it.online) "Peer ${it.nickname}" else "Peer offline" } ?: "Peer not selected"
    val actionSummary: String = selectedTargetKind?.let { kind ->
        selectedPeer?.let { "${kind.displayName} target selected for ${it.nickname}. Runtime action remains blocked until its feature slice is validated." }
    } ?: selectedPeer?.actionSummary ?: "Select an online peer before enabling chat, file, voice, or video actions."
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX peer list remains production fallback" else "JavaFX peer list fallback unavailable"
    val targetActions: ComposePeerTargetActions = ComposePeerTargetActions.from(selectedPeer)
    val targetControlPlan: ComposePeerTargetControlPlan =
        ComposePeerTargetControlPlan.from(selectedPeer, targetActions, javaFxFallbackAvailable, selectedTargetKind)
    val peerListLifecyclePlan: ComposePeerListLifecyclePlan = ComposePeerListLifecyclePlan.from(this)
    val peerListTransitionPlan: ComposePeerListTransitionPlan =
        ComposePeerListTransitionPlan.from(this, peerListLifecyclePlan, targetControlPlan)
    val peerListAdapterEventRouting: ComposePeerListAdapterEventRouting = ComposePeerListAdapterEventRouting.from(this)

    fun selectionKeyFor(index: Int): String? = visiblePeers.getOrNull(index)?.nickname

    private fun resolveSelectedPeerIndex(): Int {
        val indexByNickname = selectedPeerNickname?.let { selectedNickname ->
            visiblePeers.indexOfFirst { it.nickname.equals(selectedNickname, ignoreCase = true) }
        } ?: -1
        return when {
            indexByNickname >= 0 -> indexByNickname
            selectedPeerIndex in visiblePeers.indices -> selectedPeerIndex
            else -> -1
        }
    }
}

val ComposePeerTargetCommandKind.displayName: String
    get() = when (this) {
        ComposePeerTargetCommandKind.CHAT_TARGET -> "Chat"
        ComposePeerTargetCommandKind.FILE_TARGET -> "File"
        ComposePeerTargetCommandKind.VOICE_TARGET -> "Voice"
        ComposePeerTargetCommandKind.VIDEO_TARGET -> "Video"
        ComposePeerTargetCommandKind.DATA_TARGET -> "RTC data"
    }

enum class ComposePeerTargetCommandKind {
    CHAT_TARGET,
    FILE_TARGET,
    VOICE_TARGET,
    VIDEO_TARGET,
    DATA_TARGET,
}

data class ComposePeerTargetCommand(
    val kind: ComposePeerTargetCommandKind,
    val label: String,
    val enabled: Boolean,
    val summary: String,
    val blockedReason: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val statusText: String = if (enabled) summary else blockedReason
    val queuedEvent: ComposeConnectionEvent = ComposeConnectionEvent(
        ComposeConnectionEventKind.INFO,
        "Queued ${kind.name.lowercase().replace('_', '-')} intent for the future live peer-list boundary: $summary",
    )
}

data class ComposePeerTargetControlPlan(
    val commands: List<ComposePeerTargetCommand>,
) {
    val title: String = "Selected-peer command boundary"
    val enabledCommands: List<ComposePeerTargetCommand> = commands.filter(ComposePeerTargetCommand::enabled)
    val disabledCommands: List<ComposePeerTargetCommand> = commands.filterNot(ComposePeerTargetCommand::enabled)
    val enabledSummary: String = if (enabledCommands.isEmpty()) {
        "No selected-peer commands are ready for Compose-side dispatch."
    } else {
        "Ready peer commands: ${enabledCommands.joinToString { it.label }}"
    }
    val disabledSummary: String = if (disabledCommands.isEmpty()) {
        "No selected-peer commands are blocked in this state."
    } else {
        "Blocked peer commands: ${disabledCommands.joinToString { it.label }}"
    }

    fun command(kind: ComposePeerTargetCommandKind): ComposePeerTargetCommand = commands.first { it.kind == kind }

    companion object {
        fun from(
            peer: ComposePeerListItem?,
            targetActions: ComposePeerTargetActions,
            javaFxFallbackAvailable: Boolean,
            selectedTargetKind: ComposePeerTargetCommandKind? = null,
        ): ComposePeerTargetControlPlan {
            val peerName = peer?.nickname ?: "selected peer"
            val fallbackBlock = "JavaFX peer-list fallback is unavailable; keep live Compose peer dispatch disabled."
            fun blocked(defaultReason: String): String = if (!javaFxFallbackAvailable) fallbackBlock else defaultReason
            fun firstBlockedReason(): String = targetActions.blockedReasons.firstOrNull()
                ?: "Select an online peer before dispatching peer-targeted commands."
            fun label(kind: ComposePeerTargetCommandKind, base: String): String =
                if (kind == selectedTargetKind) "$base selected" else base

            return ComposePeerTargetControlPlan(
                listOf(
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.CHAT_TARGET,
                        label = label(ComposePeerTargetCommandKind.CHAT_TARGET, "Use for chat"),
                        enabled = javaFxFallbackAvailable && targetActions.chatReady,
                        summary = "Prepare $peerName as the chat target while preserving shared-room messaging semantics.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.FILE_TARGET,
                        label = label(ComposePeerTargetCommandKind.FILE_TARGET, "Use for files"),
                        enabled = javaFxFallbackAvailable && targetActions.fileReady,
                        summary = "Prepare $peerName as the encrypted file-transfer target using its available file receiver endpoint.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.VOICE_TARGET,
                        label = label(ComposePeerTargetCommandKind.VOICE_TARGET, "Use for voice"),
                        enabled = javaFxFallbackAvailable && targetActions.voiceReady,
                        summary = "Prepare $peerName as the voice-call target while keeping WebRTC runtime ownership in JavaFX.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.VIDEO_TARGET,
                        label = label(ComposePeerTargetCommandKind.VIDEO_TARGET, "Use for video"),
                        enabled = javaFxFallbackAvailable && targetActions.videoReady,
                        summary = "Prepare $peerName as the experimental video target without changing fallback diagnostics.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.DATA_TARGET,
                        label = label(ComposePeerTargetCommandKind.DATA_TARGET, "Use for RTC data"),
                        enabled = javaFxFallbackAvailable && targetActions.dataChannelReady,
                        summary = "Prepare $peerName as the RTC data-channel target while keeping signaling routed through chat-core.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                ),
            )
        }
    }
}

data class ComposePeerTargetActions(
    val chatReady: Boolean,
    val fileReady: Boolean,
    val voiceReady: Boolean,
    val videoReady: Boolean,
    val dataChannelReady: Boolean,
    val blockedReasons: List<String>,
) {
    val title: String = "Peer target action readiness"
    val chatLabel: String = if (chatReady) "Chat target ready" else "Chat target blocked"
    val fileLabel: String = if (fileReady) "File transfer target ready" else "File transfer target blocked"
    val voiceLabel: String = if (voiceReady) "Voice target ready" else "Voice target blocked"
    val videoLabel: String = if (videoReady) "Experimental video target ready" else "Experimental video target blocked"
    val dataChannelLabel: String = if (dataChannelReady) "RTC data target ready" else "RTC data target blocked"
    val summary: String = listOf(chatLabel, fileLabel, voiceLabel, videoLabel, dataChannelLabel).joinToString(" · ")
    val blockedSummary: String = if (blockedReasons.isEmpty()) {
        "All selected-peer actions are ready for the next live peer-list wiring boundary."
    } else {
        blockedReasons.joinToString(" · ")
    }

    companion object {
        fun from(peer: ComposePeerListItem?): ComposePeerTargetActions {
            val online = peer?.online == true
            val fileCapable = peer?.fileCapable == true
            val voiceCapable = peer?.voiceCapable == true
            val videoCapable = peer?.videoCapable == true
            val dataChannelCapable = peer?.dataChannelCapable == true
            return ComposePeerTargetActions(
                chatReady = online,
                fileReady = online && fileCapable,
                voiceReady = online && voiceCapable,
                videoReady = online && videoCapable,
                dataChannelReady = online && dataChannelCapable,
                blockedReasons = blockedReasonsFor(peer, online, fileCapable, voiceCapable, videoCapable, dataChannelCapable),
            )
        }

        private fun blockedReasonsFor(
            peer: ComposePeerListItem?,
            online: Boolean,
            fileCapable: Boolean,
            voiceCapable: Boolean,
            videoCapable: Boolean,
            dataChannelCapable: Boolean,
        ): List<String> = buildList {
            if (peer == null) {
                add("Select an online peer before enabling chat, file, voice, video, or RTC data actions.")
                return@buildList
            }
            if (!online) {
                add("Selected peer is offline; wait for chat presence or discovery refresh.")
            }
            if (online && !fileCapable) {
                add("Encrypted file transfer is blocked until the peer has an available file receiver endpoint.")
            }
            if (online && (!voiceCapable || !videoCapable)) {
                add("Voice and video are blocked because the selected peer does not advertise RTC media support.")
            }
            if (online && !dataChannelCapable) {
                add("RTC data is blocked because the selected peer does not advertise data-channel support.")
            }
        }
    }
}

/**
 * JavaFX → Compose mapping for the Actions-column selected-peer header:
 * selectedPeerTitleValue / selectedPeerMetaValue become deterministic selected-peer summary copy;
 * Attach / Voice call / Video call / End call become quick-action readiness flags;
 * JavaFX disabled states become explicit enabled flags plus blocked reasons.
 */
data class ComposeSelectedPeerQuickActionsState(
    val peerListState: ComposePeerListState,
    val clientConnected: Boolean,
    val voiceRuntimeReady: Boolean = true,
    val videoRuntimeReady: Boolean = true,
    val hangUpReady: Boolean = false,
    val javaFxFallbackAvailable: Boolean = peerListState.javaFxFallbackAvailable,
) {
    val title: String = peerListState.selectedPeerTitle
    val meta: String = peerListState.selectedPeerMeta
    val selectedPeer: ComposePeerListItem? = peerListState.selectedPeer
    val selectedPeerStatus: String = peerListState.peerStatus
    val fileTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.fileCapable
    val voiceTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.voiceCapable
    val videoTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.videoCapable
    val realtimeTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.realtimeCapable
    val attachEnabled: Boolean = clientConnected && fileTargetReady && javaFxFallbackAvailable
    val voiceEnabled: Boolean = clientConnected && voiceTargetReady && voiceRuntimeReady && javaFxFallbackAvailable
    val videoEnabled: Boolean = clientConnected && videoTargetReady && videoRuntimeReady && javaFxFallbackAvailable
    val hangUpEnabled: Boolean = hangUpReady && realtimeTargetReady && javaFxFallbackAvailable
    val attachLabel: String = if (attachEnabled) "Attach ready" else "Attach blocked"
    val voiceLabel: String = if (voiceEnabled) "Voice call ready" else "Voice call blocked"
    val videoLabel: String = if (videoEnabled) "Video call ready" else "Video call blocked"
    val hangUpLabel: String = if (hangUpEnabled) "End call ready" else "End call blocked"
    val enabledActionLabels: List<String> = buildList {
        if (attachEnabled) add("Attach")
        if (voiceEnabled) add("Voice call")
        if (videoEnabled) add("Video call")
        if (hangUpEnabled) add("End call")
    }
    val readinessLabel: String = if (enabledActionLabels.isEmpty()) {
        "Quick actions blocked"
    } else {
        "Ready: ${enabledActionLabels.joinToString()}"
    }
    val actionSummary: String = selectedPeer?.let { peer ->
        if (clientConnected) {
            "Actions target “${peer.nickname}”. Text chat remains shared; quick actions use peer-specific readiness."
        } else {
            "Selected “${peer.nickname}”. Connect to chat before sending files or starting calls."
        }
    } ?: "No peer selected. Choose an online chat peer to send files or start a voice/video session."
    val blockedReasons: List<String> = buildList {
        if (selectedPeer == null) {
            add("Select an online peer before using quick actions.")
        } else {
            if (!selectedPeer.online) {
                add("Selected peer is offline; quick actions wait for discovery or chat presence refresh.")
            }
            if (selectedPeer.online && !selectedPeer.fileCapable) {
                add("Attach is blocked until a file receiver endpoint is available.")
            }
            if (selectedPeer.online && !selectedPeer.voiceCapable) {
                add("Voice call is blocked because the selected peer does not advertise voice support.")
            }
            if (selectedPeer.online && !selectedPeer.videoCapable) {
                add("Video call is blocked because the selected peer does not advertise video support.")
            }
        }
        if (!clientConnected) {
            add("Connect to chat before sending files or starting calls.")
        }
        if (clientConnected && voiceTargetReady && !voiceRuntimeReady) {
            add("Voice runtime is not ready; keep Voice call disabled.")
        }
        if (clientConnected && videoTargetReady && !videoRuntimeReady) {
            add("Video runtime is not ready; keep Video call disabled.")
        }
        if (!javaFxFallbackAvailable) {
            add("JavaFX fallback is unavailable; keep live Compose quick actions disabled.")
        }
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Quick actions are ready for ${selectedPeer?.nickname ?: "the selected peer"}: Attach, Voice call, and Video call."
    } else {
        blockedReasons.joinToString(" · ")
    }
    val buttonReadinessSummary: String = listOf(attachLabel, voiceLabel, videoLabel, hangUpLabel).joinToString(" · ")
}

data class ComposePeerListItem(
    val nickname: String,
    val online: Boolean,
    val discovered: Boolean,
    val listMeta: String,
    val selectedMeta: String,
    val filePort: Int = 0,
    val voiceCapable: Boolean = true,
    val videoCapable: Boolean = true,
    val dataChannelCapable: Boolean = true,
    val fileCapableOverride: Boolean? = null,
) {
    val fileCapable: Boolean = fileCapableOverride ?: (discovered || filePort > 0)
    val realtimeCapable: Boolean = voiceCapable || videoCapable || dataChannelCapable
    val availabilityLabel: String = if (online) "Online" else "Offline"
    val actionSummary: String = when {
        !online -> "Offline target; wait for chat or discovery refresh before enabling actions."
        !realtimeCapable && fileCapable -> "Chat and encrypted file transfer can target this peer after connection; voice, video, and RTC data are unavailable for this client."
        !realtimeCapable -> "Chat can target this peer after connection; voice, video, RTC data, and encrypted file transfer are unavailable for this client."
        discovered -> "Chat, encrypted file transfer, voice, and experimental video can target this peer after connection."
        filePort > 0 -> "Chat, encrypted file transfer, voice, and experimental video can target this peer after connection; file receiver was inferred from chat."
        else -> "Chat, voice, and experimental video can target this peer after connection; encrypted file transfer needs a file receiver endpoint."
    }

    companion object {
        fun fromPeer(peer: PeerPresence, clientConnected: Boolean): ComposePeerListItem {
            val capabilities = peer.capabilities()
            val unknownCapabilities = capabilities == PeerCapabilities.unknown()
            return ComposePeerListItem(
                nickname = peer.nickname(),
                online = peer.online(),
                discovered = peer.discovered() && !peer.peerId().isNullOrBlank(),
                listMeta = DesktopPeerFormatters.formatListMeta(peer),
                selectedMeta = DesktopPeerFormatters.formatSelectedPeerMeta(peer, clientConnected),
                filePort = peer.filePort(),
                voiceCapable = unknownCapabilities || capabilities.supportsVoice(),
                videoCapable = unknownCapabilities || capabilities.supportsVideo(),
                dataChannelCapable = unknownCapabilities || capabilities.supportsRtcDataChannel(),
                fileCapableOverride = DesktopMainViewHelpers.selectedPeerFileCapable(peer),
            )
        }

        fun fromDiscoveredPeer(peer: DiscoveredPeer): ComposePeerListItem = fromPeer(
            PeerPresence(
                peer.nickname,
                true,
                peer.peerId,
                peer.host,
                peer.chatPort,
                peer.filePort,
                peer.lastSeen,
            ),
            clientConnected = true,
        )

        fun defaultPreviewItems(clientConnected: Boolean): List<ComposePeerListItem> = listOf(
            fromPeer(
                PeerPresence(
                    "Astra Laptop",
                    true,
                    "peer-astra",
                    "192.168.1.20",
                    NetworkConstants.DEFAULT_CHAT_PORT,
                    NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
                    Instant.parse("2026-05-22T09:00:00Z"),
                ),
                clientConnected,
            ),
            fromPeer(
                PeerPresence(
                    "Beta Phone",
                    true,
                    "peer-beta",
                    null,
                    0,
                    0,
                    Instant.parse("2026-05-22T09:01:00Z"),
                ),
                clientConnected,
            ),
            fromPeer(
                PeerPresence(
                    "Offline NAS",
                    false,
                    "peer-nas",
                    "192.168.1.30",
                    NetworkConstants.DEFAULT_CHAT_PORT,
                    NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
                    Instant.parse("2026-05-22T08:45:00Z"),
                ),
                clientConnected,
            ),
        )
    }
}

data class ComposeChatMessage(
    val sender: String,
    val text: String,
    val system: Boolean = false,
) {
    val displayText: String = if (system) "[$sender] $text" else "$sender: $text"
}

data class ComposeChatWorkspaceState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val draftMessage: String = "Hello from Compose preview",
    val messages: List<ComposeChatMessage> = defaultPreviewMessages(),
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Shared room activity"
    val subtitle: String = peerListState.selectedPeer?.let { peer ->
        "Actions on the right will target “${peer.nickname}”. Text chat remains shared for now."
    } ?: "Connect to chat, then select a peer on the left for voice, video, and file actions."
    val transcriptLines: List<String> = messages.map(ComposeChatMessage::displayText)
    val transcriptSummary: String =
        if (messages.isEmpty()) "No chat messages yet." else "Preview transcript lines: ${messages.size}"
    val draftValid: Boolean = draftMessage.trim().isNotEmpty()
    val canSendMessage: Boolean = statusState.clientConnected && draftValid
    val sendLabel: String = if (canSendMessage) "Send ready" else "Send blocked"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX chat workspace remains production fallback" else "JavaFX chat workspace fallback unavailable"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) {
            add("Connect to chat before sending shared-room messages.")
        }
        if (!draftValid) {
            add("Type a non-empty message before sending.")
        }
        if (!javaFxFallbackAvailable) {
            add("JavaFX fallback is unavailable; keep live Compose chat sending disabled.")
        }
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Chat input is ready for the next live workspace wiring boundary."
    } else {
        blockedReasons.joinToString(" · ")
    }

    companion object {
        fun defaultPreviewMessages(): List<ComposeChatMessage> = listOf(
            ComposeChatMessage("connected", "Compose Preview -> 127.0.0.1", system = true),
            ComposeChatMessage("Astra Laptop", "Desktop chat transcript preview"),
            ComposeChatMessage("join", "Beta Phone", system = true),
        )
    }
}

data class ComposeIncomingTransferPrompt(
    val id: String,
    val senderId: String,
    val fileName: String,
    val fileSize: Long,
    val remoteAddress: String,
    val status: ComposeIncomingTransferPromptStatus = ComposeIncomingTransferPromptStatus.WAITING,
) {
    val title: String = DesktopTransferFormatters.incomingFileTitle()
    val header: String = DesktopTransferFormatters.incomingFileHeader(senderId)
    val content: String = DesktopTransferFormatters.incomingFileContent(fileName, fileSize, remoteAddress)
    val sizeLabel: String = DesktopTransferFormatters.formatMegabytes(fileSize)
    val statusLabel: String = status.label
    val waitingForDecision: Boolean = status == ComposeIncomingTransferPromptStatus.WAITING

    fun withStatus(nextStatus: ComposeIncomingTransferPromptStatus): ComposeIncomingTransferPrompt = copy(status = nextStatus)

    companion object {
        fun from(
            metadata: FileTransferMetadata,
            remoteAddress: String,
            status: ComposeIncomingTransferPromptStatus = ComposeIncomingTransferPromptStatus.WAITING,
        ): ComposeIncomingTransferPrompt =
            ComposeIncomingTransferPrompt(
                id = metadata.transferId,
                senderId = metadata.senderId,
                fileName = metadata.fileName,
                fileSize = metadata.fileSize,
                remoteAddress = remoteAddress,
                status = status,
            )
    }
}

enum class ComposeIncomingTransferPromptStatus(val label: String) {
    WAITING("Needs your decision"),
    ACCEPTED("Accepted"),
    AUTO_ACCEPTED("Auto-accepted"),
    REJECTED("Rejected"),
}

data class ComposeFileTransferState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val selectedFilePath: String = "",
    val senderId: String = statusState.nickname,
    val sessionPassword: String = "",
    val entries: List<TransferEntry> = emptyList(),
    val incomingPrompts: List<ComposeIncomingTransferPrompt> = emptyList(),
    val autoAcceptFiles: Boolean = false,
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Encrypted file transfer"
    val selectedPeer = peerListState.selectedPeer
    val selectedPeerName: String = selectedPeer?.nickname ?: "No peer selected"
    val hasSelectedFile: Boolean = selectedFilePath.trim().isNotEmpty()
    val senderReady: Boolean = senderId.trim().isNotEmpty()
    val passwordReady: Boolean = sessionPassword.isNotEmpty()
    val sendTargetReady: Boolean = statusState.clientConnected && selectedPeer?.online == true && selectedPeer.fileCapable
    val listenerReady: Boolean = statusState.resolvedLocalFilePort != null
    val canSendSelectedFile: Boolean = sendTargetReady && hasSelectedFile && senderReady && passwordReady && javaFxFallbackAvailable
    val activeCount: Long = entries.count { it.active() }.toLong()
    val completedCount: Int = entries.count { it.status == "Completed" }
    val failedCount: Int = entries.count { it.status == "Failed" }
    val waitingPromptCount: Int = incomingPrompts.count { it.waitingForDecision }
    val hint: String = DesktopTransferFormatters.formatTransferHint(activeCount, entries.isNotEmpty())
    val heroTitle: String = when {
        activeCount > 0 -> DesktopTransferFormatters.formatActiveTransferSummary(activeCount)
        waitingPromptCount > 0 -> "$waitingPromptCount incoming file${if (waitingPromptCount == 1) "" else "s"} waiting"
        entries.isNotEmpty() -> "Transfers are idle"
        else -> "Ready when you need to send a file"
    }
    val heroSubtitle: String = when {
        activeCount > 0 -> "Keep this panel open to watch encrypted file progress."
        waitingPromptCount > 0 -> "Review incoming files from known online peers before saving."
        entries.isNotEmpty() -> "Recent completed or failed transfers remain below for troubleshooting."
        else -> "Choose an online discovered peer, pick a file, and enter the shared file password."
    }
    val activeSummary: String = DesktopTransferFormatters.formatActiveTransferSummary(activeCount)
    val entryRows: List<String> = entries.map(DesktopTransferFormatters::formatTransferListMeta)
    val recentEntries: List<TransferEntry> = entries.takeLast(4)
    val recentEntryRows: List<ComposeTransferRow> = recentEntries.map(ComposeTransferRow::from)
    val promptSummary: String = if (incomingPrompts.isEmpty()) {
        "No incoming receive prompts."
    } else {
        val waitingCount = incomingPrompts.count { it.waitingForDecision }
        "Incoming files: ${incomingPrompts.size}; $waitingCount waiting for review."
    }
    val receiveModeLabel: String = if (autoAcceptFiles) {
        "Auto-accept is on for known online peers"
    } else {
        "Ask before saving incoming files"
    }
    val receiveModeDescription: String = if (autoAcceptFiles) {
        "Known online peers can send files without an extra prompt. Unknown or offline senders are still rejected."
    } else {
        "Incoming files from known online peers require your confirmation before they are saved."
    }
    val receiveModeShortLabel: String = if (autoAcceptFiles) "Known peers auto-save" else "Ask before saving"
    val transferCountSummary: String = buildList {
        add("$activeCount active")
        add("$completedCount completed")
        if (failedCount > 0) add("$failedCount failed")
        if (waitingPromptCount > 0) add("$waitingPromptCount needs review")
    }.joinToString(" · ")
    val targetSummary: String = if (sendTargetReady) {
        if (selectedPeer?.discovered == true) {
            "Sending to $selectedPeerName over its discovered LAN file endpoint."
        } else {
            "Sending to $selectedPeerName over the file receiver inferred from its chat connection."
        }
    } else {
        selectedPeer?.let { peer ->
            when {
                !statusState.clientConnected -> "Connect to chat before sending to ${peer.nickname}."
                !peer.online -> "${peer.nickname} is offline. Wait until the peer is online."
                !peer.fileCapable -> "${peer.nickname} is online, but no file receiver endpoint is available."
                else -> "Select an online peer before sending."
            }
        } ?: "Select an online peer from the Peers list."
    }
    val selectedFileName: String = selectedFilePath.trim()
        .takeIf(String::isNotEmpty)
        ?.let { runCatching { Paths.get(it).fileName?.toString() ?: it }.getOrDefault(it) }
        ?: "No file selected"
    val selectedFileSummary: String = if (hasSelectedFile) selectedFileName else "Choose a local file before sending."
    val passwordSummary: String = if (passwordReady) "Using the current room password" else "Reconnect with a room password before sending files."
    val senderSummary: String = if (senderReady) "Sending as ${senderId.trim()}" else "Reconnect with your name before sending files."
    val sendLabel: String = if (canSendSelectedFile) "Send file ready" else "Send file blocked"
    val receiveLabel: String = if (listenerReady) "Receive listener ready" else "Receive listener blocked"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX transfer workspace remains production fallback" else "JavaFX transfer workspace fallback unavailable"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) {
            add("Connect to chat before sending encrypted files.")
        }
        if (selectedPeer == null) {
            add("Select an online peer before sending files.")
        } else if (!selectedPeer.online) {
            add("Selected peer is offline; wait for discovery or chat presence refresh.")
        } else if (!selectedPeer.fileCapable) {
            add("Selected peer does not have an available file receiver endpoint.")
        }
        if (!hasSelectedFile) {
            add("Choose a local file before sending.")
        }
        if (!senderReady) {
            add("Enter a sender name before sending files.")
        }
        if (!passwordReady) {
            add("Enter the file-transfer session password before sending files.")
        }
        if (!listenerReady) {
            add("Configure a valid local file-transfer listener port.")
        }
        if (!javaFxFallbackAvailable) {
            add("JavaFX fallback is unavailable; keep live Compose file-transfer actions disabled.")
        }
    }
    val nextStepSummary: String = blockedReasons.firstOrNull() ?: "Ready to send encrypted file to $selectedPeerName."
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Encrypted file-transfer send/receive controls are ready for Compose wiring."
    } else {
        blockedReasons.joinToString(" · ")
    }
}

data class ComposeTransferRow(
    val fileName: String,
    val directionLabel: String,
    val status: String,
    val percent: Int,
    val sizeLabel: String?,
    val speedLabel: String?,
    val active: Boolean,
    val failed: Boolean,
) {
    val title: String = "$directionLabel · $fileName"
    val progressLabel: String = when {
        active -> "$status · $percent%"
        status == "Completed" -> "Completed · 100%"
        failed -> "Failed"
        percent > 0 -> "$status · $percent%"
        else -> status
    }
    val detail: String = listOfNotNull(sizeLabel, speedLabel).joinToString(" · ").ifBlank { "Size not reported yet" }

    companion object {
        fun from(entry: TransferEntry): ComposeTransferRow = ComposeTransferRow(
            fileName = entry.fileName,
            directionLabel = entry.directionLabel(),
            status = entry.status,
            percent = entry.percent.coerceIn(0, 100),
            sizeLabel = entry.totalBytes.takeIf { it > 0 }?.let(DesktopTransferFormatters::formatMegabytes),
            speedLabel = entry.speedBytesPerSecond.takeIf { entry.active() && it > 0 }?.let(DesktopTransferFormatters::formatTransferSpeed),
            active = entry.active(),
            failed = entry.status == "Failed",
        )
    }
}

data class ComposeQuickShareState(
    val running: Boolean = false,
    val portText: String = NetworkConstants.DEFAULT_QUICK_SHARE_PORT.toString(),
    val selectedFilePath: String = "",
    val textDraft: String = "SecureLanSuite quick-share text",
    val expirationMinutesText: String = "10",
    val accessLimitText: String = "3",
    val entries: List<QuickShareEntry> = emptyList(),
    val landingUrls: List<String> = emptyList(),
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Share by browser link"
    val subtitle: String = "Create temporary file or text links that people on this trusted LAN can open in a browser."
    val port: Int? = portText.trim().toIntOrNull()?.takeIf { it in 1..65_535 }
    val expirationMinutes: Long? = expirationMinutesText.trim().toLongOrNull()?.takeIf { it >= 1 }
    val accessLimit: Int? = accessLimitText.trim().toIntOrNull()?.takeIf { it >= 1 }
    val hasSelectedFile: Boolean = selectedFilePath.trim().isNotEmpty()
    val hasText: Boolean = textDraft.trim().isNotEmpty()
    val activeEntries: List<QuickShareEntry> = entries.filter { it.active() }
    val inactiveEntries: List<QuickShareEntry> = entries.filterNot { it.active() }
    val statusText: String = if (running && port != null) DesktopQuickShareFormatters.formatServerStatus(port) else "Share server is stopped"
    val statusDetail: String = if (running) {
        "Links are available until their time or access limit is reached."
    } else {
        "Start the share server first, then create a file or text link."
    }
    val landingText: String = DesktopQuickShareFormatters.formatLandingValue(landingUrls)
    val trustedLanWarning: String =
        "Trusted LAN only. These links have no login, so stop shares when everyone has downloaded them."
    val canStartServer: Boolean = !running && port != null && javaFxFallbackAvailable
    val canStopServer: Boolean = running && javaFxFallbackAvailable
    val canCreateFileShare: Boolean = hasSelectedFile && expirationMinutes != null && accessLimit != null && javaFxFallbackAvailable
    val canCreateTextShare: Boolean = hasText && expirationMinutes != null && accessLimit != null && javaFxFallbackAvailable
    val canCopyIndex: Boolean = running && landingUrls.isNotEmpty() && javaFxFallbackAvailable
    val shareRows: List<String> = entries.map { DesktopQuickShareFormatters.formatSnapshotMeta(it.snapshot()) }
    val activeShareCountLabel: String = when (activeEntries.size) {
        0 -> "No active links"
        1 -> "1 active link"
        else -> "${activeEntries.size} active links"
    }
    val inactiveShareCountLabel: String = when (inactiveEntries.size) {
        0 -> "No stopped or expired links"
        1 -> "1 stopped or expired link"
        else -> "${inactiveEntries.size} stopped or expired links"
    }
    val serverActionLabel: String = if (running) "Server running" else "Start share server"
    val fileShareActionLabel: String = if (hasSelectedFile) "Create file link" else "Choose a file first"
    val textShareActionLabel: String = if (hasText) "Create text link" else "Enter text first"
    val selectedFileLabel: String = selectedFilePath.trim().ifBlank { "No file selected" }
    val policySummary: String = buildList {
        add(expirationMinutes?.let { "expires after $it min" } ?: "set expiration")
        add(accessLimit?.let { "$it opens max" } ?: "set access limit")
        add(port?.let { "port $it" } ?: "fix port")
    }.joinToString(" · ")
    val readinessSummary: String = buildList {
        if (port == null) add("Enter a valid port from 1 to 65535.")
        if (expirationMinutes == null) add("Set expiration to at least 1 minute.")
        if (accessLimit == null) add("Set access limit to at least 1 open.")
        if (!hasSelectedFile && !hasText) add("Choose a file or enter text to create a link.")
        if (!javaFxFallbackAvailable) add("JavaFX fallback is unavailable; live quick-share actions stay disabled.")
    }.ifEmpty {
        listOf("Ready to create trusted-LAN browser links.")
    }.joinToString(" · ")
    val emptySharesTitle: String = if (running) "No links created yet" else "Share server is idle"
    val emptySharesDetail: String = if (running) {
        "Choose a file or enter text, then create a browser link. New links are copied after creation."
    } else {
        "Start the server to create temporary browser links for this LAN."
    }
    val quickStartSteps: List<String> = listOf(
        "1. Start the share server.",
        "2. Choose a file or type text.",
        "3. Create a link and send it to trusted LAN peers.",
    )
    val shareRowsDetailed: List<ComposeQuickShareRow> = entries.map(ComposeQuickShareRow::from)
}

data class ComposeQuickShareRow(
    val id: String,
    val title: String,
    val typeLabel: String,
    val statusLabel: String,
    val detail: String,
    val url: String,
    val active: Boolean,
) {
    companion object {
        fun from(entry: QuickShareEntry): ComposeQuickShareRow {
            val snapshot = entry.snapshot()
            val typeLabel = when (snapshot.type()) {
                QuickShareType.FILE -> "File link"
                QuickShareType.TEXT -> "Text link"
            }
            val statusLabel = when (snapshot.status()) {
                QuickShareStatus.ACTIVE -> "Active"
                QuickShareStatus.STOPPED -> "Stopped"
                QuickShareStatus.EXPIRED -> "Expired"
                QuickShareStatus.LIMIT_REACHED -> "Limit reached"
            }
            val title = snapshot.displayName().ifBlank { snapshot.fileName() }.ifBlank { snapshot.id() }
            val sizeLabel = snapshot.fileSize().takeIf { it > 0 }?.let(DesktopTransferFormatters::formatMegabytes)
            val detail = buildList {
                add("${snapshot.accessCount()}/${snapshot.accessLimit()} opens")
                add("expires ${snapshot.expiresAt()}")
                if (sizeLabel != null) add(sizeLabel)
            }.joinToString(" · ")
            return ComposeQuickShareRow(
                id = entry.id(),
                title = title,
                typeLabel = typeLabel,
                statusLabel = statusLabel,
                detail = detail,
                url = entry.url(),
                active = entry.active(),
            )
        }
    }
}

data class ComposeSteganographyState(
    val coverPathText: String = "",
    val inputPathText: String = "",
    val outputPathText: String = "",
    val messageDraft: String = "Hidden Compose message",
    val passwordDraft: String = "",
    val encryptPayload: Boolean = false,
    val encryptedExtract: Boolean = false,
    val capacity: BmpCapacity? = null,
    val extractedMessage: String = "",
    val statusText: String = "Choose a BMP cover or input image to start.",
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Steganography"
    val coverPath = coverPathText.trim()
    val inputPath = inputPathText.trim()
    val outputPath = outputPathText.trim()
    val hasCover: Boolean = coverPath.isNotEmpty()
    val hasInput: Boolean = inputPath.isNotEmpty()
    val hasOutput: Boolean = outputPath.isNotEmpty()
    val hasMessage: Boolean = messageDraft.trim().isNotEmpty()
    val passwordRequiredForHide: Boolean = encryptPayload
    val passwordRequiredForExtract: Boolean = encryptedExtract
    val passwordReady: Boolean = passwordDraft.isNotEmpty()
    val capacityText: String = capacity?.let(DesktopMainViewHelpers::formatStegoCapacity) ?: "Capacity unavailable until a BMP is inspected."
    val canInspectCover: Boolean = hasCover && javaFxFallbackAvailable
    val canHideMessage: Boolean = hasCover && hasOutput && hasMessage && (!passwordRequiredForHide || passwordReady) && javaFxFallbackAvailable
    val canExtractMessage: Boolean = hasInput && (!passwordRequiredForExtract || passwordReady) && javaFxFallbackAvailable
    val hideLabel: String = if (canHideMessage) "Hide message ready" else "Hide message blocked"
    val extractLabel: String = if (canExtractMessage) "Extract ready" else "Extract blocked"
    val extractedSummary: String = if (extractedMessage.isBlank()) "No extracted message yet." else "Extracted ${extractedMessage.length} characters."
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX steganography workspace remains production fallback" else "JavaFX steganography fallback unavailable"
    val blockedReasons: List<String> = buildList {
        if (!hasCover) add("Choose a cover image before hiding or inspecting capacity.")
        if (!hasOutput) add("Choose an output BMP path before hiding a message.")
        if (!hasMessage) add("Enter non-empty text before hiding a message.")
        if (!hasInput) add("Choose an input BMP before extracting a message.")
        if ((passwordRequiredForHide || passwordRequiredForExtract) && !passwordReady) add("Enter a password for encrypted steganography workflows.")
        if (!javaFxFallbackAvailable) add("JavaFX fallback is unavailable; keep live Compose steganography actions disabled.")
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Steganography controls are ready for BMP hide/extract workflows."
    } else {
        blockedReasons.joinToString(" · ")
    }
}

data class ComposeMediaVoiceState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val microphones: List<MediaDeviceChoice> = listOf(MediaDeviceChoice.systemDefault("System default microphone")),
    val outputDevices: List<MediaDeviceChoice> = listOf(MediaDeviceChoice.systemDefault("System default speaker")),
    val selectedMicrophoneId: String = "",
    val selectedOutputDeviceId: String = "",
    val runtimeStatus: RtcRuntimeStatus? = null,
    val currentSession: RtcSessionSnapshot? = null,
    val localAudioLevel: Double = 0.0,
    val remoteAudioLevel: Double = 0.0,
    val microphoneTestStatus: String = "Not tested",
    val speakerTestStatus: String = "Not tested",
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Media devices and voice"
    val selectedPeer = peerListState.selectedPeer
    val selectedPeerName: String = selectedPeer?.nickname ?: "No peer selected"
    val selectedMicrophone: MediaDeviceChoice = microphones.firstOrNull { it.matches(selectedMicrophoneId) }
        ?: microphones.firstOrNull()
        ?: MediaDeviceChoice.systemDefault("System default microphone")
    val selectedOutputDevice: MediaDeviceChoice = outputDevices.firstOrNull { it.matches(selectedOutputDeviceId) }
        ?: outputDevices.firstOrNull()
        ?: MediaDeviceChoice.systemDefault("System default speaker")
    val physicalMicrophoneCount: Int = microphones.count { !it.systemDefault }
    val physicalOutputDeviceCount: Int = outputDevices.count { !it.systemDefault }
    val runtimeLabel: String = DesktopRealtimeFormatters.formatRuntimeStatus(runtimeStatus)
    val permissionStatusLabel: String = when {
        runtimeStatus?.available == true -> "Microphone permission: ready to test"
        runtimeStatus == null -> "Microphone permission: not checked yet"
        else -> "Microphone permission: unavailable"
    }
    val microphoneEmptyState: String = if (physicalMicrophoneCount == 0) {
        "No microphones found. Connect a microphone, allow OS/browser access, then refresh devices."
    } else {
        "$physicalMicrophoneCount microphone option${if (physicalMicrophoneCount == 1) "" else "s"} available."
    }
    val outputEmptyState: String = if (physicalOutputDeviceCount == 0) {
        "No speaker list is available. Calls and tests will use the system default output."
    } else {
        "$physicalOutputDeviceCount speaker option${if (physicalOutputDeviceCount == 1) "" else "s"} available."
    }
    val voiceState: RtcSessionState = currentSession?.state ?: RtcSessionState.IDLE
    val voiceStatusText: String = DesktopRealtimeFormatters.voiceStatusText(voiceState, currentSession?.remotePeer ?: selectedPeer?.nickname)
    val localAudioLabel: String = DesktopRealtimeFormatters.formatAudioLevel(localAudioLevel > 0.01, true, selectedPeer?.nickname, localAudioLevel)
    val remoteAudioLabel: String = DesktopRealtimeFormatters.formatAudioLevel(remoteAudioLevel > 0.01, false, selectedPeer?.nickname, remoteAudioLevel)
    val localAudioPercent: Int = (localAudioLevel.coerceIn(0.0, 1.0) * 100).toInt()
    val voiceTargetReady: Boolean = statusState.clientConnected && selectedPeer?.online == true && selectedPeer.voiceCapable
    val canRefreshDevices: Boolean = javaFxFallbackAvailable
    val canTestMicrophone: Boolean = microphones.isNotEmpty() && javaFxFallbackAvailable
    val canTestSpeaker: Boolean = outputDevices.isNotEmpty() && javaFxFallbackAvailable
    val canStartVoice: Boolean = voiceTargetReady && javaFxFallbackAvailable
    val canHangUp: Boolean = DesktopMainViewHelpers.hangUpAvailable(currentSession?.state) && javaFxFallbackAvailable
    val startVoiceLabel: String = if (canStartVoice) "Start voice call" else "Voice call blocked"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX voice workspace remains production fallback" else "JavaFX voice fallback unavailable"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) add("Connect to chat before starting a voice call.")
        if (selectedPeer == null) add("Select an online peer before starting voice.") else if (!selectedPeer.online) add("Selected peer is offline; voice must remain blocked.") else if (!selectedPeer.voiceCapable) add("Selected peer does not advertise voice support; voice must remain blocked.")
        if (!javaFxFallbackAvailable) add("JavaFX fallback is unavailable; keep live Compose voice actions disabled.")
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Voice controls are ready for WebRTC session wiring."
    } else {
        blockedReasons.joinToString(" · ")
    }
}

data class ComposeExperimentalVideoState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val cameras: List<MediaDeviceChoice> = listOf(MediaDeviceChoice.systemDefault("System default camera")),
    val selectedCameraId: String = "",
    val runtimeStatus: RtcRuntimeStatus? = null,
    val currentSession: RtcSessionSnapshot? = null,
    val localPreviewEnabled: Boolean = true,
    val remotePreviewEnabled: Boolean = true,
    val previewRunning: Boolean = false,
    val latestPreviewFrame: RtcVideoFrameEvent? = null,
    val cameraTestStatus: String = "Not tested",
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Experimental camera and video"
    val selectedPeer = peerListState.selectedPeer
    val selectedPeerName: String = selectedPeer?.nickname ?: "No peer selected"
    val selectedCamera: MediaDeviceChoice = cameras.firstOrNull { it.matches(selectedCameraId) }
        ?: cameras.firstOrNull()
        ?: MediaDeviceChoice.systemDefault("System default camera")
    val physicalCameraCount: Int = cameras.count { !it.systemDefault }
    val runtimeLabel: String = DesktopRealtimeFormatters.formatRuntimeStatus(runtimeStatus)
    val permissionStatusLabel: String = when {
        runtimeStatus?.available == true -> "Camera permission: ready to test"
        runtimeStatus == null -> "Camera permission: not checked yet"
        else -> "Camera permission: unavailable"
    }
    val cameraEmptyState: String = if (physicalCameraCount == 0) {
        "No cameras found. Connect a camera, allow OS/browser access, then refresh devices."
    } else {
        "$physicalCameraCount camera option${if (physicalCameraCount == 1) "" else "s"} available."
    }
    val sessionMode: RtcSessionMode = currentSession?.mode ?: RtcSessionMode.AUDIO_VIDEO
    val sessionState: RtcSessionState = currentSession?.state ?: RtcSessionState.IDLE
    val stageTitle: String = DesktopRealtimeFormatters.videoStageTitle(sessionMode, currentSession?.remotePeer ?: selectedPeer?.nickname)
    val stageBadge: String = DesktopRealtimeFormatters.videoStageBadge(sessionState)
    val mediaLabel: String = DesktopRealtimeFormatters.videoMediaLabel(sessionMode)
    val previewStatus: String = latestPreviewFrame?.let { frame -> DesktopRealtimeFormatters.cameraPreviewLiveStatus(frame.width(), frame.height()) }
        ?: if (previewRunning) "Camera preview starting…" else "Camera preview idle"
    val frameCaption: String = latestPreviewFrame?.let { frame ->
        DesktopRealtimeFormatters.videoFrameCaption(frame.local(), frame.peer(), frame.width(), frame.height())
    } ?: "No preview frames yet."
    val previewStateLabel: String = when {
        cameraTestStatus.startsWith("Camera preview failed", ignoreCase = true) -> "Preview failed"
        latestPreviewFrame != null -> "Preview is live"
        previewRunning -> "Preview is starting"
        else -> "Preview is off"
    }
    val previewActionHint: String = when {
        cameraTestStatus.startsWith("Camera preview failed", ignoreCase = true) -> "Preview could not start. Check the message below, close other camera apps, then try again."
        previewRunning && latestPreviewFrame == null -> "Waiting for the first camera frame. This can take a few seconds after granting access."
        previewRunning -> "Preview is running. Use Stop preview before switching cameras."
        physicalCameraCount == 0 -> "No camera has been confirmed yet. Refresh devices or try the system default camera."
        else -> "Choose a camera, then start preview to check image and lighting."
    }
    val videoTargetReady: Boolean = statusState.clientConnected && selectedPeer?.online == true && selectedPeer.videoCapable
    val canRefreshCameras: Boolean = javaFxFallbackAvailable
    val canTestCamera: Boolean = cameras.isNotEmpty() && javaFxFallbackAvailable
    val canStartPreview: Boolean = cameras.isNotEmpty() && !previewRunning && javaFxFallbackAvailable
    val canStopPreview: Boolean = previewRunning && javaFxFallbackAvailable
    val canStartVideo: Boolean = videoTargetReady && javaFxFallbackAvailable
    val canHangUp: Boolean = DesktopMainViewHelpers.hangUpAvailable(currentSession?.state) && javaFxFallbackAvailable
    val startVideoLabel: String = if (canStartVideo) "Start video call" else "Video call blocked"
    val startPreviewLabel: String = if (canStartPreview) "Start camera preview" else "Preview unavailable"
    val stopPreviewLabel: String = if (canStopPreview) "Stop camera preview" else "Stop preview unavailable"
    val previewConfigurationLabel: String = "Self preview ${if (localPreviewEnabled) "on" else "off"} • remote preview ${if (remotePreviewEnabled) "on" else "off"}"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX experimental video workspace remains production fallback" else "JavaFX video fallback unavailable"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) add("Connect to chat before starting experimental video.")
        if (selectedPeer == null) add("Select an online peer before starting video.") else if (!selectedPeer.online) add("Selected peer is offline; video must remain blocked.") else if (!selectedPeer.videoCapable) add("Selected peer does not advertise video support; video must remain blocked.")
        if (!javaFxFallbackAvailable) add("JavaFX fallback is unavailable; keep live Compose video actions disabled.")
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Experimental video controls are ready for camera preview and call wiring."
    } else {
        blockedReasons.joinToString(" · ")
    }
}

data class ComposeDiagnosticsState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val chatDiagnostics: List<String> = emptyList(),
    val fileTransferDiagnostics: List<String> = emptyList(),
    val quickShareDiagnostics: List<String> = emptyList(),
    val realtimeDiagnostics: List<String> = emptyList(),
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Runtime diagnostics"
    val fallbackStatus: String =
        if (javaFxFallbackAvailable) "JavaFX fallback is available" else "JavaFX fallback is unavailable"
    val statusAdapterSummary: String = statusState.validationSummary
    val connectionActionSummary: String = statusState.actionState.diagnosticSummary
    val selectedPeerSummary: String = "Selected peer: ${peerListState.selectedPeerTitle} · ${peerListState.peerStatus}"
    val visiblePeerSummary: String = "Visible peers: ${peerListState.visiblePeers.size}"
    val alerts: List<ComposeDiagnosticAlert> = buildList {
        if (!javaFxFallbackAvailable) {
            add(ComposeDiagnosticAlert(ComposeDiagnosticAlertKind.ERROR, "Fallback unavailable", "Restore the JavaFX fallback before changing runtime or packaging settings."))
        }
        if (!statusState.nicknameValid) {
            add(ComposeDiagnosticAlert(ComposeDiagnosticAlertKind.WARNING, "Profile name required", "Enter your name before opening a room or connecting manually."))
        }
        if (statusState.serverChatPort == null || statusState.serverFilePort == null) {
            add(ComposeDiagnosticAlert(ComposeDiagnosticAlertKind.WARNING, "Room ports need attention", "Use valid chat and file-transfer ports before hosting a room."))
        }
        if (!statusState.manualHostValid || statusState.clientChatPort == null || statusState.clientFilePort == null) {
            add(ComposeDiagnosticAlert(ComposeDiagnosticAlertKind.WARNING, "Manual connection incomplete", "Enter a host address and valid ports before connecting manually."))
        }
        if (peerListState.selectedPeer == null) {
            add(ComposeDiagnosticAlert(ComposeDiagnosticAlertKind.INFO, "No peer selected", "Select an online peer to enable file, voice, video, and realtime actions."))
        } else if (peerListState.selectedPeer?.online != true) {
            add(ComposeDiagnosticAlert(ComposeDiagnosticAlertKind.WARNING, "Selected peer is offline", "Choose an online peer before starting peer-targeted actions."))
        }
    }
    val warningMessages: List<String> = alerts.map(ComposeDiagnosticAlert::summary)
    val warningSummary: String = if (warningMessages.isEmpty()) {
        "No runtime alerts. Diagnostics will update as chat, transfer, quick-share, and realtime events arrive."
    } else {
        alerts.joinToString(" · ") { it.summary }
    }
    val hasErrors: Boolean = alerts.any { it.kind == ComposeDiagnosticAlertKind.ERROR }
    val hasWarnings: Boolean = alerts.any { it.kind == ComposeDiagnosticAlertKind.WARNING }
    val statusLabel: String = when {
        hasErrors -> "Needs attention"
        hasWarnings -> "Check settings"
        alerts.isNotEmpty() -> "Ready for activity"
        else -> "Healthy"
    }
    val diagnosticChannels: List<String> = listOf(
        "chat=${chatDiagnostics.size}",
        "file-transfer=${fileTransferDiagnostics.size}",
        "quick-share=${quickShareDiagnostics.size}",
        "realtime=${realtimeDiagnostics.size}",
    )
    val diagnosticChannelSummary: String = "Diagnostic channels: ${diagnosticChannels.joinToString(" · ")}"
    val channelCards: List<ComposeDiagnosticChannel> = listOf(
        ComposeDiagnosticChannel(
            kind = ComposeDiagnosticChannelKind.CHAT,
            title = "Chat",
            description = "Room connection, messages, joins, leaves, and chat errors.",
            messages = chatDiagnostics,
            emptyState = "Chat events will appear after you open or join a room.",
        ),
        ComposeDiagnosticChannel(
            kind = ComposeDiagnosticChannelKind.FILE_TRANSFER,
            title = "File transfer",
            description = "Encrypted send/receive progress, confirmations, and failures.",
            messages = fileTransferDiagnostics,
            emptyState = "File-transfer activity will appear when a listener starts or a file is sent.",
        ),
        ComposeDiagnosticChannel(
            kind = ComposeDiagnosticChannelKind.QUICK_SHARE,
            title = "Quick share",
            description = "Trusted-LAN browser links, server status, and share access events.",
            messages = quickShareDiagnostics,
            emptyState = "Quick-share events will appear after the local sharing server starts.",
        ),
        ComposeDiagnosticChannel(
            kind = ComposeDiagnosticChannelKind.REALTIME,
            title = "Realtime media",
            description = "Voice, camera, device, and RTC session diagnostics.",
            messages = realtimeDiagnostics,
            emptyState = "Realtime diagnostics will appear after refreshing devices or starting a call.",
        ),
    )
    val activeChannelCount: Int = channelCards.count { it.hasMessages }
    val totalDiagnosticMessages: Int = channelCards.sumOf { it.messageCount }
    val runtimeOverview: String = when {
        totalDiagnosticMessages == 0 -> "No runtime events yet. Start a room, send a file, share a link, or refresh media devices to populate diagnostics."
        hasErrors -> "Runtime events are available, but one or more settings need attention."
        hasWarnings -> "Runtime events are available. Review the highlighted setup notes before starting peer actions."
        else -> "Runtime diagnostics are up to date across active channels."
    }
    val recentMessages: List<String> = channelCards.flatMap { channel -> channel.recentMessages.map { "${channel.title}: $it" } }.takeLast(6)
    val summaryLines: List<String> = listOf(
        title,
        statusLabel,
        runtimeOverview,
        fallbackStatus,
        statusAdapterSummary,
        connectionActionSummary,
        selectedPeerSummary,
        visiblePeerSummary,
        diagnosticChannelSummary,
        warningSummary,
    )
}

enum class ComposeDiagnosticChannelKind {
    CHAT,
    FILE_TRANSFER,
    QUICK_SHARE,
    REALTIME,
}

data class ComposeDiagnosticChannel(
    val kind: ComposeDiagnosticChannelKind,
    val title: String,
    val description: String,
    val messages: List<String>,
    val emptyState: String,
) {
    val messageCount: Int = messages.size
    val hasMessages: Boolean = messages.isNotEmpty()
    val recentMessages: List<String> = messages.takeLast(3)
    val stateLabel: String = if (hasMessages) "$messageCount event${if (messageCount == 1) "" else "s"}" else "Waiting"
    val latestMessage: String = recentMessages.lastOrNull() ?: emptyState
}

enum class ComposeDiagnosticAlertKind {
    INFO,
    WARNING,
    ERROR,
}

data class ComposeDiagnosticAlert(
    val kind: ComposeDiagnosticAlertKind,
    val title: String,
    val message: String,
) {
    val summary: String = "$title: $message"
}

enum class ComposeRegressionGateKind {
    STATUS_CONNECTION,
    PEER_TARGETS,
    CHAT_INTEROP,
    FILE_TRANSFER,
    QUICK_SHARE,
    STEGANOGRAPHY,
    MEDIA_VOICE,
    EXPERIMENTAL_VIDEO,
    DIAGNOSTICS,
}

data class ComposeRegressionGate(
    val kind: ComposeRegressionGateKind,
    val label: String,
    val ready: Boolean,
    val evidence: String,
    val blocker: String,
) {
    val displayLabel: String = if (ready) "$label ready" else "$label blocked"
}

enum class ComposeRuntimeEvidenceKind {
    CHAT_INTEROP,
    FILE_TRANSFER,
    QUICK_SHARE,
    STEGANOGRAPHY,
    VOICE,
    EXPERIMENTAL_VIDEO,
    FULL_REGRESSION,
}

enum class ComposeRuntimeEvidenceChecklistStatus {
    PENDING,
    RECORDED,
    ACCEPTED,
}

data class ComposeRuntimeEvidenceRecord(
    val kind: ComposeRuntimeEvidenceKind,
    val status: ComposeRuntimeEvidenceChecklistStatus,
    val note: String,
    val recordedAt: Instant,
) {
    val accepted: Boolean = status == ComposeRuntimeEvidenceChecklistStatus.ACCEPTED
    val recorded: Boolean = status != ComposeRuntimeEvidenceChecklistStatus.PENDING
    val copyLine: String = "${kind.name.lowercase().replace('_', '-')}=${status.name.lowercase()} @ $recordedAt — $note"
}

data class ComposeRuntimeEvidenceRequirement(
    val kind: ComposeRuntimeEvidenceKind,
    val label: String,
    val recorded: Boolean,
    val validationScope: String,
    val evidence: String,
    val blocker: String,
) {
    val displayLabel: String = if (recorded) "$label evidence recorded" else "$label evidence missing"
    val statusText: String = if (recorded) evidence else blocker
}

data class ComposeRuntimeRegressionChecklistItem(
    val order: Int,
    val requirementKind: ComposeRuntimeEvidenceKind,
    val label: String,
    val recorded: Boolean,
    val action: String,
) {
    val displayText: String = "$order. $label — ${if (recorded) "recorded" else "pending"}: $action"
}

data class ComposeRegressionReadinessState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val chatState: ComposeChatWorkspaceState,
    val fileTransferState: ComposeFileTransferState,
    val quickShareState: ComposeQuickShareState,
    val steganographyState: ComposeSteganographyState,
    val mediaVoiceState: ComposeMediaVoiceState,
    val experimentalVideoState: ComposeExperimentalVideoState,
    val diagnosticsState: ComposeDiagnosticsState = ComposeDiagnosticsState(
        statusState = statusState,
        peerListState = peerListState,
        chatDiagnostics = chatState.transcriptLines,
        fileTransferDiagnostics = fileTransferState.entryRows,
        quickShareDiagnostics = quickShareState.shareRows,
        realtimeDiagnostics = listOf(mediaVoiceState.runtimeLabel, experimentalVideoState.runtimeLabel),
    ),
    val chatRuntimeValidated: Boolean = false,
    val fileTransferRuntimeValidated: Boolean = false,
    val quickShareRuntimeValidated: Boolean = false,
    val steganographyRuntimeValidated: Boolean = false,
    val voiceRuntimeValidated: Boolean = false,
    val videoRuntimeValidated: Boolean = false,
    val fullRuntimeRegressionValidated: Boolean = false,
    val javaFxFallbackAvailable: Boolean = true,
    val runtimeEvidenceRecords: List<ComposeRuntimeEvidenceRecord> = emptyList(),
) {
    val title: String = "Compose regression readiness"
    val gates: List<ComposeRegressionGate> = buildList {
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.STATUS_CONNECTION,
                label = "Status/connection",
                ready = javaFxFallbackAvailable && statusState.canOpenRoom && statusState.canConnect,
                evidence = statusState.validationSummary,
                blocker = "Status/connection inputs or JavaFX fallback are not ready.",
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.PEER_TARGETS,
                label = "Peer targets",
                ready = javaFxFallbackAvailable && peerListState.targetActions.chatReady,
                evidence = peerListState.targetActions.summary,
                blocker = peerListState.targetActions.blockedSummary,
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.CHAT_INTEROP,
                label = "Chat interoperability",
                ready = javaFxFallbackAvailable && chatRuntimeValidated,
                evidence = chatState.readinessSummary,
                blocker = "Desktop-to-desktop and desktop-to-Android chat smoke validation is still pending.",
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.FILE_TRANSFER,
                label = "Encrypted file transfer",
                ready = javaFxFallbackAvailable && fileTransferRuntimeValidated,
                evidence = fileTransferState.readinessSummary,
                blocker = "Desktop/Android encrypted file-transfer runtime validation is still pending.",
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.QUICK_SHARE,
                label = "Quick share",
                ready = javaFxFallbackAvailable && quickShareRuntimeValidated,
                evidence = quickShareState.readinessSummary,
                blocker = "Remote LAN browser quick-share validation is still pending.",
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.STEGANOGRAPHY,
                label = "Steganography",
                ready = javaFxFallbackAvailable && steganographyRuntimeValidated,
                evidence = steganographyState.readinessSummary,
                blocker = "Manual BMP hide/extract validation is still pending.",
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.MEDIA_VOICE,
                label = "Media devices and voice",
                ready = javaFxFallbackAvailable && voiceRuntimeValidated,
                evidence = mediaVoiceState.readinessSummary,
                blocker = "Live voice start/stop validation is still pending.",
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.EXPERIMENTAL_VIDEO,
                label = "Experimental camera/video",
                ready = javaFxFallbackAvailable && videoRuntimeValidated,
                evidence = experimentalVideoState.readinessSummary,
                blocker = "Live camera preview and experimental-video validation is still pending.",
            ),
        )
        add(
            ComposeRegressionGate(
                kind = ComposeRegressionGateKind.DIAGNOSTICS,
                label = "Diagnostics",
                ready = javaFxFallbackAvailable && diagnosticsState.warningMessages.isEmpty() && fullRuntimeRegressionValidated,
                evidence = diagnosticsState.diagnosticChannelSummary,
                blocker = "Full runtime regression and diagnostic-channel evidence are still pending.",
            ),
        )
    }
    val readyGates: List<ComposeRegressionGate> = gates.filter { it.ready }
    val blockedGates: List<ComposeRegressionGate> = gates.filterNot { it.ready }
    val readyCount: Int = readyGates.size
    val blockedCount: Int = blockedGates.size
    val totalCount: Int = gates.size
    val allRuntimeValidated: Boolean = chatRuntimeValidated &&
        fileTransferRuntimeValidated &&
        quickShareRuntimeValidated &&
        steganographyRuntimeValidated &&
        voiceRuntimeValidated &&
        videoRuntimeValidated &&
        fullRuntimeRegressionValidated
    val runtimeEvidenceRequirements: List<ComposeRuntimeEvidenceRequirement> = listOf(
        ComposeRuntimeEvidenceRequirement(
            kind = ComposeRuntimeEvidenceKind.CHAT_INTEROP,
            label = "Chat interop",
            recorded = chatRuntimeValidated,
            validationScope = "desktop-to-desktop and desktop-to-Android chat smoke",
            evidence = "Chat runtime evidence accepted with transcript lines=${chatState.transcriptLines.size}.",
            blocker = "Run desktop-to-desktop and desktop-to-Android chat smoke checks.",
        ),
        ComposeRuntimeEvidenceRequirement(
            kind = ComposeRuntimeEvidenceKind.FILE_TRANSFER,
            label = "Encrypted file transfer",
            recorded = fileTransferRuntimeValidated,
            validationScope = "desktop-to-desktop, Android-to-desktop, and desktop-to-Android file transfer",
            evidence = "Encrypted transfer runtime evidence accepted with rows=${fileTransferState.entryRows.size} and prompts=${fileTransferState.incomingPrompts.size}.",
            blocker = "Validate encrypted file send/receive across desktop and Android peers.",
        ),
        ComposeRuntimeEvidenceRequirement(
            kind = ComposeRuntimeEvidenceKind.QUICK_SHARE,
            label = "Quick share",
            recorded = quickShareRuntimeValidated,
            validationScope = "remote LAN browser quick-share access",
            evidence = "Quick-share browser-link evidence accepted with rows=${quickShareState.shareRows.size}.",
            blocker = "Open a text and file quick-share link from another LAN browser.",
        ),
        ComposeRuntimeEvidenceRequirement(
            kind = ComposeRuntimeEvidenceKind.STEGANOGRAPHY,
            label = "Steganography",
            recorded = steganographyRuntimeValidated,
            validationScope = "manual BMP inspect, hide, encrypted hide, and extract smoke",
            evidence = "Steganography BMP workflow evidence accepted: ${steganographyState.extractedSummary}",
            blocker = "Run BMP inspect/hide/extract success and failure checks manually.",
        ),
        ComposeRuntimeEvidenceRequirement(
            kind = ComposeRuntimeEvidenceKind.VOICE,
            label = "Voice",
            recorded = voiceRuntimeValidated,
            validationScope = "live WebRTC voice start/stop with diagnostics",
            evidence = "Voice runtime evidence accepted: ${mediaVoiceState.voiceStatusText}",
            blocker = "Validate voice start/stop and audio-level diagnostics on LAN peers.",
        ),
        ComposeRuntimeEvidenceRequirement(
            kind = ComposeRuntimeEvidenceKind.EXPERIMENTAL_VIDEO,
            label = "Experimental camera/video",
            recorded = videoRuntimeValidated,
            validationScope = "camera preview and experimental 1-to-1 video smoke",
            evidence = "Experimental video evidence accepted: ${experimentalVideoState.previewStatus}",
            blocker = "Validate camera preview and experimental video while preserving voice fallback.",
        ),
        ComposeRuntimeEvidenceRequirement(
            kind = ComposeRuntimeEvidenceKind.FULL_REGRESSION,
            label = "Full Compose regression",
            recorded = fullRuntimeRegressionValidated,
            validationScope = "all accepted Compose flows plus diagnostics review",
            evidence = "Full Compose runtime regression evidence accepted: ${diagnosticsState.diagnosticChannelSummary}",
            blocker = "Run the complete Compose runtime regression before packaging validation.",
        ),
    )
    val recordedRuntimeEvidence: List<ComposeRuntimeEvidenceRequirement> = runtimeEvidenceRequirements.filter { it.recorded }
    val missingRuntimeEvidence: List<ComposeRuntimeEvidenceRequirement> = runtimeEvidenceRequirements.filterNot { it.recorded }
    val acceptedRuntimeEvidenceRecords: List<ComposeRuntimeEvidenceRecord> = runtimeEvidenceRecords.filter(ComposeRuntimeEvidenceRecord::accepted)
    val pendingRuntimeEvidenceRecords: List<ComposeRuntimeEvidenceRecord> = runtimeEvidenceRecords.filterNot(ComposeRuntimeEvidenceRecord::accepted)
    val runtimeEvidenceRecordSummary: String = if (runtimeEvidenceRecords.isEmpty()) {
        "No runtime evidence records captured yet."
    } else {
        "${acceptedRuntimeEvidenceRecords.size} accepted runtime records; ${pendingRuntimeEvidenceRecords.size} pending review."
    }
    val runtimeEvidenceCopyText: String = runtimeEvidenceRecords.joinToString("\n") { it.copyLine }
    val runtimeEvidenceSummary: String =
        "${recordedRuntimeEvidence.size} of ${runtimeEvidenceRequirements.size} runtime evidence checks recorded; ${missingRuntimeEvidence.size} missing."
    val runtimeChecklistSummary: String = if (missingRuntimeEvidence.isEmpty()) {
        "All runtime evidence checks are recorded; proceed to packaging validation."
    } else {
        "Next runtime checks: ${missingRuntimeEvidence.joinToString { it.validationScope }}"
    }
    val runtimeRegressionChecklist: List<ComposeRuntimeRegressionChecklistItem> = runtimeEvidenceRequirements.mapIndexed { index, requirement ->
        ComposeRuntimeRegressionChecklistItem(
            order = index + 1,
            requirementKind = requirement.kind,
            label = requirement.label,
            recorded = requirement.recorded,
            action = if (requirement.recorded) requirement.evidence else requirement.blocker,
        )
    }
    val runtimeRegressionChecklistCopy: String = runtimeRegressionChecklist.joinToString("\n") { it.displayText }
    val acceptedRuntimeFlowSummary: String = if (recordedRuntimeEvidence.isEmpty()) {
        "No runtime evidence has been accepted yet."
    } else {
        "Accepted runtime evidence: ${recordedRuntimeEvidence.joinToString { it.label }}."
    }
    val pendingRuntimeFlowSummary: String = if (missingRuntimeEvidence.isEmpty()) {
        "No runtime evidence remains pending."
    } else {
        "Pending runtime evidence: ${missingRuntimeEvidence.joinToString { it.label }}."
    }
    val summary: String = "$readyCount of $totalCount Compose regression gates ready; $blockedCount blocked."
    val blockedSummary: String = if (blockedGates.isEmpty()) {
        "No Compose regression gates are blocked."
    } else {
        blockedGates.joinToString(" · ") { "${it.label}: ${it.blocker}" }
    }
    val nextActionSummary: String = when {
        !javaFxFallbackAvailable -> "Restore JavaFX fallback before accepting more Compose runtime slices."
        !chatRuntimeValidated -> "Run desktop-to-desktop and desktop-to-Android chat smoke validation next."
        !fileTransferRuntimeValidated -> "Run encrypted file-transfer runtime validation across desktop and Android next."
        !quickShareRuntimeValidated -> "Run remote LAN browser quick-share validation next."
        !steganographyRuntimeValidated -> "Run manual BMP steganography success/failure validation next."
        !voiceRuntimeValidated -> "Run live WebRTC voice validation next."
        !videoRuntimeValidated -> "Run camera preview and experimental-video validation next."
        !fullRuntimeRegressionValidated -> "Run full Compose runtime regression before packaging validation."
        else -> "All runtime gates are ready for packaging validation."
    }
}

enum class ComposePackagingGateKind {
    DESKTOP_TESTS,
    DESKTOP_BUILD,
    COMPOSE_RUNTIME_SMOKE,
    PORTABLE_ZIP,
    COMPOSE_PORTABLE_ZIP,
    WINDOWS_EXE,
    LAUNCHER_DECISION,
}

enum class ComposePackagingEvidenceKind {
    DESKTOP_TESTS,
    DESKTOP_BUILD,
    COMPOSE_RUNTIME_SMOKE,
    PORTABLE_ZIP,
    COMPOSE_PORTABLE_ZIP,
    WINDOWS_EXE,
    FULL_RUNTIME_REGRESSION,
    PROMOTION_APPROVAL,
}

data class ComposePackagingEvidenceRecord(
    val kind: ComposePackagingEvidenceKind,
    val validated: Boolean,
    val note: String,
    val recordedAt: Instant,
) {
    val copyLine: String = "${kind.name.lowercase().replace('_', '-')}=${if (validated) "validated" else "pending"} @ $recordedAt — $note"
}

data class ComposePackagingValidationReport(
    val title: String,
    val gateSummary: String,
    val artifactSummary: String,
    val releaseSummary: String,
    val rollbackSummary: String,
    val evidenceLines: List<String>,
) {
    val copyText: String = listOf(title, gateSummary, artifactSummary, releaseSummary, rollbackSummary)
        .plus(evidenceLines)
        .joinToString("\n")
}

data class ComposePackagingGate(
    val kind: ComposePackagingGateKind,
    val label: String,
    val ready: Boolean,
    val taskName: String,
    val evidence: String,
    val blocker: String,
) {
    val displayLabel: String = if (ready) "$label ready" else "$label blocked"
}

enum class ComposePackagingArtifactKind {
    PORTABLE_ZIP,
    COMPOSE_PORTABLE_ZIP,
    WINDOWS_EXE,
    JAVAFX_LAUNCHER,
    COMPOSE_ENTRYPOINT,
}

data class ComposePackagingArtifactRequirement(
    val kind: ComposePackagingArtifactKind,
    val label: String,
    val pathText: String,
    val validated: Boolean,
    val validationAction: String,
) {
    val displayText: String = "$label (${if (validated) "validated" else "pending"}) -> $pathText"
}

enum class ComposePromotionDecisionStepKind {
    PRESERVE_FALLBACK,
    COMPLETE_RUNTIME_REGRESSION,
    VALIDATE_PACKAGING,
    REQUIRE_APPROVAL,
    SEPARATE_PROMOTION_TASK,
}

data class ComposePromotionDecisionStep(
    val kind: ComposePromotionDecisionStepKind,
    val label: String,
    val satisfied: Boolean,
    val action: String,
) {
    val displayText: String = "${if (satisfied) "✓" else "•"} $label: $action"
}

enum class ComposeLauncherDecisionKind {
    KEEP_JAVAFX_FALLBACK,
    CONTINUE_VALIDATION,
    PROMOTE_COMPOSE_AFTER_ACCEPTANCE,
}

data class ComposeLauncherDecisionOption(
    val kind: ComposeLauncherDecisionKind,
    val label: String,
    val enabled: Boolean,
    val recommended: Boolean,
    val reason: String,
) {
    val displayLabel: String = if (recommended) "$label recommended" else label
}

data class ComposeLauncherDecisionState(
    val applicationMainClass: String,
    val composeMainClass: String,
    val releaseValidationReady: Boolean,
    val composePromotionApproved: Boolean,
    val javaFxFallbackAvailable: Boolean,
) {
    val title: String = "Compose launcher decision"
    val javaFxLauncherPreserved: Boolean = applicationMainClass == "com.shterneregen.securelan.desktop.Main"
    val canPromoteComposeLauncher: Boolean =
        javaFxLauncherPreserved && javaFxFallbackAvailable && releaseValidationReady && composePromotionApproved
    private val recommendedKind: ComposeLauncherDecisionKind = when {
        canPromoteComposeLauncher -> ComposeLauncherDecisionKind.PROMOTE_COMPOSE_AFTER_ACCEPTANCE
        !releaseValidationReady || !javaFxFallbackAvailable -> ComposeLauncherDecisionKind.CONTINUE_VALIDATION
        else -> ComposeLauncherDecisionKind.KEEP_JAVAFX_FALLBACK
    }
    val options: List<ComposeLauncherDecisionOption> = listOf(
        ComposeLauncherDecisionOption(
            kind = ComposeLauncherDecisionKind.KEEP_JAVAFX_FALLBACK,
            label = "Keep JavaFX launcher",
            enabled = javaFxLauncherPreserved,
            recommended = recommendedKind == ComposeLauncherDecisionKind.KEEP_JAVAFX_FALLBACK,
            reason = "Current packaged launcher remains $applicationMainClass while Compose runs through $composeMainClass.",
        ),
        ComposeLauncherDecisionOption(
            kind = ComposeLauncherDecisionKind.CONTINUE_VALIDATION,
            label = "Continue validation",
            enabled = !releaseValidationReady || !javaFxFallbackAvailable,
            recommended = recommendedKind == ComposeLauncherDecisionKind.CONTINUE_VALIDATION,
            reason = "Runtime, portable ZIP, WiX EXE, and fallback evidence must be complete before launcher promotion.",
        ),
        ComposeLauncherDecisionOption(
            kind = ComposeLauncherDecisionKind.PROMOTE_COMPOSE_AFTER_ACCEPTANCE,
            label = "Promote Compose after acceptance",
            enabled = canPromoteComposeLauncher,
            recommended = recommendedKind == ComposeLauncherDecisionKind.PROMOTE_COMPOSE_AFTER_ACCEPTANCE,
            reason = "Promotion may only happen in a separate accepted task after all gates and explicit approval are recorded.",
        ),
    )
    val recommendedOption: ComposeLauncherDecisionOption = options.first { it.kind == recommendedKind }
    val decisionSummary: String = "${recommendedOption.label}: ${recommendedOption.reason}"
    val blockerSummary: String = when {
        !javaFxLauncherPreserved -> "Launcher was changed before acceptance; restore JavaFX packaging baseline."
        !javaFxFallbackAvailable -> "JavaFX fallback is unavailable; promotion is blocked."
        !releaseValidationReady -> "Release validation is incomplete; promotion is blocked."
        !composePromotionApproved -> "Explicit Compose promotion approval is missing; promotion is blocked."
        else -> "No launcher-decision blockers remain; use a separate promotion task."
    }
}

data class ComposePackagingReadinessState(
    val desktopTestsPassed: Boolean = true,
    val desktopBuildPassed: Boolean = false,
    val composeRuntimeSmokePassed: Boolean = false,
    val portableZipValidated: Boolean = false,
    val composePortableZipValidated: Boolean = false,
    val windowsExeValidated: Boolean = false,
    val composePromotionApproved: Boolean = false,
    val javaFxFallbackAvailable: Boolean = true,
    val fullRuntimeRegressionValidated: Boolean = false,
    val applicationMainClass: String = "com.shterneregen.securelan.desktop.Main",
    val composeMainClass: String = "com.shterneregen.securelan.desktop.compose.ComposeDesktopMainKt",
    val portableTask: String = ":apps:desktop-client:buildPortable",
    val composePortableTask: String = ":apps:desktop-client:buildComposePortable",
    val exeTask: String = ":apps:desktop-client:buildExe",
    val packagingOutputPath: Path = Path.of("apps", "desktop-client", "build", "packaging"),
    val composePackagingOutputPath: Path = Path.of("apps", "desktop-client", "build", "compose-packaging"),
    val portableOutputPath: Path = Path.of("apps", "desktop-client", "build", "distributions"),
    val evidenceRecords: List<ComposePackagingEvidenceRecord> = emptyList(),
) {
    val title: String = "Compose packaging readiness"
    val launcherStatus: String = if (applicationMainClass == "com.shterneregen.securelan.desktop.Main") {
        "JavaFX launcher remains packaged baseline"
    } else {
        "Launcher changed; verify rollback and jpackage main-class before release"
    }
    val fallbackStatus: String = if (javaFxFallbackAvailable) {
        "JavaFX fallback available for packaging rollback"
    } else {
        "JavaFX fallback unavailable; packaging promotion must remain blocked"
    }
    val packagingTasksSummary: String = "JavaFX portable ZIP: $portableTask -> $portableOutputPath · Compose portable ZIP: $composePortableTask -> $portableOutputPath · Windows EXE: $exeTask -> $packagingOutputPath"
    val artifactRequirements: List<ComposePackagingArtifactRequirement> = listOf(
        ComposePackagingArtifactRequirement(
            kind = ComposePackagingArtifactKind.PORTABLE_ZIP,
            label = "JavaFX portable ZIP artifact",
            pathText = portableOutputPath.toString(),
            validated = portableZipValidated,
            validationAction = "Run $portableTask and verify the generated portable archive launches with JavaFX fallback intact.",
        ),
        ComposePackagingArtifactRequirement(
            kind = ComposePackagingArtifactKind.COMPOSE_PORTABLE_ZIP,
            label = "Compose portable ZIP artifact",
            pathText = portableOutputPath.toString(),
            validated = composePortableZipValidated,
            validationAction = "Run $composePortableTask and verify the generated portable archive launches the experimental Compose shell without replacing JavaFX packaging.",
        ),
        ComposePackagingArtifactRequirement(
            kind = ComposePackagingArtifactKind.WINDOWS_EXE,
            label = "Windows EXE artifact",
            pathText = packagingOutputPath.toString(),
            validated = windowsExeValidated,
            validationAction = "Run $exeTask on a WiX 5.0.2 environment and verify install/launch/uninstall smoke behavior.",
        ),
        ComposePackagingArtifactRequirement(
            kind = ComposePackagingArtifactKind.JAVAFX_LAUNCHER,
            label = "JavaFX packaged launcher",
            pathText = applicationMainClass,
            validated = javaFxFallbackAvailable && applicationMainClass == "com.shterneregen.securelan.desktop.Main",
            validationAction = "Keep application.mainClass, manifest main class, and jpackage main class on the JavaFX launcher.",
        ),
        ComposePackagingArtifactRequirement(
            kind = ComposePackagingArtifactKind.COMPOSE_ENTRYPOINT,
            label = "Compose experimental entrypoint",
            pathText = composeMainClass,
            validated = composeRuntimeSmokePassed,
            validationAction = "Launch :apps:desktop-client:runComposeShell without changing packaged launcher settings.",
        ),
    )
    val artifactSummary: String = "${artifactRequirements.count { it.validated }} of ${artifactRequirements.size} packaging artifact checks validated."
    val pendingArtifactSummary: String = artifactRequirements.filterNot { it.validated }.joinToString(" · ") { it.validationAction }.ifBlank {
        "All packaging artifact checks are validated."
    }
    val gates: List<ComposePackagingGate> = listOf(
        ComposePackagingGate(
            kind = ComposePackagingGateKind.DESKTOP_TESTS,
            label = "Desktop tests",
            ready = desktopTestsPassed,
            taskName = ":apps:desktop-client:test",
            evidence = "Desktop Compose/Kotlin tests must pass before packaging work.",
            blocker = "Run gradlew.bat :apps:desktop-client:test --no-daemon.",
        ),
        ComposePackagingGate(
            kind = ComposePackagingGateKind.DESKTOP_BUILD,
            label = "Desktop build",
            ready = desktopBuildPassed,
            taskName = ":apps:desktop-client:build",
            evidence = "Mixed Java/Kotlin desktop module and Application-plugin archives compile.",
            blocker = "Run gradlew.bat :apps:desktop-client:build --no-daemon.",
        ),
        ComposePackagingGate(
            kind = ComposePackagingGateKind.COMPOSE_RUNTIME_SMOKE,
            label = "Compose runtime smoke",
            ready = composeRuntimeSmokePassed,
            taskName = ":apps:desktop-client:runComposeShell",
            evidence = "Experimental Compose shell launches without replacing the JavaFX launcher.",
            blocker = "Launch and close runComposeShell after runtime regression checks.",
        ),
        ComposePackagingGate(
            kind = ComposePackagingGateKind.PORTABLE_ZIP,
            label = "JavaFX portable ZIP",
            ready = portableZipValidated,
            taskName = portableTask,
            evidence = "Portable image contains JavaFX baseline and Compose runtime dependencies without duplicate archive failures.",
            blocker = "Validate portable ZIP from apps/desktop-client/build/distributions.",
        ),
        ComposePackagingGate(
            kind = ComposePackagingGateKind.COMPOSE_PORTABLE_ZIP,
            label = "Compose portable ZIP",
            ready = composePortableZipValidated,
            taskName = composePortableTask,
            evidence = "Separate Compose portable image launches through the experimental Compose main class while JavaFX portable packaging remains unchanged.",
            blocker = "Validate Compose portable ZIP from apps/desktop-client/build/distributions and app image from apps/desktop-client/build/compose-packaging.",
        ),
        ComposePackagingGate(
            kind = ComposePackagingGateKind.WINDOWS_EXE,
            label = "Windows EXE",
            ready = windowsExeValidated,
            taskName = exeTask,
            evidence = "WiX 5.0.2 installer creation and app launch are verified on Windows.",
            blocker = "Validate buildExe/createExe on a WiX-enabled Windows environment.",
        ),
        ComposePackagingGate(
            kind = ComposePackagingGateKind.LAUNCHER_DECISION,
            label = "Launcher decision",
            ready = composePromotionApproved && javaFxFallbackAvailable && applicationMainClass == "com.shterneregen.securelan.desktop.Main",
            taskName = "manual acceptance gate",
            evidence = "Keep JavaFX launcher until Compose parity, runtime regression, portable ZIP, and EXE evidence are accepted.",
            blocker = "Do not change application.mainClass, manifest, or jpackage main class yet.",
        ),
    )
    val readyGates: List<ComposePackagingGate> = gates.filter { it.ready }
    val blockedGates: List<ComposePackagingGate> = gates.filterNot { it.ready }
    val releaseValidationReady: Boolean = javaFxFallbackAvailable &&
        desktopTestsPassed &&
        desktopBuildPassed &&
        composeRuntimeSmokePassed &&
        fullRuntimeRegressionValidated &&
        portableZipValidated &&
        composePortableZipValidated &&
        windowsExeValidated
    val launcherDecision: ComposeLauncherDecisionState = ComposeLauncherDecisionState(
        applicationMainClass = applicationMainClass,
        composeMainClass = composeMainClass,
        releaseValidationReady = releaseValidationReady,
        composePromotionApproved = composePromotionApproved,
        javaFxFallbackAvailable = javaFxFallbackAvailable,
    )
    val summary: String = "${readyGates.size} of ${gates.size} packaging gates ready; ${blockedGates.size} blocked."
    val blockedSummary: String = blockedGates.joinToString(" · ") { "${it.label}: ${it.blocker}" }.ifBlank {
        "No packaging gates are blocked."
    }
    val canPromoteComposeLauncher: Boolean = launcherDecision.canPromoteComposeLauncher
    val promotionSummary: String = if (canPromoteComposeLauncher) {
        "Compose launcher can be considered after a separate accepted promotion task."
    } else {
        "Keep JavaFX launcher and jpackage main class unchanged."
    }
    val rollbackPlanSummary: String = if (javaFxFallbackAvailable && applicationMainClass == "com.shterneregen.securelan.desktop.Main") {
        "Rollback path preserved: packaged launcher remains JavaFX and Compose stays on runComposeShell."
    } else {
        "Rollback path broken: restore JavaFX launcher before considering Compose promotion."
    }
    val promotionChecklistSummary: String = listOf(
        "desktop tests=${desktopTestsPassed}",
        "desktop build=${desktopBuildPassed}",
        "Compose smoke=${composeRuntimeSmokePassed}",
        "runtime regression=${fullRuntimeRegressionValidated}",
        "JavaFX portable ZIP=${portableZipValidated}",
        "Compose portable ZIP=${composePortableZipValidated}",
        "Windows EXE=${windowsExeValidated}",
        "fallback=${javaFxFallbackAvailable}",
        "approval=${composePromotionApproved}",
    ).joinToString(" · ")
    val acceptedEvidenceRecords: List<ComposePackagingEvidenceRecord> = evidenceRecords.filter(ComposePackagingEvidenceRecord::validated)
    val pendingEvidenceRecords: List<ComposePackagingEvidenceRecord> = evidenceRecords.filterNot(ComposePackagingEvidenceRecord::validated)
    val packagingEvidenceSummary: String = if (evidenceRecords.isEmpty()) {
        "No packaging evidence records captured yet."
    } else {
        "${acceptedEvidenceRecords.size} packaging evidence records validated; ${pendingEvidenceRecords.size} pending."
    }
    val packagingEvidenceCopyText: String = evidenceRecords.joinToString("\n") { it.copyLine }
    val promotionDecisionSteps: List<ComposePromotionDecisionStep> = listOf(
        ComposePromotionDecisionStep(
            kind = ComposePromotionDecisionStepKind.PRESERVE_FALLBACK,
            label = "Preserve fallback",
            satisfied = javaFxFallbackAvailable && applicationMainClass == "com.shterneregen.securelan.desktop.Main",
            action = "Keep JavaFX launcher and fallback UI available until a separate promotion task is accepted.",
        ),
        ComposePromotionDecisionStep(
            kind = ComposePromotionDecisionStepKind.COMPLETE_RUNTIME_REGRESSION,
            label = "Complete runtime regression",
            satisfied = fullRuntimeRegressionValidated,
            action = "Record full Compose runtime regression across accepted flows before packaging release gates.",
        ),
        ComposePromotionDecisionStep(
            kind = ComposePromotionDecisionStepKind.VALIDATE_PACKAGING,
            label = "Validate packaging",
            satisfied = desktopTestsPassed && desktopBuildPassed && composeRuntimeSmokePassed && portableZipValidated && composePortableZipValidated && windowsExeValidated,
            action = "Pass desktop tests/build, runComposeShell smoke, JavaFX portable ZIP, Compose portable ZIP, and WiX EXE validation.",
        ),
        ComposePromotionDecisionStep(
            kind = ComposePromotionDecisionStepKind.REQUIRE_APPROVAL,
            label = "Require explicit approval",
            satisfied = composePromotionApproved,
            action = "Keep promotion blocked until explicit Compose launcher approval is recorded.",
        ),
        ComposePromotionDecisionStep(
            kind = ComposePromotionDecisionStepKind.SEPARATE_PROMOTION_TASK,
            label = "Separate promotion task",
            satisfied = canPromoteComposeLauncher,
            action = "Only change launcher, manifest, or jpackage main class in a dedicated accepted promotion task.",
        ),
    )
    val promotionDecisionSummary: String = if (canPromoteComposeLauncher) {
        "All promotion decision steps are satisfied; launcher changes still belong in a separate task."
    } else {
        "Promotion remains blocked: ${promotionDecisionSteps.filterNot { it.satisfied }.joinToString { it.label }}."
    }
    val validationReport: ComposePackagingValidationReport = ComposePackagingValidationReport(
        title = "Compose packaging validation report",
        gateSummary = summary,
        artifactSummary = artifactSummary,
        releaseSummary = promotionDecisionSummary,
        rollbackSummary = rollbackPlanSummary,
        evidenceLines = if (evidenceRecords.isEmpty()) listOf("No packaging evidence records captured yet.") else evidenceRecords.map { it.copyLine },
    )
}

enum class ComposePeerListLifecycleState {
    IDLE,
    DISCOVERING,
    PEERS_VISIBLE,
    PEER_SELECTED,
    PEER_TARGETED,
    BLOCKED_ERROR,
}

data class ComposePeerListLifecycleStep(
    val state: ComposePeerListLifecycleState,
    val ready: Boolean,
    val label: String,
    val sideEffectContract: String,
) {
    val displayText: String = if (ready) "$label ready" else "$label blocked"
}

data class ComposePeerListLifecyclePlan(
    val currentState: ComposePeerListLifecycleState,
    val steps: List<ComposePeerListLifecycleStep>,
    val blockedReasons: List<String>,
    val fallbackAvailable: Boolean,
    val selectedPeerLabel: String,
) {
    val title: String = "Live peer-list binding contract"
    val stateLabel: String = currentState.name.lowercase().replace('_', '/')
    val readySteps: List<ComposePeerListLifecycleStep> = steps.filter { it.ready }
    val readinessSummary: String = if (readySteps.isEmpty()) {
        "No live peer-list lifecycle steps are ready for Compose wiring."
    } else {
        "Ready lifecycle steps: ${readySteps.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedReasons.isEmpty()) {
        "No peer-list lifecycle blockers; service calls remain intentionally deferred."
    } else {
        blockedReasons.joinToString(" · ")
    }
    val fallbackStatus: String = if (fallbackAvailable) {
        "JavaFX peer-list fallback available"
    } else {
        "JavaFX peer-list fallback unavailable; live Compose peer binding must remain blocked"
    }
    val sideEffectContractSummary: String = steps.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    companion object {
        fun from(state: ComposePeerListState): ComposePeerListLifecyclePlan {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val selectedPeer = state.selectedPeer
            val hasPeers = state.visiblePeers.isNotEmpty()
            val hasOnlinePeer = selectedPeer?.online == true
            val selectedPeerLabel = selectedPeer?.nickname ?: "no peer selected"

            val blockers = buildList {
                if (!fallbackAvailable) {
                    add("JavaFX peer-list fallback is unavailable; live Compose peer binding must remain blocked.")
                }
            }

            val currentState = when {
                blockers.isNotEmpty() -> ComposePeerListLifecycleState.BLOCKED_ERROR
                hasOnlinePeer && state.targetActions.chatReady -> ComposePeerListLifecycleState.PEER_TARGETED
                hasOnlinePeer -> ComposePeerListLifecycleState.PEER_SELECTED
                hasPeers -> ComposePeerListLifecycleState.PEERS_VISIBLE
                fallbackAvailable -> ComposePeerListLifecycleState.DISCOVERING
                else -> ComposePeerListLifecycleState.BLOCKED_ERROR
            }

            val steps = listOf(
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.IDLE,
                    ready = currentState == ComposePeerListLifecycleState.IDLE && blockers.isEmpty(),
                    label = "Idle",
                    sideEffectContract = "observe peer-list preview state only; do not subscribe to discovery or chat events",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.DISCOVERING,
                    ready = fallbackAvailable && blockers.isEmpty() && !hasOnlinePeer,
                    label = "Discovering",
                    sideEffectContract = "reflect discovery/listening state; an empty peer list is normal while JavaFX remains discovery owner",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.PEERS_VISIBLE,
                    ready = fallbackAvailable && hasPeers,
                    label = "Peers visible",
                    sideEffectContract = "display visible peer list without subscribing to discovery refresh",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.PEER_SELECTED,
                    ready = fallbackAvailable && hasOnlinePeer,
                    label = "Peer selected",
                    sideEffectContract = "reflect selected-peer metadata and target-action readiness; do not connect or start RTC",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.PEER_TARGETED,
                    ready = fallbackAvailable && hasOnlinePeer && state.targetActions.chatReady,
                    label = "Peer targeted",
                    sideEffectContract = "peer is selected and target actions are ready; JavaFX still owns live targeting and runtime actions",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.BLOCKED_ERROR,
                    ready = blockers.isNotEmpty(),
                    label = "Blocked/error",
                    sideEffectContract = "surface validation or fallback blockers before any live peer-list binding can run",
                ),
            )

            return ComposePeerListLifecyclePlan(
                currentState = currentState,
                steps = steps,
                blockedReasons = blockers,
                fallbackAvailable = fallbackAvailable,
                selectedPeerLabel = selectedPeerLabel,
            )
        }
    }
}

enum class ComposePeerListTransitionKind {
    SELECT_PEER,
    DESELECT_PEER,
    TARGET_PEER_FOR_CHAT,
    TARGET_PEER_FOR_FILE,
    TARGET_PEER_FOR_VOICE,
    TARGET_PEER_FOR_VIDEO,
    TARGET_PEER_FOR_DATA,
    REFRESH_PEER_LIST,
}

data class ComposePeerListTransitionIntent(
    val kind: ComposePeerListTransitionKind,
    val label: String,
    val sourceState: ComposePeerListLifecycleState,
    val targetState: ComposePeerListLifecycleState,
    val enabled: Boolean,
    val guardSummary: String,
    val blockedReason: String,
    val sideEffectContract: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val routeSummary: String = "${sourceState.name.lowercase()} -> ${targetState.name.lowercase()}"
}

data class ComposePeerListTransitionPlan(
    val transitions: List<ComposePeerListTransitionIntent>,
) {
    val title: String = "Peer-list transition intents"
    val enabledTransitions: List<ComposePeerListTransitionIntent> = transitions.filter { it.enabled }
    val blockedTransitions: List<ComposePeerListTransitionIntent> = transitions.filterNot { it.enabled }
    val enabledSummary: String = if (enabledTransitions.isEmpty()) {
        "No peer-list transitions are ready for future live Compose wiring."
    } else {
        "Ready transitions: ${enabledTransitions.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedTransitions.isEmpty()) {
        "No peer-list transitions are blocked in this preview state."
    } else {
        "Blocked transitions: ${blockedTransitions.joinToString { it.label }}"
    }
    val sideEffectSummary: String = transitions.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    companion object {
        fun from(
            state: ComposePeerListState,
            lifecyclePlan: ComposePeerListLifecyclePlan = state.peerListLifecyclePlan,
            controlPlan: ComposePeerTargetControlPlan = state.targetControlPlan,
        ): ComposePeerListTransitionPlan {
            val source = lifecyclePlan.currentState
            val fallbackBlock = "JavaFX peer-list fallback is unavailable; transition intents must remain blocked."
            fun blocked(reason: String): String = if (!lifecyclePlan.fallbackAvailable) fallbackBlock else reason

            return ComposePeerListTransitionPlan(
                listOf(
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.SELECT_PEER,
                        label = "Select peer",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_SELECTED,
                        enabled = lifecyclePlan.fallbackAvailable && state.visiblePeers.isNotEmpty(),
                        guardSummary = "Peer selection requires at least one visible peer in the list.",
                        blockedReason = blocked("No visible peers; select-peer transition must remain blocked."),
                        sideEffectContract = "future implementation may update selected-peer index without opening connections",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.DESELECT_PEER,
                        label = "Deselect peer",
                        sourceState = source,
                        targetState = if (state.visiblePeers.isNotEmpty()) ComposePeerListLifecycleState.PEERS_VISIBLE else ComposePeerListLifecycleState.IDLE,
                        enabled = lifecyclePlan.fallbackAvailable && state.selectedPeer != null,
                        guardSummary = "Deselection clears the selected peer and returns to the visible-peers or idle state.",
                        blockedReason = blocked("No peer is currently selected; deselect transition must remain blocked."),
                        sideEffectContract = "future implementation may clear selected-peer index without affecting discovery",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_CHAT,
                        label = "Target peer for chat",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.chatReady,
                        guardSummary = "Chat targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set chat target while JavaFX owns shared-room messaging",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_FILE,
                        label = "Target peer for file transfer",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.fileReady,
                        guardSummary = "File transfer targeting requires an online discovered peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set encrypted file-transfer target without invoking transfer",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_VOICE,
                        label = "Target peer for voice",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.voiceReady,
                        guardSummary = "Voice targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.VOICE_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set voice target while WebRTC runtime stays JavaFX-owned",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_VIDEO,
                        label = "Target peer for video",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.videoReady,
                        guardSummary = "Experimental video targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.VIDEO_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set experimental video target while preserving fallback diagnostics",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_DATA,
                        label = "Target peer for RTC data",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.dataChannelReady,
                        guardSummary = "RTC data-channel targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.DATA_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set data-channel target while signaling stays routed through chat-core",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.REFRESH_PEER_LIST,
                        label = "Refresh peer list",
                        sourceState = source,
                        targetState = if (state.visiblePeers.isNotEmpty()) ComposePeerListLifecycleState.PEERS_VISIBLE else ComposePeerListLifecycleState.DISCOVERING,
                        enabled = lifecyclePlan.fallbackAvailable,
                        guardSummary = "Peer-list refresh re-evaluates visible peers without starting discovery.",
                        blockedReason = blocked("JavaFX fallback is unavailable; peer-list refresh must remain blocked."),
                        sideEffectContract = "future implementation may trigger a discovery scan through JavaFX-owned discovery service",
                    ),
                ),
            )
        }
    }
}

enum class ComposePeerListAdapterEventKind {
    PEER_DISCOVERED,
    PEER_LOST,
    PEER_SELECTED,
    PEER_DESELECTED,
    PEER_TARGET_CHANGED,
    PEER_LIST_REFRESHED,
}

data class ComposePeerListAdapterEventContract(
    val kind: ComposePeerListAdapterEventKind,
    val label: String,
    val ready: Boolean,
    val guarded: Boolean,
    val description: String,
    val prerequisites: List<String>,
    val blockedReason: String,
) {
    val routeTag: String = kind.name.lowercase().replace('_', '-')
    val readinessLabel: String = when {
        !ready && guarded -> "$label blocked (guarded)"
        !ready -> "$label blocked"
        else -> "$label ready"
    }
}

data class ComposePeerListAdapterEventRouting(
    val contracts: List<ComposePeerListAdapterEventContract>,
    val fallbackAvailable: Boolean,
    val readyEvents: List<ComposePeerListAdapterEventContract>,
    val blockedEvents: List<ComposePeerListAdapterEventContract>,
) {
    val title: String = "Peer-list adapter event contract"
    val subtitle: String =
        "Side-effect-free event contract for future live peer-list integration; JavaFX still owns discovery and selection."
    val readyCount: Int = readyEvents.size
    val blockedCount: Int = blockedEvents.size
    val totalCount: Int = contracts.size
    val readinessSummary: String = when {
        readyCount == totalCount -> "All $totalCount peer-list adapter events are ready for future live wiring."
        blockedCount == totalCount -> "All $totalCount peer-list adapter events are blocked; JavaFX discovery must remain active."
        else -> "$readyCount of $totalCount peer-list events ready; $blockedCount blocked."
    }
    val fallbackStatus: String =
        if (fallbackAvailable) "JavaFX fallback available; adapter event routing is speculative." else "JavaFX fallback unavailable; live adapter event routing must remain blocked."
    val blockedSummary: String = if (blockedEvents.isEmpty()) {
        "No peer-list adapter events are blocked."
    } else {
        blockedEvents.joinToString(" · ") { "${it.readinessLabel}: ${it.blockedReason}" }
    }
    val eventOrderLabel: String = contracts.joinToString(" → ") { it.routeTag }

    companion object {
        fun from(state: ComposePeerListState): ComposePeerListAdapterEventRouting {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val fallbackBlock =
                "JavaFX fallback is unavailable; live peer-list adapter event routing must remain blocked."

            fun block(reason: String): String = if (!fallbackAvailable) fallbackBlock else reason

            val hasPeers = state.visiblePeers.isNotEmpty()
            val selectedPeer = state.selectedPeer
            val hasSelected = selectedPeer != null
            val selectedOnline = selectedPeer?.online == true

            val peerDiscoveredReady = fallbackAvailable && hasPeers
            val peerLostReady = fallbackAvailable && hasPeers
            val peerSelectedReady = fallbackAvailable && hasSelected && selectedOnline
            val peerDeselectedReady = fallbackAvailable && hasSelected
            val peerTargetChangedReady = fallbackAvailable && hasSelected && selectedOnline
            val peerListRefreshedReady = fallbackAvailable

            return ComposePeerListAdapterEventRouting(
                contracts = listOf(
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_DISCOVERED,
                        label = "Peer discovered",
                        ready = peerDiscoveredReady,
                        guarded = !peerDiscoveredReady && fallbackAvailable,
                        description = "Future runtime adapter fires when a new LAN peer is discovered and added to the visible list.",
                        prerequisites = listOf("visible peers exist"),
                        blockedReason = block("No peers are visible; peer-discovered event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_LOST,
                        label = "Peer lost",
                        ready = peerLostReady,
                        guarded = !peerLostReady && fallbackAvailable,
                        description = "Future runtime adapter fires when a peer goes offline or is removed from the visible list.",
                        prerequisites = listOf("visible peers exist"),
                        blockedReason = block("No peers are visible; peer-lost event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_SELECTED,
                        label = "Peer selected",
                        ready = peerSelectedReady,
                        guarded = !peerSelectedReady && fallbackAvailable,
                        description = "Future runtime adapter fires when the user selects an online peer from the list.",
                        prerequisites = listOf("online peer selected"),
                        blockedReason = block("No online peer is selected; peer-selected event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_DESELECTED,
                        label = "Peer deselected",
                        ready = peerDeselectedReady,
                        guarded = !peerDeselectedReady && fallbackAvailable,
                        description = "Future runtime adapter fires when the user deselects the current peer.",
                        prerequisites = listOf("a peer is currently selected"),
                        blockedReason = block("No peer is currently selected; peer-deselected event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_TARGET_CHANGED,
                        label = "Peer target changed",
                        ready = peerTargetChangedReady,
                        guarded = !peerTargetChangedReady && fallbackAvailable,
                        description = "Future runtime adapter fires when target actions (chat/file/voice/video/data) are set for the selected peer.",
                        prerequisites = listOf("online peer selected"),
                        blockedReason = block("No online peer is selected; peer-target-changed event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_LIST_REFRESHED,
                        label = "Peer list refreshed",
                        ready = peerListRefreshedReady,
                        guarded = true,
                        description = "Future runtime adapter fires after discovery produces updated peer presence; it is guarded and fires only on actual refresh.",
                        prerequisites = listOf("discovery refresh triggered"),
                        blockedReason = block("JavaFX fallback is unavailable; peer-list refresh is suspended."),
                    ),
                ),
                fallbackAvailable = fallbackAvailable,
                readyEvents = emptyList(),
                blockedEvents = emptyList(),
            ).let { routing ->
                routing.copy(
                    readyEvents = routing.contracts.filter { it.ready },
                    blockedEvents = routing.contracts.filterNot { it.ready },
                )
            }
        }
    }
}
