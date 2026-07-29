package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun StatusChip(text: String) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        shape = RoundedCornerShape(tokens.radius.pill),
        color = tokens.colors.surfaceLevel2,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIndicator(text)
            Text(text, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
internal fun StatusIndicator(text: String) {
    val color = when {
        text.contains("running", ignoreCase = true) || text.contains(
            "connected",
            ignoreCase = true
        ) || text.contains(
            "hosting",
            ignoreCase = true
        ) || text.contains(
            "online",
            ignoreCase = true
        ) -> MaterialTheme.colors.primary

        text.contains("error", ignoreCase = true) || text.contains(
            "failed",
            ignoreCase = true
        ) -> MaterialTheme.colors.error

        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
    }
    Canvas(modifier = Modifier.size(7.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2f, center = Offset(size.width / 2f, size.height / 2f))
    }
}
