package com.shterneregen.securelan.desktop.compose.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import com.shterneregen.securelan.desktop.compose.settings.*
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.ui.media.AudioVideoDevicesPreviewCard
import com.shterneregen.securelan.desktop.compose.ui.media.LiveAudioVideoDevicesCard
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.util.openComposeDirectoryChooser
import java.awt.Dimension
import java.nio.file.Path
import kotlin.math.roundToInt

@Composable
internal fun LiveSettingsDialog(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    settingsController: DesktopAppSettingsController,
    onClose: () -> Unit,
) {
    SettingsDialog(onClose = onClose) {
        SettingsPreferencesContent(hostAdapter, settingsController)
        Spacer(Modifier.height(16.dp))
        LiveAudioVideoDevicesCard(hostAdapter = hostAdapter, peerState = peerState)
    }
}

@Composable
internal fun PreviewSettingsDialog(onClose: () -> Unit) {
    SettingsDialog(onClose = onClose) {
        AudioVideoDevicesPreviewCard()
    }
}

@Composable
private fun SettingsDialog(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(980.dp, 780.dp)),
        title = "SecureLanSuite · Settings",
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
            window.minimumSize = Dimension(820, 640)
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            SettingsWindowContent(onClose = onClose, content = content)
        }
    }
}

@Composable
private fun SettingsWindowContent(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(
        modifier = Modifier.fillMaxSize().padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SecureLanIcons.Settings,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colors.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings", style = MaterialTheme.typography.h5)
                Text(
                    "Configure SecureLanSuite for calls and everyday use.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }
            CompactButton(onClick = onClose, tone = CompactButtonTone.TERTIARY) {
                Icon(
                    imageVector = SecureLanIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Close")
            }
        }
        Divider(color = tokens.colors.borderSubtle)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            SettingsNavigation(modifier = Modifier.width(190.dp).fillMaxHeight())
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(end = tokens.spacing.xs, bottom = tokens.spacing.md),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsPreferencesContent(
    hostAdapter: ComposeDesktopHostAdapter,
    controller: DesktopAppSettingsController,
) {
    val settings = controller.settings
    var displayName by remember(settings.displayName) { mutableStateOf(settings.displayName.orEmpty()) }
    var volume by remember(settings.media.volumePercent) { mutableFloatStateOf(settings.media.volumePercent.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsSection("Profile & appearance", "Identity and visual behavior restored at startup.") {
            CompactTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    hostAdapter.updatePreferredNickname(it)
                },
                label = "Display name",
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Theme", style = MaterialTheme.typography.subtitle2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecureLanThemeMode.entries.forEach { mode ->
                    CompactButton(
                        onClick = { controller.update { it.copy(themeMode = mode) } },
                        tone = if (settings.themeMode == mode) CompactButtonTone.SECONDARY else CompactButtonTone.TERTIARY,
                    ) { Text(mode.displayName.replaceFirstChar(Char::uppercase)) }
                }
            }
            PreferenceSwitch(
                title = "Reduce motion",
                description = "Disable non-essential UI animation.",
                checked = settings.reducedMotion,
                onCheckedChange = { enabled -> controller.update { it.copy(reducedMotion = enabled) } },
            )
        }

        SettingsSection("Sound & notifications", "Control application feedback and transfer notices.") {
            Text("Application volume: ${volume.roundToInt()}%", style = MaterialTheme.typography.subtitle2)
            Slider(
                value = volume,
                onValueChange = { volume = it },
                onValueChangeFinished = {
                    controller.update { it.copy(media = it.media.copy(volumePercent = volume.roundToInt())) }
                },
                valueRange = 0f..100f,
            )
            PreferenceSwitch(
                "Desktop notifications",
                "Allow application notifications.",
                settings.notifications.enabled,
            ) { enabled -> controller.update { it.copy(notifications = it.notifications.copy(enabled = enabled)) } }
            PreferenceSwitch(
                "Notification sounds",
                "Play a sound for enabled notifications.",
                settings.notifications.soundsEnabled,
                enabled = settings.notifications.enabled,
            ) { enabled -> controller.update { it.copy(notifications = it.notifications.copy(soundsEnabled = enabled)) } }
            PreferenceSwitch(
                "Transfer notifications",
                "Show incoming and completed transfer notifications.",
                settings.notifications.transferNotificationsEnabled,
                enabled = settings.notifications.enabled,
            ) { enabled ->
                controller.update { it.copy(notifications = it.notifications.copy(transferNotificationsEnabled = enabled)) }
            }
        }

        SettingsSection("Files & confirmations", "Choose where received files go and how trusted transfers are handled.") {
            Text("Downloads directory", style = MaterialTheme.typography.subtitle2)
            Text(
                settings.downloadsDirectory,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
            CompactButton(
                onClick = {
                    openComposeDirectoryChooser("Choose downloads directory", Path.of(settings.downloadsDirectory))
                        ?.let(hostAdapter::updateDownloadsDirectory)
                },
            ) { Text("Choose directory") }
            PreferenceSwitch(
                "Automatically accept files",
                "Only files from known online peers are accepted automatically.",
                hostAdapter.autoAcceptIncomingFiles,
            ) { hostAdapter.updateAutoAcceptIncomingFiles(it) }
            PreferenceSwitch(
                "Notify when a transfer completes",
                "Keep completion feedback enabled for file transfers.",
                settings.transfers.notifyOnCompletion,
            ) { enabled ->
                controller.update { it.copy(transfers = it.transfers.copy(notifyOnCompletion = enabled)) }
            }
        }

        SettingsSection("Network defaults", "Defaults used the next time a room is hosted or joined.") {
            PreferenceSwitch(
                "Discoverable room",
                "Advertise hosted rooms to peers on the LAN.",
                settings.network.discoverable,
            ) { enabled ->
                if (hostAdapter.statusState.localServerRunning) {
                    hostAdapter.setDiscoverable(enabled)
                } else {
                    hostAdapter.updateNetworkDefaults(settings.network.copy(discoverable = enabled))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PortPreference("Host chat port", settings.network.serverChatPort, Modifier.weight(1f)) { port ->
                    hostAdapter.updateNetworkDefaults(controller.settings.network.copy(serverChatPort = port))
                }
                PortPreference("Host file port", settings.network.serverFilePort, Modifier.weight(1f)) { port ->
                    hostAdapter.updateNetworkDefaults(controller.settings.network.copy(serverFilePort = port))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PortPreference("Join chat port", settings.network.clientChatPort, Modifier.weight(1f)) { port ->
                    hostAdapter.updateNetworkDefaults(controller.settings.network.copy(clientChatPort = port))
                }
                PortPreference("Local file port", settings.network.clientFilePort, Modifier.weight(1f)) { port ->
                    hostAdapter.updateNetworkDefaults(controller.settings.network.copy(clientFilePort = port))
                }
            }
            Text(
                "Last mode: ${settings.network.lastConnectionMode.name.lowercase().replaceFirstChar(Char::uppercase)}",
                style = MaterialTheme.typography.body2,
            )
            if (settings.network.recentRooms.isNotEmpty()) {
                Text("Recent rooms", style = MaterialTheme.typography.subtitle2)
                settings.network.recentRooms.forEach { room ->
                    Text(
                        "${room.host}:${room.chatPort} · files ${room.filePort}",
                        style = MaterialTheme.typography.caption,
                    )
                }
                CompactButton(
                    onClick = {
                        hostAdapter.updateNetworkDefaults(controller.settings.network.copy(recentRooms = emptyList()))
                    },
                    tone = CompactButtonTone.TERTIARY,
                ) { Text("Clear recent rooms") }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(modifier = Modifier.fillMaxWidth(), elevation = 0.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.h6)
            Text(
                description,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
            content()
        }
    }
}

@Composable
private fun PreferenceSwitch(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Text(
                description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = if (enabled) 0.64f else 0.38f),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun PortPreference(
    label: String,
    port: Int,
    modifier: Modifier = Modifier,
    onValidPortChange: (Int) -> Unit,
) {
    var text by remember(port) { mutableStateOf(port.toString()) }
    CompactTextField(
        value = text,
        onValueChange = { value ->
            text = value.filter(Char::isDigit).take(5)
            text.toIntOrNull()?.takeIf { it in 1..65_535 }?.let(onValidPortChange)
        },
        label = label,
        modifier = modifier,
    )
}

@Composable
private fun SettingsNavigation(modifier: Modifier = Modifier) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radius.large),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        color = tokens.colors.surfaceLevel1,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(tokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Text(
                "SETTINGS",
                modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
                style = MaterialTheme.typography.overline,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(tokens.radius.medium),
                color = MaterialTheme.colors.primary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.32f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = SecureLanIcons.Devices,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colors.primary,
                    )
                    Text(
                        "All settings",
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.primary,
                    )
                }
            }
        }
    }
}
