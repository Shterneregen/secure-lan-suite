package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeFileTransferState

@Composable
internal fun FileTransferPreviewCard(state: ComposeFileTransferState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Transfers", style = MaterialTheme.typography.subtitle1)
        if (state.entryRows.isEmpty()) {
            Text(
                text = state.heroTitle,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
            )
            Text(
                text = state.heroSubtitle,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Text(
                text = state.nextStepSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
            )
        } else {
            Text(
                text = state.hint,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Text(
                text = state.entryRows.joinToString(" · "),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}
