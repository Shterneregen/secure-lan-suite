package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ui.components.CalmFocusButton

@Composable
internal fun ThemeToggleButton(
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    CalmFocusButton(
        onClick = onThemeToggle,
        modifier = Modifier.heightIn(min = 26.dp),
    ) {
        Text(if (darkTheme) "Dark theme" else "Light theme", style = MaterialTheme.typography.button)
    }
}
