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
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.ComposeIncomingTransferPrompt
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton

@Composable
internal fun IncomingTransferPromptRow(
    prompt: ComposeIncomingTransferPrompt,
    hostAdapter: ComposeDesktopHostAdapter,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
        border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(prompt.header, style = MaterialTheme.typography.subtitle2)
                Text(
                    "${prompt.fileName} · ${prompt.sizeLabel} · ${prompt.remoteAddress}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            CompactButton(
                onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, true) },
                modifier = Modifier.widthIn(min = 76.dp)
            ) { Text("Accept") }
            CompactButton(
                onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, false) },
                modifier = Modifier.widthIn(min = 76.dp)
            ) { Text("Decline") }
        }
    }
}
