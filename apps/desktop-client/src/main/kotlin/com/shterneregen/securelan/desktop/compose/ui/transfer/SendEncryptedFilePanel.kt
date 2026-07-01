package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.ComposeAdvancedPane

@Composable
internal fun SendEncryptedFilePanel(
    transferState: ComposeFileTransferState,
    filePath: String,
    onFilePathChange: (String) -> Unit,
    onChooseFile: () -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
) {
    ComposeAdvancedPane("Send encrypted file") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Send to ${transferState.selectedPeerName}", style = MaterialTheme.typography.subtitle2)
                Text(
                    transferState.targetSummary,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("File", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface)
                SelectedFileSummary(
                    filePath = filePath,
                    fallbackSummary = transferState.selectedFileSummary,
                    modifier = Modifier.weight(1f),
                )
                CompactButton(onClick = onChooseFile) { Text("Browse") }
            }
            Text(
                "Sender and encryption password are reused from the current room connection.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
                border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
                color = if (sendEnabled) MaterialTheme.colors.primary.copy(alpha = 0.08f) else LocalSecureLanDesignTokens.current.colors.surfaceLevel2,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        transferState.nextStepSummary,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
                        modifier = Modifier.weight(1f),
                    )
                    CompactButton(
                        onClick = onSend,
                        enabled = sendEnabled,
                        modifier = Modifier.widthIn(min = 132.dp)
                    ) { Text("Send encrypted file") }
                }
            }
        }
    }
}
