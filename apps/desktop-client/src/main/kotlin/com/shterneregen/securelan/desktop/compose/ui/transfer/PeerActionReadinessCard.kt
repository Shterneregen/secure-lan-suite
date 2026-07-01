package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.runtime.Composable
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ComposePeerListState

@Composable
internal fun PeerActionReadinessCard(peerState: ComposePeerListState, hostAdapter: ComposeDesktopHostAdapter) {
    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        senderId = hostAdapter.statusState.nickname,
        sessionPassword = hostAdapter.currentRoomPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles,
    )
    PeerActionReadinessPreviewCard(transferState)
}
