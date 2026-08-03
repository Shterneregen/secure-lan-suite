package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareRow

@Composable
internal fun QuickShareLinkRow(
    row: ComposeQuickShareRow,
    onCopy: (String) -> Unit,
    onOpen: (String) -> Unit,
    onStop: (String) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.small),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        color = tokens.colors.surfaceLevel2,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.subtitle2,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                QuickShareStatusPill(row.statusLabel, active = row.active)
            }
            Text(
                text = "${row.typeLabel} · ${row.detail}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = row.url.ifBlank { "URL unavailable" },
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                QuickShareLinkActions(
                    url = row.url,
                    showQrCode = row.active && row.fileLink,
                    onCopy = { onCopy(row.url) },
                    onOpen = if (row.active) ({ onOpen(row.url) }) else null,
                    onStop = if (row.active) ({ onStop(row.id) }) else null,
                )
            }
        }
    }
}
