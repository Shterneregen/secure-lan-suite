package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata

@Composable
internal fun InlineEmptyState(
    situation: String,
    explanation: String,
    nextAction: String? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = ComposeShellMetadata.INLINE_EMPTY_STATE_MIN_HEIGHT),
        shape = RoundedCornerShape(tokens.radius.small),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.38f)),
        color = MaterialTheme.colors.onSurface.copy(alpha = if (MaterialTheme.colors.isLight) 0.022f else 0.040f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
        ) {
            Text(
                text = situation,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (nextAction != null) {
                Text(
                    text = nextAction,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                )
            }
        }
    }
}
