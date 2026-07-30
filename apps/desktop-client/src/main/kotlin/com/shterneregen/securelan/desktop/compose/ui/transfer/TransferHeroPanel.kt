package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.components.TransferInfoChip

@Composable
internal fun TransferHeroPanel(transferState: ComposeFileTransferState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(transferState.heroTitle, style = MaterialTheme.typography.subtitle2)
        if (transferState.heroSubtitle != transferState.peerListState.selectedPeerMeta) {
            Text(
                transferState.heroSubtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TransferInfoChip(transferState.transferCountSummary)
            TransferInfoChip(transferState.receiveModeShortLabel)
        }
    }
}
