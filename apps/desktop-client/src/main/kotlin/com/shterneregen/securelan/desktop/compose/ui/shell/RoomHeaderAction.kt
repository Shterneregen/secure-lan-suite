package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

/** Consistent outlined controls for the shared-room header, including its disclosure. */
@Composable
internal fun RoomHeaderAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    destructive: Boolean = false,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val accent = if (destructive) tokens.colors.error else tokens.colors.accent
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier.heightIn(min = 38.dp).semantics {
            expanded?.let { stateDescription = if (it) "Expanded" else "Collapsed" }
        },
        shape = RoundedCornerShape(tokens.radius.small),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        border = BorderStroke(tokens.border.subtle, when {
            focused -> tokens.colors.borderFocus
            expanded == true || hovered -> accent.copy(alpha = 0.64f)
            else -> accent.copy(alpha = if (enabled) 0.32f else 0.12f)
        }),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = when {
                pressed -> accent.copy(alpha = 0.20f)
                expanded == true -> accent.copy(alpha = 0.14f)
                hovered -> accent.copy(alpha = 0.08f)
                else -> Color.Transparent
            },
            contentColor = if (destructive) tokens.colors.error else tokens.colors.textSecondary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = tokens.colors.textTertiary,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium), maxLines = 1)
    }
}
