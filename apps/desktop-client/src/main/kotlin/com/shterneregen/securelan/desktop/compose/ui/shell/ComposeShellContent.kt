package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import com.shterneregen.securelan.desktop.compose.settings.DesktopAppSettingsController

@Composable
internal fun ComposeShellContent(
    hostAdapter: ComposeDesktopHostAdapter? = null,
    themeMode: SecureLanThemeMode,
    settingsController: DesktopAppSettingsController,
    onThemeToggle: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        if (hostAdapter != null) {
            LiveComposeShellContent(hostAdapter, themeMode, settingsController, onThemeToggle)
        } else {
            PreviewComposeShellContent(themeMode, onThemeToggle)
        }
    }
}
