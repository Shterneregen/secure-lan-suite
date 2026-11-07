package com.shterneregen.securelan.desktop.compose.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.ui.media.AudioVideoDevicesPreviewCard
import com.shterneregen.securelan.desktop.compose.ui.media.LiveAudioVideoDevicesCard
import com.shterneregen.securelan.desktop.compose.util.openComposeDirectoryChooser
import java.awt.Dimension
import java.nio.file.Path
import kotlin.math.roundToInt

internal enum class SettingsPage(
    val title: String,
    val description: String,
) {
    GENERAL("General", "Profile, appearance, and accessibility"),
    AUDIO_VIDEO("Audio & video", "Call devices and testing"),
    NOTIFICATIONS("Notifications", "Sound and desktop alerts"),
    FILES("Files & transfers", "Downloads and confirmations"),
    NETWORK("Network", "Hosting and connection defaults"),
}

@Composable
internal fun LiveSettingsDialog(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    settingsController: DesktopAppSettingsController,
    onClose: () -> Unit,
) {
    SettingsDialog(
        pages = SettingsPage.entries,
        persistenceStatus = when {
            settingsController.persistenceError != null -> "Changes could not be saved"
            settingsController.saving -> "Saving changes..."
            else -> "Changes saved automatically"
        },
        onClose = onClose,
    ) { page ->
        LiveSettingsPageContent(hostAdapter, peerState, settingsController, page)
    }
}

@Composable
internal fun LiveSettingsPageContent(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    settingsController: DesktopAppSettingsController,
    page: SettingsPage,
) {
    settingsController.persistenceError?.let { message ->
        Text(message, color = MaterialTheme.colors.error)
        CompactButton(onClick = settingsController::retryPersistence, enabled = !settingsController.saving) { Text("Retry saving settings") }
    }
    when (page) {
        SettingsPage.GENERAL -> GeneralSettingsContent(hostAdapter, settingsController)
        SettingsPage.AUDIO_VIDEO -> LiveAudioVideoDevicesCard(hostAdapter = hostAdapter, peerState = peerState)
        SettingsPage.NOTIFICATIONS -> NotificationSettingsContent(settingsController)
        SettingsPage.FILES -> FileSettingsContent(hostAdapter, settingsController)
        SettingsPage.NETWORK -> NetworkSettingsContent(hostAdapter, settingsController)
    }
}

@Composable
internal fun PreviewSettingsDialog(onClose: () -> Unit) {
    SettingsDialog(
        pages = listOf(SettingsPage.AUDIO_VIDEO),
        initialPage = SettingsPage.AUDIO_VIDEO,
        onClose = onClose,
    ) {
        AudioVideoDevicesPreviewCard()
    }
}

@Composable
private fun SettingsDialog(
    pages: List<SettingsPage>,
    initialPage: SettingsPage = pages.first(),
    onClose: () -> Unit,
    persistenceStatus: String = "Preview",
    content: @Composable (SettingsPage) -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(1200.dp, 800.dp)),
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
            SettingsWindowContent(
                pages = pages,
                initialPage = initialPage,
                persistenceStatus = persistenceStatus,
                content = content,
            )
        }
    }
}

@Composable
internal fun SettingsWindowContent(
    pages: List<SettingsPage>,
    initialPage: SettingsPage = pages.first(),
    persistenceStatus: String = "Changes saved automatically",
    content: @Composable (SettingsPage) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var selectedPage by remember(pages, initialPage) { mutableStateOf(initialPage) }
    Column(Modifier.fillMaxSize().padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
            Column(Modifier.weight(1f)) {
                Text("PREFERENCES", style = MaterialTheme.typography.overline, color = tokens.colors.textSecondary)
                Text("Settings", style = MaterialTheme.typography.h5)
                Text("SecureLanSuite · App preferences", style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
            }
            Text(persistenceStatus, style = MaterialTheme.typography.caption)
        }
        Divider(color = tokens.colors.borderSubtle)
        SettingsNavigation(pages, selectedPage) { selectedPage = it }
        BoxWithConstraints(Modifier.weight(1f)) {
            val showAside = maxWidth >= 1100.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                key(selectedPage) {
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        Text(selectedPage.title, style = MaterialTheme.typography.h6)
                        Text(selectedPage.description, style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                        content(selectedPage)
                    }
                }
                if (showAside) Surface(Modifier.width(260.dp), shape = RoundedCornerShape(tokens.radius.medium), color = tokens.colors.surfaceLevel2,
                    border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
                    Column(Modifier.padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        Text("About these settings", style = MaterialTheme.typography.subtitle2)
                        Text(when (selectedPage) {
                            SettingsPage.GENERAL -> "Your display name identifies you, not the room. Profile changes apply to your next connection."
                            SettingsPage.AUDIO_VIDEO -> "Device selections apply to the next call. Your active call is preserved. Video and camera preview are experimental."
                            SettingsPage.NOTIFICATIONS -> "Disabling notifications leaves chat and file transfers running. Message previews follow your notification preferences."
                            SettingsPage.FILES -> "Automatic acceptance applies only to known online room peers. Quick Share uses separate temporary local HTTP links."
                            SettingsPage.NETWORK -> "Room visibility changes immediately while hosting. Ports are defaults for future connections. Discovery does not authenticate a room."
                        }, style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsContent(
    hostAdapter: ComposeDesktopHostAdapter,
    controller: DesktopAppSettingsController,
) {
    val settings = controller.settings
    val tokens = LocalSecureLanDesignTokens.current
    var displayName by remember(settings.displayName) { mutableStateOf(settings.displayName.orEmpty()) }

    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        BoxWithConstraints {
            val profile: @Composable () -> Unit = {
                SettingsSection("Your profile", "Your participant label; applied to the next connection.") {
                    CompactTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            hostAdapter.updatePreferredNickname(it)
                        },
                        label = "Display name",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            val appearance: @Composable () -> Unit = {
                SettingsSection("Appearance", "Choose the application appearance.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Theme", Modifier.weight(1f), style = MaterialTheme.typography.subtitle2)
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
            }
            if (maxWidth >= 800.dp) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Box(Modifier.weight(1f)) { profile() }
                Box(Modifier.weight(1f)) { appearance() }
            } else Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) { profile(); appearance() }
        }
        SettingsSection("Window behavior", "Choose what the main-window close button does.") {
            PreferenceSwitch(
                title = "Keep running after window closes",
                description = "Hide SecureLanSuite in the system tray instead of exiting.",
                checked = settings.lifecycle.keepRunningAfterWindowClose,
                onCheckedChange = { enabled ->
                    controller.update { current ->
                        current.copy(
                            lifecycle = current.lifecycle.copy(keepRunningAfterWindowClose = enabled),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NotificationSettingsContent(controller: DesktopAppSettingsController) {
    val settings = controller.settings
    var volume by remember(settings.media.volumePercent) { mutableFloatStateOf(settings.media.volumePercent.toFloat()) }

    Column {
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
                "Chat message notifications",
                "Show the sender and a short message preview while the window is hidden or unfocused.",
                settings.notifications.messageNotificationsEnabled,
                enabled = settings.notifications.enabled,
            ) { enabled ->
                controller.update { it.copy(notifications = it.notifications.copy(messageNotificationsEnabled = enabled)) }
            }
            PreferenceSwitch(
                "Transfer notifications",
                "Show incoming and completed transfer notifications.",
                settings.notifications.transferNotificationsEnabled,
                enabled = settings.notifications.enabled,
            ) { enabled ->
                controller.update { it.copy(notifications = it.notifications.copy(transferNotificationsEnabled = enabled)) }
            }
        }
    }
}

@Composable
private fun FileSettingsContent(
    hostAdapter: ComposeDesktopHostAdapter,
    controller: DesktopAppSettingsController,
) {
    val settings = controller.settings

    Column {
        SettingsSection("Download location", "Choose where received files are stored.") {
            Text("Downloads directory", style = MaterialTheme.typography.subtitle2)
            DirectoryPreference(
                path = settings.downloadsDirectory,
                onChoose = {
                    openComposeDirectoryChooser("Choose downloads directory", Path.of(settings.downloadsDirectory))
                        ?.let(hostAdapter::updateDownloadsDirectory)
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        SettingsSection("Transfer confirmations", "Control incoming file decisions and completion feedback.") {
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
    }
}

@Composable
private fun NetworkSettingsContent(
    hostAdapter: ComposeDesktopHostAdapter,
    controller: DesktopAppSettingsController,
) {
    val settings = controller.settings

    Column {
        if (hostAdapter.roomDiscoverySnapshot.providers.isNotEmpty()) SettingsSection("Nearby search", "Discovery hints are unauthenticated. QR/manual Join remains available if search fails.") {
            hostAdapter.roomDiscoverySnapshot.providers.forEach { provider ->
                Text("${provider.provider}: ${provider.availability}" + if (provider.limited) " (limited results)" else "")
            }
            Text("mDNS prefers physical LAN interfaces. Legacy UDP and TCP listeners retain their existing interface scope.", style = MaterialTheme.typography.caption)
        }
        SettingsSection("Room visibility", "Choose whether hosted rooms are advertised on the LAN.") {
            PreferenceSwitch(
                if (hostAdapter.statusState.localServerRunning) "Current room discoverable" else "Advertise future hosted rooms",
                if (hostAdapter.statusState.localServerRunning) "Applies immediately and is saved for future rooms." else "Used when you create a room.",
                settings.network.discoverable,
            ) { enabled ->
                if (hostAdapter.statusState.localServerRunning) {
                    hostAdapter.setDiscoverable(enabled)
                } else {
                    hostAdapter.updateNetworkDefaults(settings.network.copy(discoverable = enabled))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingsSection("Defaults for future connections", "These ports do not change the active room.") {
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
        }
        Spacer(Modifier.height(12.dp))
        SettingsSection("Connection history", "Saved endpoints contain no room passwords.") {
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
private fun DirectoryPreference(
    path: String,
    onChoose: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.small),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.66f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SecureLanIcons.File,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
            )
            Text(
                text = path,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.76f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            CompactButton(onClick = onChoose, tone = CompactButtonTone.TERTIARY) {
                Text("Change")
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
    Card(modifier = Modifier.fillMaxWidth(), elevation = 0.dp, shape = RoundedCornerShape(tokens.radius.medium), border = BorderStroke(1.dp, tokens.colors.borderSubtle), backgroundColor = tokens.colors.surfaceLevel2) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.subtitle2)
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
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 2.dp),
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
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
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
private fun SettingsNavigation(pages: List<SettingsPage>, selectedPage: SettingsPage, onPageSelected: (SettingsPage) -> Unit) {
    val focus = remember(pages) { pages.map { FocusRequester() } }
    ScrollableTabRow(selectedTabIndex = pages.indexOf(selectedPage), edgePadding = 0.dp, backgroundColor = MaterialTheme.colors.background) {
        pages.forEachIndexed { index, page ->
            Tab(selected = selectedPage == page, onClick = { onPageSelected(page) },
                modifier = Modifier.focusRequester(focus[index]).onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) {
                        val next = (index + if (event.key == Key.DirectionRight) 1 else pages.size - 1) % pages.size
                        onPageSelected(pages[next]); focus[next].requestFocus(); true
                    } else false
                },
                text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(page.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(page.title, maxLines = 1)
                } },
            )
        }
    }
}

private val SettingsPage.icon
    get() = when (this) {
        SettingsPage.GENERAL -> SecureLanIcons.Person
        SettingsPage.AUDIO_VIDEO -> SecureLanIcons.Devices
        SettingsPage.NOTIFICATIONS -> SecureLanIcons.Notifications
        SettingsPage.FILES -> SecureLanIcons.File
        SettingsPage.NETWORK -> SecureLanIcons.Network
    }
