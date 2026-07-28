package com.shterneregen.securelan.desktop.compose.state.shell

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatWorkspaceState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposeSelectedPeerQuickActionsState
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState

object ComposeShellMetadata {
    const val WINDOW_TITLE: String = "SecureLanSuite Chat"
    const val APP_NAME: String = "SecureLanSuite"
    val DEFAULT_WINDOW_WIDTH: Dp = 1360.dp
    val DEFAULT_WINDOW_HEIGHT: Dp = 860.dp

    // Composer pinning / overflow-safe startup layout constants
    val COMPOSER_MIN_HEIGHT: Dp = 52.dp
    val COMPOSER_SAFE_VERTICAL_SPACE: Dp = 96.dp
    val CONNECTION_HUB_EXPANDED_MAX_FRACTION: Float = 0.55f
    val MIN_CHAT_SURFACE_HEIGHT: Dp = 140.dp
    val CENTER_COLUMN_SPACING: Dp = 8.dp
    val ADVANCED_PANE_MAX_HEIGHT: Dp = 260.dp
    val ATTACHMENT_MENU_MAX_WIDTH: Dp = 320.dp
    val ATTACHMENT_MENU_MIN_WIDTH: Dp = 248.dp
    val ATTACHMENT_MENU_MAX_HEIGHT: Dp = 300.dp
    val SIDE_EMPTY_STATE_GUIDANCE_MAX_WIDTH: Dp = 244.dp
    val CENTER_EMPTY_STATE_GUIDANCE_MAX_WIDTH: Dp = 440.dp
    val INLINE_EMPTY_STATE_MIN_HEIGHT: Dp = 44.dp
    val DEFAULT_STATUS_ADAPTER_STATE: ComposeStatusConnectionState = ComposeStatusConnectionState()
    val DEFAULT_CONNECTION_HUB_STATE: ComposeConnectionHubState = ComposeConnectionHubState(
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
    )
    val DEFAULT_ROOM_STARTUP_STATE: ComposeRoomStartupState = ComposeRoomStartupState(
        connectionHubState = DEFAULT_CONNECTION_HUB_STATE,
        nearbyRooms = ComposePeerListItem.defaultPreviewItems(clientConnected = false),
    )
    val DEFAULT_PEER_LIST_STATE: ComposePeerListState = ComposePeerListState()
    val DEFAULT_CHAT_WORKSPACE_STATE: ComposeChatWorkspaceState = ComposeChatWorkspaceState(
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
    val DEFAULT_WORKSPACE_LAYOUT: ComposeWorkspaceLayout = ComposeWorkspaceLayout()
    val DEFAULT_CONTEXT_PANEL_STATE: ComposeContextPanelState = ComposeContextPanelState.forRoom(
        peerListState = DEFAULT_PEER_LIST_STATE,
        transferState = DEFAULT_FILE_TRANSFER_STATE,
    )
    val DEFAULT_ONBOARDING_STATE: ComposeOnboardingState = ComposeOnboardingState()
    val DEFAULT_PRODUCT_SCREEN_STATE: ComposeProductScreenState = ComposeProductScreenState()
    val DEFAULT_WORKSPACE_STATE: ComposeWorkspaceState = ComposeWorkspaceState()
    val DEFAULT_APP_SHELL_STATE: ComposeAppShellState = ComposeAppShellState(
        productState = DEFAULT_PRODUCT_SCREEN_STATE,
        statusState = DEFAULT_STATUS_ADAPTER_STATE,
        workspaceState = DEFAULT_WORKSPACE_STATE,
    )
    val DEFAULT_WORKSPACE_CONSISTENCY_REVIEW_STATE: ComposeWorkspaceConsistencyReviewState = ComposeWorkspaceConsistencyReviewState()
}
