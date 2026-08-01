package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.components.ComposeAdvancedPane

@Composable
internal fun ReceiveModePanel(
    transferState: ComposeFileTransferState,
    autoAcceptFiles: Boolean,
    onAutoAcceptChanged: (Boolean) -> Unit,
) {
    ComposeAdvancedPane(
        title = "Incoming files",
        tooltip = transferState.receiveModeDescription,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = autoAcceptFiles,
                    role = Role.Checkbox,
                    onValueChange = onAutoAcceptChanged,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = autoAcceptFiles, onCheckedChange = null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "${transferState.receiveModeLabel} (${transferState.receiveModeStatusLabel})",
                    style = MaterialTheme.typography.body2,
                    color = if (autoAcceptFiles) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface
                    },
                )
                Text(
                    text = transferState.receiveModeSupportingText,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                )
            }
        }
    }
}
