package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState

@Composable
internal fun LiveFileTransferCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles
    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        senderId = hostAdapter.statusState.nickname,
        sessionPassword = hostAdapter.currentRoomPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = autoAcceptFiles,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val waitingPrompts = transferState.incomingPrompts.filter { it.waitingForDecision }
        if (waitingPrompts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                waitingPrompts.forEach { prompt ->
                    IncomingTransferPromptRow(prompt, hostAdapter)
                }
            }
        }
        if (transferState.hasRecentTransfers) {
            RecentTransfersPanel(transferState)
        }
        ReceiveModePanel(
            transferState = transferState,
            autoAcceptFiles = autoAcceptFiles,
            onAutoAcceptChanged = hostAdapter::updateAutoAcceptIncomingFiles,
        )
    }
}
