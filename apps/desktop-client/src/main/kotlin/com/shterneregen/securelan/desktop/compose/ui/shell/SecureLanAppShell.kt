package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeAppShellState
import com.shterneregen.securelan.desktop.compose.ui.components.StatusChip

@Composable
internal fun SecureLanAppShell(
    shellState: ComposeAppShellState,
    topBarLabel: String = shellState.currentContextLabel,
    topBarStatus: String = "",
    themeMode: SecureLanThemeMode,
    onSettingsClick: () -> Unit,
    onThemeToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp, max = 56.dp),
            shape = RoundedCornerShape(tokens.radius.large),
            border = BorderStroke(1.dp, tokens.colors.borderSubtle),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.surface,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = topBarLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.subtitle2,
                    )
                    if (topBarStatus.isNotBlank()) {
                        StatusChip(topBarStatus)
                    }
                }
                SettingsButton(onClick = onSettingsClick)
                ThemeToggleButton(themeMode = themeMode, onThemeToggle = onThemeToggle)
            }
        }
        content()
    }
}
