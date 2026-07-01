package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyMode
import java.awt.Dimension

@Composable
internal fun LiveSteganographyDialog(
    hostAdapter: ComposeDesktopHostAdapter,
    initialMode: ComposeSteganographyMode,
    recipient: DiscoveredPeer?,
    onClose: () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(640.dp, 740.dp)),
        title = "SecureLanSuite · Steganography",
        resizable = true,
        onPreviewKeyEvent = { event ->
            if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                onClose()
                true
            } else {
                false
            }
        },
    ) {
        LaunchedEffect(window) {
            window.minimumSize = Dimension(560, 620)
        }
        Surface(color = MaterialTheme.colors.background, modifier = Modifier.fillMaxSize()) {
            LiveSteganographyCard(
                hostAdapter = hostAdapter,
                initialMode = initialMode,
                recipient = recipient,
                onClose = onClose,
            )
        }
    }
}
