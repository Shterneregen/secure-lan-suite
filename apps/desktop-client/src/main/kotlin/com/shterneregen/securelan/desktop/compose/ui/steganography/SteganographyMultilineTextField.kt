package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun SteganographyMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    isError: Boolean = false,
    height: Dp = 168.dp,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val scrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val fieldState = rememberTextFieldState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val borderColor = when {
        isError -> MaterialTheme.colors.error
        focused -> MaterialTheme.colors.primary
        else -> tokens.colors.borderSubtle
    }

    LaunchedEffect(value) {
        if (fieldState.text.toString() != value) {
            fieldState.edit { replace(0, length, value) }
        }
    }
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.collect(currentOnValueChange)
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(tokens.radius.small),
        border = BorderStroke(if (focused || isError) 2.dp else 1.dp, borderColor),
        color = MaterialTheme.colors.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BasicTextField(
                state = fieldState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 11.dp)
                    .padding(top = 20.dp, end = 8.dp),
                enabled = true,
                readOnly = readOnly,
                interactionSource = interactionSource,
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                decorator = TextFieldDecorator { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (fieldState.text.isEmpty() && !placeholder.isNullOrBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.52f),
                            )
                        }
                        innerTextField()
                    }
                },
                scrollState = scrollState,
            )
            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 5.dp),
                style = MaterialTheme.typography.caption,
                color = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.primary,
            )
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
        }
    }
}
