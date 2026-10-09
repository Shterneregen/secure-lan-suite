package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.components.TitleWithHelp
import com.shterneregen.securelan.desktop.compose.ui.components.ComposeAdvancedPane

@Composable
internal fun RecentTransfersPanel(transferState: ComposeFileTransferState) {
    var historyExpanded by remember { mutableStateOf(false) }
    if (transferState.activeEntryRows.isEmpty() && transferState.recentEntryRows.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (transferState.activeEntryRows.isNotEmpty()) {
            TitleWithHelp(
                title = "Active transfers",
                tooltip = "Files currently being transferred.",
            )
            transferState.activeEntryRows.forEach { row -> TransferActivityRow(row) }
        }
        if (transferState.recentEntryRows.isNotEmpty()) {
            ComposeAdvancedPane(
                title = "Recent files · ${transferState.recentEntryRows.size}",
                expanded = historyExpanded,
                onExpandedChange = { historyExpanded = it },
            ) {
                transferState.recentEntryRows.forEach { row -> TransferActivityRow(row) }
            }
        }
    }
}
