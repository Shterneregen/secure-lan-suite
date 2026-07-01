package com.shterneregen.securelan.desktop.compose.ui.components

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
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMessageTone

@Composable
internal fun ConnectionStatusBadge(label: String) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        shape = RoundedCornerShape(tokens.radius.pill),
        color = tokens.colors.surfaceLevel2,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
internal fun ConnectionHubStatusMessage(
    text: String,
    tone: ComposeConnectionHubMessageTone,
) {
    val accent = when (tone) {
        ComposeConnectionHubMessageTone.INFO -> MaterialTheme.colors.primary
        ComposeConnectionHubMessageTone.SUCCESS -> LocalSecureLanDesignTokens.current.colors.success
        ComposeConnectionHubMessageTone.ERROR -> MaterialTheme.colors.error
    }
    val backgroundAlpha = when (tone) {
        ComposeConnectionHubMessageTone.INFO -> if (MaterialTheme.colors.isLight) 0.08f else 0.12f
        ComposeConnectionHubMessageTone.SUCCESS -> if (MaterialTheme.colors.isLight) 0.09f else 0.13f
        ComposeConnectionHubMessageTone.ERROR -> if (MaterialTheme.colors.isLight) 0.08f else 0.14f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small),
        color = accent.copy(alpha = backgroundAlpha),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = LocalSecureLanDesignTokens.current.spacing.sm, vertical = LocalSecureLanDesignTokens.current.spacing.xs),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.caption,
            color = accent,
        )
    }
}
