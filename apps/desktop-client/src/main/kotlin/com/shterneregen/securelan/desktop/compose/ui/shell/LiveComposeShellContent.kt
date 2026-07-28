package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.runtime.*
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.*
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyMode
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.chat.LiveChatWorkspaceCard
import com.shterneregen.securelan.desktop.compose.ui.connection.MessengerCenterPanel
import com.shterneregen.securelan.desktop.compose.ui.context.ChatCallActions
import com.shterneregen.securelan.desktop.compose.ui.context.LiveActionsColumn
import com.shterneregen.securelan.desktop.compose.ui.media.ComposeVideoStage
import com.shterneregen.securelan.desktop.compose.ui.peerlist.LivePeerListCard
import com.shterneregen.securelan.desktop.compose.ui.settings.LiveSettingsDialog
import com.shterneregen.securelan.desktop.compose.ui.steganography.LiveSteganographyDialog
import com.shterneregen.securelan.desktop.compose.util.resolveAttachCandidatePeer
import com.shterneregen.securelan.desktop.compose.util.resolveSelectedJoinTarget

@Composable
internal fun LiveComposeShellContent(
    hostAdapter: ComposeDesktopHostAdapter,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    val inMessengerMode = hostAdapter.statusState.localServerRunning || hostAdapter.statusState.clientConnected

    var selectedPeerKey by remember { mutableStateOf<String?>(null) }
    var selectedJoinTarget by remember { mutableStateOf<ComposeConnectionJoinTarget?>(null) }
    val peers =
        hostAdapter.visiblePeerItems.map { peer -> ComposePeerListItem.fromPeer(peer, hostAdapter.chatConnected) }
    val defaultSelectedPeerIndex = if (selectedPeerKey == null) {
        peers.indexOfFirst { it.online }.takeIf { it >= 0 } ?: peers.indices.firstOrNull() ?: -1
    } else {
        -1
    }
    val peerState = ComposePeerListState(
        peers = peers,
        selectedPeerIndex = defaultSelectedPeerIndex,
        selectedPeerNickname = selectedPeerKey,
    )
    if (selectedPeerKey != null && peerState.selectedPeer == null) {
        selectedPeerKey = null
    }
    LaunchedEffect(peerState.selectedPeer) {
        selectedJoinTarget = resolveSelectedJoinTarget(hostAdapter, peerState.selectedPeer)
    }
    val hubTooltip =
        peerState.selectedPeer?.let { "Actions on the right will target \"${it.nickname}\". Text chat remains shared for now." }
            ?: "Connect to chat, then select a peer on the left for voice, video, and file actions."

    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        senderId = hostAdapter.statusState.nickname,
        sessionPassword = hostAdapter.currentRoomPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles,
    )
    val voiceState = hostAdapter.mediaVoiceState.copy(peerListState = peerState)
    val videoState = hostAdapter.experimentalVideoState.copy(peerListState = peerState)
    val workspaceState = ComposeWorkspaceState.from(
        statusState = hostAdapter.statusState,
        peerState = peerState,
        transferState = transferState,
        voiceState = voiceState,
        videoState = videoState,
    )
    var expandedCardKind by remember { mutableStateOf<ComposeContextPanelCardKind?>(null) }
    var steganographyDialogMode by remember { mutableStateOf<ComposeSteganographyMode?>(null) }
    var settingsDialogOpen by remember { mutableStateOf(false) }
    val productState = ComposeProductScreenState.from(
        statusState = hostAdapter.statusState,
        requestedAppMode = if (inMessengerMode) AppMode.MESSENGER else AppMode.WELCOME,
        connectionHubMode = ComposeConnectionHubMode.HOST,
        selectedPeer = peerState.selectedPeer,
    )
    val topBarLabel = if (inMessengerMode) {
        hostAdapter.statusState.nickname.ifBlank { "Secure room" }
    } else {
        ComposeShellMetadata.DEFAULT_ONBOARDING_STATE.headline
    }
    SecureLanAppShell(
        shellState = ComposeAppShellState(
            productState = productState,
            statusState = hostAdapter.statusState,
            peerStatus = peerState.peerStatus,
            workspaceState = workspaceState,
        ),
        topBarLabel = topBarLabel,
        darkTheme = darkTheme,
        onSettingsClick = { settingsDialogOpen = true },
        onThemeToggle = onThemeToggle,
    ) {
        val rightColumnTitle = ComposeShellMetadata.DEFAULT_CONTEXT_PANEL_STATE.title
        MainWorkspaceRow(
            layout = ComposeShellMetadata.DEFAULT_WORKSPACE_LAYOUT,
            peersTooltip = peerState.hint,
            rightColumnTitle = rightColumnTitle,
            chatActions = {
                if (peerState.selectedPeer != null) {
                    ChatCallActions(
                        hostAdapter = hostAdapter,
                        selectedPeer = peerState.selectedPeer,
                    )
                }
            },
            peersColumn = {
                LivePeerListCard(
                    peerState = peerState,
                    onPeerSelected = { key ->
                        selectedPeerKey = key
                    },
                )
            },
            chatColumn = {
                MessengerCenterPanel(
                    hostAdapter = hostAdapter,
                    workspaceState = workspaceState,
                    selectedJoinTarget = selectedJoinTarget,
                    hubTooltip = hubTooltip,
                    chatSurface = {
                        LiveChatWorkspaceCard(
                            hostAdapter,
                            peerState,
                            onExpandedCardKindChange = { kind ->
                                expandedCardKind = if (expandedCardKind == kind) null else kind
                            },
                            onOpenSteganography = { mode -> steganographyDialogMode = mode },
                        ) {
                            ComposeVideoStage(hostAdapter.experimentalVideoState.copy(peerListState = peerState))
                        }
                    },
                )
            },
            actionsColumn = { responsiveState ->
                LiveActionsColumn(
                    hostAdapter = hostAdapter,
                    peerState = peerState,
                    responsiveState = responsiveState,
                    expandedCardKind = expandedCardKind,
                )
            },
        )
    }
    steganographyDialogMode?.let { mode ->
        LiveSteganographyDialog(
            hostAdapter = hostAdapter,
            initialMode = mode,
            recipient = resolveAttachCandidatePeer(peerState.selectedPeer, hostAdapter::discoveredPeerFor),
            onClose = { steganographyDialogMode = null },
        )
    }
    if (settingsDialogOpen) {
        LiveSettingsDialog(
            hostAdapter = hostAdapter,
            peerState = peerState,
            onClose = { settingsDialogOpen = false },
        )
    }
}
