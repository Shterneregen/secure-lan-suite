package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.util.calmFocusRing
import com.shterneregen.securelan.desktop.compose.util.rememberInteractiveSurfaceState

@Composable
internal fun ConnectionModeSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    icon: ImageVector? = null,
) {
    val (interactionSource, interactive) = rememberInteractiveSurfaceState(selected = selected, enabled = enabled)
    val tokens = LocalSecureLanDesignTokens.current
    val background = if (selected) {
        MaterialTheme.colors.primary.copy(alpha = 0.92f)
    } else if (!enabled) {
        interactive.backgroundColor.copy(alpha = 0.5f)
    } else {
        interactive.backgroundColor.copy(alpha = if (interactive.hovered || interactive.focused) 1f else 0f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colors.onPrimary
    } else if (!enabled) {
        MaterialTheme.colors.onSurface.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
    }
    Surface(
        modifier = modifier
            .calmFocusRing(interactive.focused, tokens.radius.small)
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(tokens.radius.small),
        color = background,
        border = BorderStroke(
            tokens.border.subtle,
            if (selected) MaterialTheme.colors.primary.copy(alpha = 0.72f) else Color.Transparent,
        ),
    ) {
        Row(
            Modifier.heightIn(min = if (prominent) 48.dp else 30.dp).padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prominent && icon != null) {
                Icon(icon, null, Modifier.size(22.dp), tint = contentColor)
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = if (prominent) MaterialTheme.typography.subtitle1 else MaterialTheme.typography.button,
                color = contentColor, textAlign = TextAlign.Center)
        }
    }
}
