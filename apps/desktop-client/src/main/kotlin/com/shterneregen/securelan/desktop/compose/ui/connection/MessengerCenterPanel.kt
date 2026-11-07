package com.shterneregen.securelan.desktop.compose.ui.connection

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.settings.DesktopConnectionMode
import com.shterneregen.securelan.desktop.compose.settings.DesktopRecentRoom
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceState
import com.shterneregen.securelan.desktop.compose.ui.components.CalmFocusButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.ui.shell.CallBanner
import com.shterneregen.securelan.desktop.compose.ui.shell.RoomHeaderAction

@Composable
internal fun MessengerCenterPanel(
    hostAdapter: ComposeDesktopHostAdapter?,
    workspaceState: ComposeWorkspaceState,
    selectedJoinTarget: ComposeConnectionJoinTarget?,
    hubTooltip: String? = null,
    recentRooms: List<DesktopRecentRoom> = emptyList(),
    headerActions: @Composable () -> Unit = {},
    chatSurface: @Composable () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val connected = hostAdapter?.statusState?.let { it.localServerRunning || it.clientConnected } ?: false
    var detailsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        BoxWithConstraints {
            val wideTitle = maxWidth >= 700.dp
            val title: @Composable () -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    if (!connected) Icon(SecureLanIcons.Wake, null, Modifier.size(32.dp), tint = tokens.colors.textSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                    Text(if (connected) "Shared room" else "Get connected", style = if (connected) MaterialTheme.typography.h6 else if (wideTitle) MaterialTheme.typography.h4 else MaterialTheme.typography.h5)
                    Text(if (connected) "Messages are shared with everyone in this room." else "Start a secure room or join someone nearby.",
                        style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                    }
                }
            }
            val actions: @Composable () -> Unit = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    headerActions()
                    if (connected) RoomHeaderAction(
                        label = if (detailsOpen) "Hide room details" else "Room details",
                        icon = SecureLanIcons.Settings,
                        onClick = { detailsOpen = !detailsOpen },
                        expanded = detailsOpen,
                    )
                }
            }
            if (maxWidth >= 800.dp || (!connected && maxWidth >= 500.dp)) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Box(Modifier.weight(1f)) { title() }; actions()
            } else Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) { title(); actions() }
        }
        Divider(color = tokens.colors.borderSubtle)
        if (!connected && hostAdapter != null) {
            Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                RoomEntry(hostAdapter, selectedJoinTarget, recentRooms)
            }
        } else if (connected) {
            if (detailsOpen) Box(Modifier.testTag("room-details")) {
                ActiveRoomDetails(hostAdapter)
            }
            if (hubTooltip != null) Text(hubTooltip, style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
            if (workspaceState.callBannerVisible) CallBanner(workspaceState)
            if (!detailsOpen && hostAdapter.statusState.localServerRunning && hostAdapter.visiblePeerItems.none { it.online() }) {
                EntryCard("Room is ready", "Use Invite to share a link or QR code. Both include the room password.") {}
            }
            Box(Modifier.weight(1f)) { chatSurface() }
        } else {
            Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) { ConnectionHubPreview(com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE) }
        }
    }
}

@Composable
private fun RoomEntry(adapter: ComposeDesktopHostAdapter, selectedTarget: ComposeConnectionJoinTarget?, recentRooms: List<DesktopRecentRoom>) {
    val tokens = LocalSecureLanDesignTokens.current
    var target by remember { mutableStateOf(selectedTarget) }
    var formVisible by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(if (adapter.preferredConnectionMode == DesktopConnectionMode.JOIN) ComposeConnectionHubMode.JOIN else ComposeConnectionHubMode.HOST) }
    fun chooseMode(value: ComposeConnectionHubMode) {
        mode = value
        formVisible = true
        adapter.updateLastConnectionMode(if (value == ComposeConnectionHubMode.HOST) DesktopConnectionMode.HOST else DesktopConnectionMode.JOIN)
    }
    LaunchedEffect(selectedTarget) {
        if (selectedTarget != null) { target = selectedTarget; chooseMode(ComposeConnectionHubMode.JOIN) }
    }
    // ConnectionHub owns its draft continuously, including while the entry cards are shown.
    ConnectionHub(
        hostAdapter = adapter, initialMode = mode, selectedJoinTarget = target, wrapInCard = false,
        formVisible = formVisible,
        onImportedTarget = { target = it; chooseMode(ComposeConnectionHubMode.JOIN) },
        landingContent = { paste ->
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                if (formVisible) {
                    CompactButton(onClick = { formVisible = false }, enabled = !adapter.statusState.operationInProgress, tone = CompactButtonTone.TERTIARY) { Text("Back to connection options") }
                    ConnectionModeSelector(mode, "Host", "Join", ::chooseMode, modifier = Modifier.fillMaxWidth(),
                        hostEnabled = !adapter.statusState.operationInProgress, joinEnabled = !adapter.statusState.operationInProgress, prominent = true)
                } else {
                    Text("Connect. Chat. Share.", style = MaterialTheme.typography.h5)
                    Text("Choose how to start. Quick Share is also available without a room.", style = MaterialTheme.typography.body2)
                    BoxWithConstraints {
                        val host: @Composable (Modifier) -> Unit = { modifier ->
                            EntryCard("Host a secure room", "Create a password-protected room on this device.", modifier) {
                                CalmFocusButton(onClick = { chooseMode(ComposeConnectionHubMode.HOST) }) { Text("Set up room") }
                            }
                        }
                        val join: @Composable (Modifier) -> Unit = { modifier ->
                            EntryCard("Join a room", "Find a nearby room or connect by address.", modifier) {
                                CompactButton(onClick = { chooseMode(ComposeConnectionHubMode.JOIN) }, tone = CompactButtonTone.SECONDARY) { Text("Join a room") }
                            }
                        }
                        if (maxWidth >= 600.dp) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                            host(Modifier.weight(1f)); join(Modifier.weight(1f))
                        } else Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                            host(Modifier.fillMaxWidth()); join(Modifier.fillMaxWidth())
                        }
                    }
                    EntryActionStrip("Have an invitation?", "Paste a room link to review it before joining.") {
                        CompactButton(onClick = paste, tone = CompactButtonTone.SECONDARY) { Text("Paste room link") }
                    }
                    EntryCard("Nearby rooms", "Rooms advertised on your local network appear automatically.") {
                        adapter.roomDiscoverySnapshot.providers.forEach { provider ->
                            Text("${provider.provider}: ${provider.availability}" + if (provider.limited) " (limited results)" else "", style = MaterialTheme.typography.caption)
                        }
                        if (adapter.nearbyRoomTargets.isEmpty()) {
                            Text("No rooms discovered. Use an invitation or connect by address.", style = MaterialTheme.typography.body2)
                        }
                        adapter.nearbyRoomTargets.forEach { room ->
                            CenterPanelRoomRow(room, target?.rowId == room.rowId) { target = room; chooseMode(ComposeConnectionHubMode.JOIN) }
                        }
                    }
                    recentRooms.firstOrNull()?.let { room ->
                        EntryActionStrip("Recent room", "${room.host}:${room.chatPort} · files ${room.filePort}") {
                            CompactButton(onClick = {
                                target = ComposeConnectionJoinTarget(room.host, room.host, room.chatPort.toString(), room.filePort.toString())
                                chooseMode(ComposeConnectionHubMode.JOIN)
                            }, tone = CompactButtonTone.SECONDARY) { Text("Review connection") }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ActiveRoomDetails(adapter: ComposeDesktopHostAdapter) {
    val state = adapter.statusState
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("room-details-content"),
        shape = RoundedCornerShape(tokens.radius.medium),
        color = tokens.colors.surfaceLevel2,
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
    ) {
        Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            // Keep leaving the room outside the scrollable information region.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                    Text(
                        if (state.localServerRunning) "Hosting as ${state.nickname}" else "Joined as ${state.nickname}",
                        style = MaterialTheme.typography.subtitle2, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(if (state.localServerRunning) state.serverStatus else state.connectionStatus,
                        style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                }
                CompactButton(
                    onClick = { if (state.localServerRunning) adapter.stopHosting() else adapter.disconnect() },
                    enabled = !state.operationInProgress,
                    tone = CompactButtonTone.DESTRUCTIVE,
                ) { Text(if (state.localServerRunning) "Stop hosting" else "Leave room") }
            }
            Divider(color = tokens.colors.borderSubtle)
            Column(Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                if (state.localServerRunning) {
                    Row(
                        Modifier.fillMaxWidth().toggleable(value = state.discoverable, role = Role.Switch, onValueChange = adapter::setDiscoverable),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Discoverable", style = MaterialTheme.typography.body2)
                            Text("Visible to nearby devices. Changes apply immediately.",
                                style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                        }
                        Switch(checked = state.discoverable, onCheckedChange = null)
                    }
                }
                Text(
                    if (state.localServerRunning) "To change the password or ports, stop and recreate the room."
                    else "Leave this room before joining or creating another.",
                    style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
internal fun EntryCard(title: String, description: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radius.medium),
        color = tokens.colors.surfaceLevel2, border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Text(description, style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
            content()
        }
    }
}

@Composable
private fun EntryActionStrip(title: String, description: String, action: @Composable () -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radius.medium), color = tokens.colors.surfaceLevel2,
        border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
        Row(Modifier.padding(tokens.spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                Text(title, style = MaterialTheme.typography.subtitle2)
                Text(description, style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
            }
            action()
        }
    }
}
