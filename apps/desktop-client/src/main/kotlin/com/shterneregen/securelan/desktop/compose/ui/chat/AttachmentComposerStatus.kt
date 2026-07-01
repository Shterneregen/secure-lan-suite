package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun AttachmentComposerStatus(statusText: String) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = tokens.spacing.xxs),
        shape = RoundedCornerShape(tokens.radius.small),
        border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle),
        color = tokens.colors.surfaceLevel1,
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
        )
    }
}
