package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
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

internal fun steganographyStatusIsError(status: String): Boolean = listOf(
    "failed",
    "rejected",
    "could not",
    "no supported",
).any { marker -> status.contains(marker, ignoreCase = true) }

@Composable
internal fun SteganographyWorkflowStatus(
    label: String,
    detail: String,
    error: Boolean,
    completed: Boolean,
    ready: Boolean,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val color = when {
        error -> MaterialTheme.colors.error
        completed -> Color(0xFF35B36F)
        ready -> MaterialTheme.colors.primary
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.58f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.medium),
        border = BorderStroke(1.dp, color.copy(alpha = 0.36f)),
        color = color.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(label, style = MaterialTheme.typography.subtitle2, color = color)
            Text(
                detail,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
internal fun SteganographyInfoSurface(
    title: String,
    body: String,
    positive: Boolean,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val tokens = LocalSecureLanDesignTokens.current
    val color = if (positive) MaterialTheme.colors.primary else MaterialTheme.colors.error
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.medium),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.70f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Text(
                body,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
            actions()
        }
    }
}
