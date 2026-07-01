package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.components.InlineEmptyState

@Composable
internal fun RecentTransfersPanel(transferState: ComposeFileTransferState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Recent transfer activity", style = MaterialTheme.typography.subtitle2)
        TransferCompletionFeedback(transferState)
        if (transferState.recentEntryRows.isEmpty()) {
            InlineEmptyState(
                situation = transferState.recentEmptySituation,
                explanation = transferState.recentEmptyExplanation,
                nextAction = transferState.recentEmptyNextAction,
            )
        } else {
            transferState.recentEntryRows.forEach { row -> TransferActivityRow(row) }
        }
    }
}
