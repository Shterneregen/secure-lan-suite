package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CalmFocusButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ThemeToggleButton(
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    val actionLabel = if (darkTheme) "Switch to light theme" else "Switch to dark theme"
    val tokens = LocalSecureLanDesignTokens.current
    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(tokens.radius.small),
                border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                color = MaterialTheme.colors.surface,
            ) {
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
                )
            }
        },
    ) {
        CalmFocusButton(
            onClick = onThemeToggle,
            modifier = Modifier
                .heightIn(min = 26.dp)
                .semantics { contentDescription = actionLabel },
        ) {
            Icon(
                imageVector = if (darkTheme) SecureLanIcons.LightTheme else SecureLanIcons.DarkTheme,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
