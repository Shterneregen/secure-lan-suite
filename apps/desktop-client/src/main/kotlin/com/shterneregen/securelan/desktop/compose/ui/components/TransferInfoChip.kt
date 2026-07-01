package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun TransferInfoChip(text: String) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        shape = RoundedCornerShape(tokens.radius.pill),
        color = tokens.colors.surfaceLevel2,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
        )
    }
}

@Composable
internal fun StatusPill(text: String) {
    Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
        )
    }
}
