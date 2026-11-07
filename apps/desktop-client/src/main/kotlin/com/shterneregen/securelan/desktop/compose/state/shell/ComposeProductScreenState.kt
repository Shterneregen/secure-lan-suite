package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem

data class ComposeProductScreenState(
    val appMode: AppMode = AppMode.WELCOME,
    val roomState: RoomState = RoomState.OFFLINE,
    val selectionState: SelectionState = SelectionState.NONE,
    val rightPanelMode: RightPanelMode = RightPanelMode.HIDDEN,
) {
    companion object {
        fun from(
            statusState: ComposeStatusConnectionState,
            requestedAppMode: AppMode = AppMode.WELCOME,
            connectionHubMode: ComposeConnectionHubMode = ComposeConnectionHubMode.HOST,
            selectedPeer: ComposePeerListItem? = null,
            activeTransfer: Boolean = false,
            activeCall: Boolean = false,
            settingsRequested: Boolean = false,
            issueDetected: Boolean = false,
        ): ComposeProductScreenState {
            val runtimeConnected = statusState.clientConnected || statusState.localServerRunning
            val effectiveMode = when {
                settingsRequested -> AppMode.SETTINGS
                runtimeConnected -> AppMode.MESSENGER
                requestedAppMode == AppMode.HOST_SETUP || requestedAppMode == AppMode.JOIN_SETUP -> requestedAppMode
                connectionHubMode == ComposeConnectionHubMode.HOST && requestedAppMode == AppMode.HOST_SETUP -> AppMode.HOST_SETUP
                connectionHubMode == ComposeConnectionHubMode.JOIN && requestedAppMode == AppMode.JOIN_SETUP -> AppMode.JOIN_SETUP
                else -> AppMode.WELCOME
            }
            val roomState = when {
                issueDetected -> RoomState.ISSUE
                statusState.clientConnected -> RoomState.CONNECTED
                statusState.localServerRunning -> if (selectedPeer != null) RoomState.CONNECTED else RoomState.WAITING_FOR_PEERS
                requestedAppMode == AppMode.HOST_SETUP -> RoomState.HOSTING
                else -> RoomState.OFFLINE
            }
            val selectionState = when {
                activeCall -> SelectionState.CALL
                activeTransfer -> SelectionState.TRANSFER
                selectedPeer != null -> SelectionState.PEER
                effectiveMode == AppMode.MESSENGER -> SelectionState.ROOM_CONVERSATION
                else -> SelectionState.NONE
            }
            val rightPanelMode = when {
                activeCall -> RightPanelMode.CALL
                activeTransfer -> RightPanelMode.TRANSFERS
                selectedPeer != null -> RightPanelMode.PEER_INFO
                effectiveMode == AppMode.MESSENGER -> RightPanelMode.ROOM_INFO
                else -> RightPanelMode.HIDDEN
            }
            return ComposeProductScreenState(effectiveMode, roomState, selectionState, rightPanelMode)
        }
    }
}
