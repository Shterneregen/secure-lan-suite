package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.ComposeAdvancedPane

@Composable
internal fun SendEncryptedFilePanel(
    transferState: ComposeFileTransferState,
    filePath: String,
    onChooseFile: () -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
) {
    ComposeAdvancedPane("Send encrypted file") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Send to ${transferState.selectedPeerName}", style = MaterialTheme.typography.subtitle2)
                Text(
                    transferState.targetSummary,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 360.dp
                if (compact) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SelectedFileSummary(
                            filePath = filePath,
                            fallbackSummary = transferState.selectedFileSummary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        CompactButton(
                            onClick = onChooseFile,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (filePath.isBlank()) "Choose file" else "Choose another file") }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SelectedFileSummary(
                            filePath = filePath,
                            fallbackSummary = transferState.selectedFileSummary,
                            modifier = Modifier.weight(1f),
                        )
                        CompactButton(onClick = onChooseFile) { Text("Browse") }
                    }
                }
            }
            Text(
                "Uses the secure connection and room password already active for this peer.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
                border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
                color = if (sendEnabled) {
                    MaterialTheme.colors.primary.copy(alpha = 0.08f)
                } else {
                    LocalSecureLanDesignTokens.current.colors.surfaceLevel2
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        transferState.nextStepSummary,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
                    )
                    CompactButton(
                        onClick = onSend,
                        enabled = sendEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Send encrypted file") }
                }
            }
        }
    }
}
