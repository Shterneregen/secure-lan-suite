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

    LaunchedEffect(settings.themeMode) {
        DesktopLookAndFeel.update(settings.themeMode)
    }

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
                val incoming = hostAdapter?.mediaVoiceState?.currentSession?.takeIf {
                    it.state == com.shterneregen.securelan.common.model.rtc.RtcSessionState.INCOMING
                }
                if (incoming != null) {
                    androidx.compose.material.AlertDialog(
                        onDismissRequest = hostAdapter::closeRealtimeSession,
                        title = { androidx.compose.material.Text("Incoming call from ${incoming.remotePeer}") },
                        text = {
                            androidx.compose.material.Text(
                                when (incoming.mode) {
                                    com.shterneregen.securelan.common.model.rtc.RtcSessionMode.AUDIO_VIDEO -> "Accept to share your camera and microphone."
                                    com.shterneregen.securelan.common.model.rtc.RtcSessionMode.VIDEO -> "Accept to share your camera."
                                    com.shterneregen.securelan.common.model.rtc.RtcSessionMode.AUDIO -> "Accept to share your microphone."
                                    else -> "Accept to open a direct data connection."
                                },
                            )
                        },
                        confirmButton = {
                            com.shterneregen.securelan.desktop.compose.ui.components.CompactButton(
                                onClick = { hostAdapter.acceptIncomingRealtimeSession(incoming.sessionId.orEmpty()) },
                            ) { androidx.compose.material.Text("Accept") }
                        },
                        dismissButton = {
                            com.shterneregen.securelan.desktop.compose.ui.components.CompactButton(
                                onClick = hostAdapter::closeRealtimeSession,
                                tone = com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone.DESTRUCTIVE,
                            ) { androidx.compose.material.Text("Decline") }
                        },
                    )
                }
            }
        }
    }
}
