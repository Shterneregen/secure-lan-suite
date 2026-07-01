package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.util.MicrointeractionTone

@Composable
internal fun MicroFeedbackPill(
    text: String,
    tone: MicrointeractionTone,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val accent = when (tone) {
        MicrointeractionTone.NEUTRAL -> tokens.colors.textSecondary
        MicrointeractionTone.LOADING -> tokens.colors.warning
        MicrointeractionTone.SUCCESS -> tokens.colors.success
        MicrointeractionTone.FAILURE -> tokens.colors.error
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radius.pill),
        border = BorderStroke(tokens.border.subtle, accent.copy(alpha = 0.34f)),
        color = accent.copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.13f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(accent, tone == MicrointeractionTone.LOADING)
            Text(text, style = MaterialTheme.typography.caption, color = accent)
        }
    }
}

@Composable
internal fun StatusDot(color: Color, loading: Boolean = false) {
    val alpha by animateFloatAsState(
        targetValue = if (loading) 0.48f else 1f,
        animationSpec = motionTween<Float>(durationMillis = LocalSecureLanDesignTokens.current.motion.durationSlow),
        label = "StatusDotAlpha",
    )
    Canvas(modifier = Modifier.size(7.dp)) {
        drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension / 2f, center = Offset(size.width / 2f, size.height / 2f))
    }
}
