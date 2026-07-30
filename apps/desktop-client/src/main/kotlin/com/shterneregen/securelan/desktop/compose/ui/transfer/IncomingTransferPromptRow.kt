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
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeIncomingTransferPrompt
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton

@Composable
internal fun IncomingTransferPromptRow(
    prompt: ComposeIncomingTransferPrompt,
    hostAdapter: ComposeDesktopHostAdapter,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.medium),
        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.30f)),
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            val compact = maxWidth < 390.dp
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(prompt.header, style = MaterialTheme.typography.subtitle2)
                    Text(
                        "${prompt.fileName} · ${prompt.sizeLabel} · ${prompt.remoteAddress}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactButton(
                        onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, true) },
                        modifier = if (compact) Modifier.weight(1f) else Modifier,
                    ) { Text("Accept") }
                    CompactButton(
                        onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, false) },
                        modifier = if (compact) Modifier.weight(1f) else Modifier,
                    ) { Text("Decline") }
                }
            }
        }
    }
}
