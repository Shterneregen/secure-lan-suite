package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.material.Text
import androidx.compose.runtime.*

import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeAppShellState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.ui.chat.ChatWorkspacePreviewCard
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.connection.MessengerCenterPanel
import com.shterneregen.securelan.desktop.compose.ui.context.PreviewActionsColumn
import com.shterneregen.securelan.desktop.compose.ui.peerlist.PeerListPreviewCard
import com.shterneregen.securelan.desktop.compose.ui.settings.PreviewSettingsDialog

@Composable
internal fun PreviewComposeShellContent(
    themeMode: SecureLanThemeMode,
    onThemeToggle: () -> Unit,
) {
    var settingsDialogOpen by remember { mutableStateOf(false) }
    val previewPeerState = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE
    val previewChatState = ComposeShellMetadata.DEFAULT_CHAT_WORKSPACE_STATE
    val previewProductState = ComposeShellMetadata.DEFAULT_PRODUCT_SCREEN_STATE
    val previewWorkspaceState = ComposeShellMetadata.DEFAULT_WORKSPACE_STATE
    val hubTooltip = previewChatState.subtitle
    SecureLanAppShell(
        shellState = ComposeAppShellState(
            productState = previewProductState,
            statusState = ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE,
            workspaceState = previewWorkspaceState,
        ),
        topBarLabel = ComposeShellMetadata.DEFAULT_ONBOARDING_STATE.headline,
        topBarStatus = "Secure room · Offline",
        themeMode = themeMode,
        onOpenQuickShare = {},
        onOpenSteganography = {},
        onSettingsClick = { settingsDialogOpen = true },
        onThemeToggle = onThemeToggle,
    ) {
        MainWorkspaceRow(
            layout = ComposeShellMetadata.DEFAULT_WORKSPACE_LAYOUT,
            peersTooltip = previewPeerState.hint,
            rightColumnTitle = ComposeShellMetadata.DEFAULT_CONTEXT_PANEL_STATE.title,
            peersColumn = { PeerListPreviewCard(previewPeerState) },
            chatColumn = { responsiveHeaderActions ->
                MessengerCenterPanel(
                    hostAdapter = null,
                    workspaceState = previewWorkspaceState,
                    selectedJoinTarget = null,
                    hubTooltip = hubTooltip,
                    headerActions = {
                        if (previewPeerState.selectedPeer != null) {
                            CompactButton(onClick = {}, enabled = false) { Text("Voice call") }
                            CompactButton(onClick = {}, enabled = false) { Text("Video call") }
                            CompactButton(onClick = {}, enabled = false) { Text("End call") }
                        }
                        responsiveHeaderActions()
                    },
                    chatSurface = { ChatWorkspacePreviewCard(previewChatState) },
                )
            },
            actionsColumn = { responsiveState -> PreviewActionsColumn(responsiveState) },
        )
    }
    if (settingsDialogOpen) {
        PreviewSettingsDialog(onClose = { settingsDialogOpen = false })
    }
}
