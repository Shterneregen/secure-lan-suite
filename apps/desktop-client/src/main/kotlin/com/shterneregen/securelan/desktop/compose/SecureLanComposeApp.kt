package com.shterneregen.securelan.desktop.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.settings.DesktopAppSettingsController
import com.shterneregen.securelan.desktop.compose.ui.shell.ComposeShellContent

@Composable
fun SecureLanComposeApp(
    hostAdapter: ComposeDesktopHostAdapter? = null,
    settingsController: DesktopAppSettingsController? = null,
) {
    val localSettingsController = remember { DesktopAppSettingsController() }
    val controller = settingsController ?: localSettingsController
    val settings = controller.settings

    CompositionLocalProvider(LocalReducedMotion provides settings.reducedMotion) {
        SecureLanTheme(themeMode = settings.themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background,
            ) {
                ComposeShellContent(
                    hostAdapter = hostAdapter,
                    themeMode = settings.themeMode,
                    settingsController = controller,
                    onThemeToggle = {
                        controller.update { it.copy(themeMode = it.themeMode.next()) }
                    },
                )
            }
        }
    }
}
