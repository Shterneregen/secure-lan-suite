package com.shterneregen.securelan.desktop.compose.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween

internal enum class MicrointeractionTone {
    NEUTRAL,
    LOADING,
    SUCCESS,
    FAILURE,
}

internal enum class FocusRingEmphasis {
    CALM,
    CONTROL,
}

@Composable
internal fun Modifier.calmFocusRing(
    focused: Boolean,
    shapeRadius: androidx.compose.ui.unit.Dp,
    emphasis: FocusRingEmphasis = FocusRingEmphasis.CALM,
): Modifier {
    if (!focused) return this
    val focusColor = LocalSecureLanDesignTokens.current.colors.borderFocus
    return drawWithContent {
        drawContent()
        val strokeWidth = when (emphasis) {
            FocusRingEmphasis.CALM -> 1.dp
            FocusRingEmphasis.CONTROL -> 1.5.dp
        }.toPx()
        val inset = strokeWidth / 2f
        val radius = (shapeRadius - 1.dp).coerceAtLeast(1.dp).toPx()
        drawRoundRect(
            color = focusColor,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
internal fun rememberInteractiveSurfaceState(
    selected: Boolean = false,
    enabled: Boolean = true,
    tone: MicrointeractionTone = MicrointeractionTone.NEUTRAL,
): Pair<MutableInteractionSource, InteractiveSurfaceState> {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    return interactionSource to rememberInteractiveSurfaceState(
        hovered = hovered,
        focused = focused,
        pressed = pressed,
        selected = selected,
        enabled = enabled,
        tone = tone,
    )
}

@Composable
internal fun rememberInteractiveSurfaceState(
    hovered: Boolean,
    focused: Boolean,
    pressed: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    tone: MicrointeractionTone = MicrointeractionTone.NEUTRAL,
): InteractiveSurfaceState {
    val tokens = LocalSecureLanDesignTokens.current
    val accent = when (tone) {
        MicrointeractionTone.NEUTRAL, MicrointeractionTone.LOADING -> tokens.colors.accent
        MicrointeractionTone.SUCCESS -> tokens.colors.success
        MicrointeractionTone.FAILURE -> tokens.colors.error
    }
    val baseColor = when {
        !enabled -> tokens.colors.surfaceLevel2.copy(alpha = 0.48f)
        selected -> accent.copy(alpha = if (MaterialTheme.colors.isLight) 0.11f else 0.16f)
        tone == MicrointeractionTone.LOADING -> tokens.colors.warning.copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.12f)
        tone == MicrointeractionTone.SUCCESS -> tokens.colors.success.copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.12f)
        tone == MicrointeractionTone.FAILURE -> tokens.colors.error.copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.13f)
        hovered -> tokens.colors.surfaceLevel3.copy(alpha = if (MaterialTheme.colors.isLight) 0.82f else 0.72f)
        else -> tokens.colors.surfaceLevel2
    }
    val targetColor = when {
        !enabled -> baseColor
        pressed -> accent.copy(alpha = if (MaterialTheme.colors.isLight) 0.17f else 0.22f)
        hovered && tone != MicrointeractionTone.NEUTRAL -> accent.copy(alpha = if (MaterialTheme.colors.isLight) 0.12f else 0.18f)
        else -> baseColor
    }
    val targetBorder = when {
        selected || tone != MicrointeractionTone.NEUTRAL -> accent.copy(alpha = if (focused) 0.58f else 0.50f)
        hovered -> tokens.colors.borderFocus.copy(alpha = 0.34f)
        else -> tokens.colors.borderSubtle
    }
    return InteractiveSurfaceState(
        backgroundColor = animateColorAsState(
            targetValue = targetColor,
            animationSpec = motionTween<Color>(durationMillis = tokens.motion.durationFast),
            label = "InteractiveSurfaceBackground",
        ).value,
        borderColor = animateColorAsState(
            targetValue = targetBorder,
            animationSpec = motionTween<Color>(durationMillis = tokens.motion.durationFast),
            label = "InteractiveSurfaceBorder",
        ).value,
        contentColor = if (enabled) tokens.colors.textPrimary else tokens.colors.textTertiary,
        pressed = pressed,
        hovered = hovered,
        focused = focused,
    )
}

internal data class InteractiveSurfaceState(
    val backgroundColor: Color,
    val borderColor: Color,
    val contentColor: Color,
    val pressed: Boolean,
    val hovered: Boolean,
    val focused: Boolean,
)

@Composable
internal fun interactiveSurfaceBorder(
    interactive: InteractiveSurfaceState,
    neutralWidth: androidx.compose.ui.unit.Dp = LocalSecureLanDesignTokens.current.border.subtle,
): BorderStroke = BorderStroke(neutralWidth, interactive.borderColor)
