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
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.components.HelpTooltip

@Composable
internal fun SendEncryptedFilePanel(
    transferState: ComposeFileTransferState,
    filePath: String,
    onChooseFile: () -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val targetAccent = if (transferState.selectedPeer == null) {
            tokens.colors.warning
        } else {
            MaterialTheme.colors.primary
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Send to", style = MaterialTheme.typography.subtitle2)
            Surface(
                shape = RoundedCornerShape(tokens.radius.pill),
                border = BorderStroke(1.dp, targetAccent.copy(alpha = 0.55f)),
                color = targetAccent.copy(alpha = 0.10f),
            ) {
                Text(
                    text = transferState.selectedPeerName,
                    modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
                    style = MaterialTheme.typography.subtitle2,
                    color = targetAccent,
                )
            }
            HelpTooltip(
                if (transferState.selectedPeer == null) {
                    "Select an online peer before choosing and sending a file."
                } else {
                    "Files are encrypted using the secure connection and the current room password."
                }
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(tokens.radius.medium),
            color = tokens.colors.surfaceLevel1.copy(alpha = 0.72f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(tokens.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 360.dp
                    if (filePath.isBlank()) {
                        CompactButton(
                            onClick = onChooseFile,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Choose file") }
                    } else if (compact) {
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
                                tone = CompactButtonTone.TERTIARY,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Choose another file") }
                            CompactButton(
                                onClick = onSend,
                                enabled = sendEnabled,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Send file") }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectedFileSummary(
                                filePath = filePath,
                                fallbackSummary = transferState.selectedFileSummary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CompactButton(
                                    onClick = onChooseFile,
                                    tone = CompactButtonTone.TERTIARY,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Change") }
                                CompactButton(
                                    onClick = onSend,
                                    enabled = sendEnabled,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Send file") }
                            }
                        }
                    }
                }
                if (filePath.isNotBlank() && !sendEnabled) {
                    Text(
                        transferState.nextStepSummary,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
                    )
                }
            }
        }
    }
}
