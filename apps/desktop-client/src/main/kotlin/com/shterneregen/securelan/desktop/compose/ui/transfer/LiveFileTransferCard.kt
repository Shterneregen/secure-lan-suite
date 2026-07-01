package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.ui.components.SubtleContentSurface
import com.shterneregen.securelan.desktop.compose.util.openComposeFileChooser
import java.nio.file.Path

@Composable
internal fun LiveFileTransferCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    var filePath by remember { mutableStateOf("") }
    val autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles
    val selectedPeer = peerState.selectedPeer
        ?.takeIf { it.online }
        ?.let { selected -> hostAdapter.discoveredPeerFor(selected.nickname) }
    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        selectedFilePath = filePath,
        senderId = hostAdapter.statusState.nickname,
        sessionPassword = hostAdapter.currentRoomPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = autoAcceptFiles,
    )

    SubtleContentSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TransferHeroPanel(transferState)
            ReceiveModePanel(
                transferState = transferState,
                autoAcceptFiles = autoAcceptFiles,
                onAutoAcceptChanged = hostAdapter::updateAutoAcceptIncomingFiles,
            )
            RecentTransfersPanel(transferState)
            val waitingPrompts = transferState.incomingPrompts.filter { it.waitingForDecision }
            val recentDecisions = transferState.incomingPrompts.filterNot { it.waitingForDecision }.takeLast(3)
            if (waitingPrompts.isNotEmpty() || recentDecisions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    waitingPrompts.forEach { prompt ->
                        IncomingTransferPromptRow(prompt, hostAdapter)
                    }
                    recentDecisions.forEach { prompt ->
                        Text(
                            "${prompt.statusLabel}: ${prompt.fileName} from ${prompt.senderId}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                        )
                    }
                }
            }
            SendEncryptedFilePanel(
                transferState = transferState,
                filePath = filePath,
                onFilePathChange = { filePath = it },
                onChooseFile = {
                    openComposeFileChooser("Choose file to send to ${transferState.selectedPeerName}")?.let {
                        filePath = it.toString()
                    }
                },
                onSend = {
                    val peer = selectedPeer ?: return@SendEncryptedFilePanel
                    hostAdapter.sendFileToPeer(
                        Path.of(filePath),
                        hostAdapter.statusState.nickname,
                        peer,
                        hostAdapter.currentRoomPassword
                    )
                },
                sendEnabled = transferState.canSendSelectedFile && selectedPeer != null,
            )
        }
    }
}
