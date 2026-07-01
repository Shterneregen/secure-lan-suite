package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeChatAttachmentCard

@Composable
internal fun ChatAttachmentCardRow(card: ComposeChatAttachmentCard) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
        border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
        color = if (card.needsDecision) MaterialTheme.colors.primary.copy(alpha = 0.10f) else LocalSecureLanDesignTokens.current.colors.surfaceLevel2.copy(
            alpha = 0.72f
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(card.title, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                Text(
                    card.progressLabel,
                    style = MaterialTheme.typography.caption,
                    color = if (card.failed) MaterialTheme.colors.error else MaterialTheme.colors.primary
                )
            }
            Text(
                card.subtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f)
            )
            if (!card.needsDecision) {
                LinearProgressIndicator(
                    progress = card.progressPercent.coerceIn(0, 100) / 100f,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = if (card.failed) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.10f),
                )
            }
        }
    }
}
