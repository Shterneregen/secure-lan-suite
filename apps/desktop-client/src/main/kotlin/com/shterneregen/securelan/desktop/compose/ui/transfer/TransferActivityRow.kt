package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeTransferRow
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.util.MicrointeractionTone
import com.shterneregen.securelan.desktop.compose.util.rememberInteractiveSurfaceState

@Composable
internal fun TransferActivityRow(row: ComposeTransferRow) {
    val tokens = LocalSecureLanDesignTokens.current
    val tone = when {
        row.failed -> MicrointeractionTone.FAILURE
        row.active -> MicrointeractionTone.LOADING
        row.completed -> MicrointeractionTone.SUCCESS
        else -> MicrointeractionTone.NEUTRAL
    }
    val accent = when (tone) {
        MicrointeractionTone.FAILURE -> tokens.colors.error
        MicrointeractionTone.LOADING -> tokens.colors.accent
        MicrointeractionTone.SUCCESS -> tokens.colors.success
        MicrointeractionTone.NEUTRAL -> tokens.colors.textSecondary
    }
    val progress by animateFloatAsState(
        targetValue = row.percent.coerceIn(0, 100) / 100f,
        animationSpec = motionTween<Float>(durationMillis = tokens.motion.durationDefault),
        label = "TransferProgress",
    )
    val (_, interactive) = rememberInteractiveSurfaceState(tone = tone)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.small),
        border = BorderStroke(tokens.border.subtle, interactive.borderColor.copy(alpha = 0.55f)),
        color = interactive.backgroundColor.copy(alpha = if (row.active || row.completed || row.failed) 0.74f else 0.42f),
    ) {
    Column(
        modifier = Modifier.padding(tokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.title, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
            Text(row.progressLabel, style = MaterialTheme.typography.caption, color = accent)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = accent,
            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.10f),
        )
        Text(
            row.detail,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
        )
    }
    }
}
