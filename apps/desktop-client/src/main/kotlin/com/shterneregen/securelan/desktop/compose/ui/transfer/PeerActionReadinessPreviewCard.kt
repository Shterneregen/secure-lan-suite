package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.components.SubtleContentSurface
import com.shterneregen.securelan.desktop.compose.ui.components.TransferInfoChip

@Composable
internal fun PeerActionReadinessPreviewCard(transferState: ComposeFileTransferState) {
    SubtleContentSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(transferState.peerListState.noPeerActionTitle, style = MaterialTheme.typography.subtitle2)
            Text(
                text = transferState.peerListState.noPeerActionDetail,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransferInfoChip(transferState.transferCountSummary)
                TransferInfoChip(transferState.receiveModeShortLabel)
            }
        }
    }
}
