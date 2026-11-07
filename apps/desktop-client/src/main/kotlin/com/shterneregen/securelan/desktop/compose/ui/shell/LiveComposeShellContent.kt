package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import com.shterneregen.securelan.desktop.compose.settings.DesktopAppSettingsController
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
import com.shterneregen.securelan.desktop.compose.ui.lantools.LiveLanToolsDialog
import com.shterneregen.securelan.desktop.compose.ui.media.ComposeVideoStage
import com.shterneregen.securelan.desktop.compose.ui.peerlist.LivePeerListCard
import com.shterneregen.securelan.desktop.compose.ui.quickshare.LiveQuickShareDialog
import com.shterneregen.securelan.desktop.compose.ui.settings.LiveSettingsDialog
import com.shterneregen.securelan.desktop.compose.ui.steganography.LiveSteganographyDialog
import com.shterneregen.securelan.desktop.compose.ui.transfer.LiveTransferBatchDialog
import com.shterneregen.securelan.desktop.compose.util.resolveAttachCandidatePeer
import com.shterneregen.securelan.desktop.compose.util.resolveSelectedJoinTarget
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController

@Composable
internal fun LiveComposeShellContent(
    hostAdapter: ComposeDesktopHostAdapter,
    themeMode: SecureLanThemeMode,
    settingsController: DesktopAppSettingsController,
    onThemeToggle: () -> Unit,
    lanToolsController: DesktopLanToolsController? = null,
) {
    val toolsFocus = remember { FocusRequester() }
    var restoreToolsFocus by remember { mutableStateOf(false) }
    LaunchedEffect(restoreToolsFocus) {
        if (restoreToolsFocus) {
            withFrameNanos { }
            toolsFocus.requestFocus()
            restoreToolsFocus = false
        }
    }
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
        peerState.selectedPeer?.let { "Files and calls target \"${it.nickname}\". Messages go to everyone in the room. Video is experimental." }
            ?: "Files and calls become available when a participant joins. Messages are shared with the room."

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
    var roomInvitationOpen by remember { mutableStateOf(false) }
    var quickShareDialogOpen by remember { mutableStateOf(false) }
    var steganographyDialogMode by remember { mutableStateOf<ComposeSteganographyMode?>(null) }
    var settingsDialogOpen by remember { mutableStateOf(false) }
    val productState = ComposeProductScreenState.from(
        statusState = hostAdapter.statusState,
        requestedAppMode = if (inMessengerMode) AppMode.MESSENGER else AppMode.WELCOME,
        selectedPeer = peerState.selectedPeer,
    )
    val topBarLabel = hostAdapter.statusState.nickname.ifBlank { "SecureLanSuite" }
    val roomConnectionStatus = when {
        hostAdapter.statusState.localServerRunning -> "Hosting room - ${peerState.onlinePeers.size} online"
        hostAdapter.statusState.clientConnected -> "Connected - ${peerState.onlinePeers.size} online"
        else -> "No active room"
    }
    SecureLanAppShell(
        shellState = ComposeAppShellState(
            productState = productState,
            statusState = hostAdapter.statusState,
            peerStatus = peerState.peerStatus,
            workspaceState = workspaceState,
        ),
        topBarLabel = topBarLabel,
        topBarStatus = roomConnectionStatus,
        localNetworkAddresses = hostAdapter.localNetworkAddresses,
        themeMode = themeMode,
        onOpenQuickShare = { quickShareDialogOpen = true },
        onOpenSteganography = { steganographyDialogMode = ComposeSteganographyMode.HIDE },
        onSettingsClick = { settingsDialogOpen = true },
        onThemeToggle = onThemeToggle,
        onOpenLanTools = lanToolsController?.let { it::open },
        toolsFocusRequester = toolsFocus,
    ) {
        val rightColumnTitle = "Activity"
        MainWorkspaceRow(
            layout = ComposeShellMetadata.DEFAULT_WORKSPACE_LAYOUT,
            peersTooltip = peerState.hint,
            rightColumnTitle = rightColumnTitle,
            peersColumn = {
                LivePeerListCard(
                    peerState = peerState,
                    activeCallPeer = videoState.currentSession?.remotePeer ?: voiceState.currentSession?.remotePeer,
                    onPeerSelected = { key ->
                        selectedPeerKey = key
                    },
                )
            },
            chatColumn = { responsiveHeaderActions ->
                MessengerCenterPanel(
                    hostAdapter = hostAdapter,
                    workspaceState = workspaceState,
                    selectedJoinTarget = selectedJoinTarget,
                    hubTooltip = hubTooltip,
                    recentRooms = settingsController.settings.network.recentRooms,
                    headerActions = {
                        if (hostAdapter.statusState.localServerRunning) {
                            RoomHeaderAction("Invite", com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons.QuickShare, onClick = { roomInvitationOpen = true })
                        }
                        if (peerState.selectedPeer != null || voiceState.currentSession != null || videoState.currentSession != null) {
                            ChatCallActions(
                                hostAdapter = hostAdapter,
                                selectedPeer = peerState.selectedPeer,
                            )
                        }
                        responsiveHeaderActions()
                    },
                    chatSurface = {
                        LiveChatWorkspaceCard(
                            hostAdapter,
                            peerState,
                        ) { modifier ->
                            ComposeVideoStage(
                                state = hostAdapter.experimentalVideoState.copy(peerListState = peerState),
                                modifier = modifier,
                            )
                        }
                    },
                )
            },
            actionsColumn = { responsiveState ->
                LiveActionsColumn(
                    hostAdapter = hostAdapter,
                    peerState = peerState,
                    responsiveState = responsiveState,
                    onOpenQuickShare = { quickShareDialogOpen = true },
                    onOpenLanTools = lanToolsController?.let { it::open },
                )
            },
        )
    }
    if (lanToolsController != null) {
        val lanToolsState by lanToolsController.state.collectAsState()
        if (lanToolsState.visible) LiveLanToolsDialog(lanToolsController) {
            lanToolsController.dismiss()
            restoreToolsFocus = true
        }
    }
    if (roomInvitationOpen) com.shterneregen.securelan.desktop.compose.ui.connection.RoomInvitationDialog(hostAdapter) { roomInvitationOpen = false }
    if (quickShareDialogOpen) {
        LiveQuickShareDialog(
            hostAdapter = hostAdapter,
            onClose = { quickShareDialogOpen = false },
        )
    }
    if (hostAdapter.transferReviewVisible && hostAdapter.transferDraft != null) {
        LiveTransferBatchDialog(
            hostAdapter = hostAdapter,
            onClose = { hostAdapter.dismissTransferReview() },
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
            settingsController = settingsController,
            onClose = { settingsDialogOpen = false },
        )
    }
}
