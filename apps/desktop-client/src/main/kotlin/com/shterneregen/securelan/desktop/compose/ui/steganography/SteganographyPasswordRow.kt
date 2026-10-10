package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField

@Composable
internal fun SteganographyPasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    placeholder: String,
    emptyHint: String,
    protectedHint: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        CompactTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password (optional)",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            placeholder = placeholder,
        )
        Text(
            text = if (password.isBlank()) emptyHint else protectedHint,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
        )
    }
}
