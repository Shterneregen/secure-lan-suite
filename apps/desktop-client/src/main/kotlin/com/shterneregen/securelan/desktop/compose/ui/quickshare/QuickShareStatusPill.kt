package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun QuickShareStatusPill(text: String, active: Boolean) {
    val color = when {
        active -> MaterialTheme.colors.primary
        text.contains("expired", ignoreCase = true) || text.contains(
            "limit",
            ignoreCase = true
        ) -> Color(0xFFF59E0B)

        text.contains("stopped", ignoreCase = true) -> MaterialTheme.colors.onSurface.copy(alpha = 0.52f)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.60f)
    }
    Surface(
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.pill),
        border = BorderStroke(1.dp, color.copy(alpha = 0.36f)),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = color,
        )
    }
}
