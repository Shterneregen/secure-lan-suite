package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState

data class ComposeWorkspaceState(
    val mode: ComposeWorkspaceMode = ComposeWorkspaceMode.OFFLINE,
    val title: String = "Shared room",
    val subtitle: String = "",
    val layoutContract: ComposeWorkspaceLayoutContract = ComposeWorkspaceLayoutContract(),
) {
    val chatVisible: Boolean = true
    val startupSurfaceVisible: Boolean = true
    val connectionHubExpandedByDefault: Boolean = mode == ComposeWorkspaceMode.OFFLINE
    val videoStageVisible: Boolean = mode == ComposeWorkspaceMode.VIDEO_CALL
    val callBannerVisible: Boolean = mode == ComposeWorkspaceMode.VOICE_CALL
    val inlineTransferVisible: Boolean = mode == ComposeWorkspaceMode.FILE_TRANSFER
    val rightPanelMode: RightPanelMode = when (mode) {
        ComposeWorkspaceMode.OFFLINE -> RightPanelMode.HIDDEN
        ComposeWorkspaceMode.HOSTING, ComposeWorkspaceMode.CONNECTED -> RightPanelMode.ROOM_INFO
        ComposeWorkspaceMode.PEER_SELECTED -> RightPanelMode.PEER_INFO
        ComposeWorkspaceMode.FILE_TRANSFER -> RightPanelMode.TRANSFERS
        ComposeWorkspaceMode.VOICE_CALL, ComposeWorkspaceMode.VIDEO_CALL -> RightPanelMode.CALL
    }

    companion object {
        fun from(
            statusState: ComposeStatusConnectionState,
            peerState: ComposePeerListState,
            transferState: ComposeFileTransferState,
            voiceState: ComposeMediaVoiceState,
            videoState: ComposeExperimentalVideoState,
        ): ComposeWorkspaceState {
            val mode = when {
                videoState.currentSession != null -> ComposeWorkspaceMode.VIDEO_CALL
                voiceState.currentSession != null -> ComposeWorkspaceMode.VOICE_CALL
                transferState.activeCount > 0 || transferState.waitingPromptCount > 0 -> ComposeWorkspaceMode.FILE_TRANSFER
                statusState.clientConnected -> if (peerState.selectedPeer != null) ComposeWorkspaceMode.PEER_SELECTED else ComposeWorkspaceMode.CONNECTED
                statusState.localServerRunning -> if (peerState.selectedPeer != null) ComposeWorkspaceMode.PEER_SELECTED else ComposeWorkspaceMode.HOSTING
                else -> ComposeWorkspaceMode.OFFLINE
            }
            val title = when (mode) {
                ComposeWorkspaceMode.OFFLINE -> "Shared room"
                ComposeWorkspaceMode.HOSTING -> "Waiting for peers"
                ComposeWorkspaceMode.CONNECTED -> "Shared room"
                ComposeWorkspaceMode.PEER_SELECTED -> "Shared room"
                ComposeWorkspaceMode.VOICE_CALL -> "Voice call"
                ComposeWorkspaceMode.VIDEO_CALL -> "Video call"
                ComposeWorkspaceMode.FILE_TRANSFER -> "File transfer"
            }
            val subtitle = when (mode) {
                ComposeWorkspaceMode.OFFLINE -> ""
                ComposeWorkspaceMode.HOSTING -> "Waiting for people to join this room."
                ComposeWorkspaceMode.CONNECTED -> ""
                ComposeWorkspaceMode.PEER_SELECTED -> "Ready to message, send files, or start a call."
                ComposeWorkspaceMode.VOICE_CALL -> voiceState.callTransitionLabel
                ComposeWorkspaceMode.VIDEO_CALL -> videoState.callTransitionLabel
                ComposeWorkspaceMode.FILE_TRANSFER -> "Transfer in progress. Details are in the Context Assistant."
            }
            return ComposeWorkspaceState(mode, title, subtitle)
        }
    }
}
