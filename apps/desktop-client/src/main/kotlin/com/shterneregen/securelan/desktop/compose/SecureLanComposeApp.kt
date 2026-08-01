package com.shterneregen.securelan.desktop.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.ui.shell.ComposeShellContent

@Composable
fun SecureLanComposeApp(
    hostAdapter: ComposeDesktopHostAdapter? = null,
    reducedMotion: Boolean = false,
) {
    var themeMode by remember { mutableStateOf(SecureLanThemeMode.DARK) }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        SecureLanTheme(themeMode = themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background,
            ) {
                ComposeShellContent(
                    hostAdapter = hostAdapter,
                    themeMode = themeMode,
                    onThemeToggle = { themeMode = themeMode.next() },
                )
            }
        }
    }
}
