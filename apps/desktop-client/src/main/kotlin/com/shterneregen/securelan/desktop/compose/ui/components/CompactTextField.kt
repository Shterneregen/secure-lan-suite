package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.util.FocusRingEmphasis
import com.shterneregen.securelan.desktop.compose.util.calmFocusRing
import com.shterneregen.securelan.desktop.compose.util.interactiveSurfaceBorder
import com.shterneregen.securelan.desktop.compose.util.rememberInteractiveSurfaceState

@Composable
internal fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    onSubmit: (() -> Unit)? = null,
    placeholder: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    outlined: Boolean = false,
    // Keep focus/semantics on the editable node, independently of optional trailing actions.
    inputModifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val interactive = rememberInteractiveSurfaceState(
        hovered = hovered,
        focused = focused,
        enabled = enabled,
    )
    Row(
        // Handle Enter before the focused BasicTextField consumes the key event.
        modifier = modifier.then(
            if (onSubmit == null) Modifier else Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onSubmit()
                    true
                } else {
                    false
                }
            },
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface)
        }
        val fieldRadius = LocalSecureLanDesignTokens.current.radius.small
        Surface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 34.dp)
                .calmFocusRing(interactive.focused, fieldRadius, FocusRingEmphasis.CONTROL),
            shape = RoundedCornerShape(fieldRadius),
            border = if (outlined) androidx.compose.foundation.BorderStroke(
                LocalSecureLanDesignTokens.current.border.subtle,
                if (interactive.focused) MaterialTheme.colors.primary else LocalSecureLanDesignTokens.current.colors.textSecondary.copy(alpha = if (enabled) 0.65f else 0.3f),
            ) else interactiveSurfaceBorder(interactive),
            color = if (outlined && !interactive.hovered) LocalSecureLanDesignTokens.current.colors.surfaceLevel1 else interactive.backgroundColor,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(inputModifier)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                enabled = enabled,
                singleLine = true,
                // IME Send may arrive without a hardware Enter key event.
                keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Send else ImeAction.Default),
                keyboardActions = KeyboardActions(onSend = { onSubmit?.invoke() }),
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                visualTransformation = visualTransformation,
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        leadingContent?.invoke()
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                                )
                            }
                            innerTextField()
                        }
                        trailingContent?.invoke()
                    }
                },
            )
        }
    }
}
