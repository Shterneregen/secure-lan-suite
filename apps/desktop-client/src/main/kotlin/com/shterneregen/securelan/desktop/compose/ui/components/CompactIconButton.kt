package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.util.interactiveSurfaceBorder
import com.shterneregen.securelan.desktop.compose.util.rememberInteractiveSurfaceState

/**
 * Small, focusable icon button for compact inline actions such as text-field adornments.
 *
 * Uses project design tokens, a visible focus ring, and a semantic content description.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CompactIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val (interactionSource, interactive) = rememberInteractiveSurfaceState(enabled = enabled)
    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(tokens.radius.small),
                border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                color = MaterialTheme.colors.surface,
            ) {
                Text(
                    text = contentDescription,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
                )
            }
        },
    ) {
        Surface(
            modifier = modifier
                .size(28.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics { this.contentDescription = contentDescription },
            shape = RoundedCornerShape(tokens.radius.small),
            border = interactiveSurfaceBorder(interactive),
            color = interactive.backgroundColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = interactive.contentColor,
                )
            }
        }
    }
}
