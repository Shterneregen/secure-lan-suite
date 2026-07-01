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
    var darkTheme by remember { mutableStateOf(true) }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        SecureLanTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background,
            ) {
                ComposeShellContent(
                    hostAdapter = hostAdapter,
                    darkTheme = darkTheme,
                    onThemeToggle = { darkTheme = !darkTheme },
                )
            }
        }
    }
}
