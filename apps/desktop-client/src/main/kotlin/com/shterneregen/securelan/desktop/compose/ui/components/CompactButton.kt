package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

internal enum class CompactButtonTone {
    SECONDARY,
    TERTIARY,
    DESTRUCTIVE,
}

@Composable
internal fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: CompactButtonTone = CompactButtonTone.SECONDARY,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tokens = LocalSecureLanDesignTokens.current
    val backgroundColor = when (tone) {
        CompactButtonTone.SECONDARY -> tokens.colors.surfaceLevel2.copy(alpha = 0.72f)
        CompactButtonTone.TERTIARY -> Color.Transparent
        CompactButtonTone.DESTRUCTIVE -> tokens.colors.error.copy(alpha = 0.12f)
    }
    val contentColor = when (tone) {
        CompactButtonTone.SECONDARY -> tokens.colors.accent
        CompactButtonTone.TERTIARY -> tokens.colors.textSecondary
        CompactButtonTone.DESTRUCTIVE -> tokens.colors.error
    }
    val borderColor = when (tone) {
        CompactButtonTone.SECONDARY -> tokens.colors.accent.copy(alpha = 0.32f)
        CompactButtonTone.TERTIARY -> tokens.colors.borderSubtle.copy(alpha = 0.48f)
        CompactButtonTone.DESTRUCTIVE -> tokens.colors.error.copy(alpha = 0.46f)
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = 30.dp),
        shape = RoundedCornerShape(tokens.radius.medium),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        border = BorderStroke(tokens.border.subtle, borderColor),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            disabledBackgroundColor = tokens.colors.surfaceLevel2.copy(alpha = 0.34f),
            disabledContentColor = tokens.colors.textTertiary.copy(alpha = 0.70f),
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        content = content,
    )
}
