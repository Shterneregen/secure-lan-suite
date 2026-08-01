package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.components.TitleWithHelp

@Composable
internal fun RecentTransfersPanel(transferState: ComposeFileTransferState) {
    if (transferState.recentEntryRows.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TitleWithHelp(
            title = "Recent transfers",
            tooltip = "Shows active transfers and the four most recent completed or failed files.",
        )
        transferState.recentEntryRows.forEach { row -> TransferActivityRow(row) }
    }
}
