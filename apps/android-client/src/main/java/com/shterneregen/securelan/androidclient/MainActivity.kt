package com.shterneregen.securelan.androidclient

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Settings
import com.shterneregen.securelan.androidclient.model.AppLogEntry
import com.shterneregen.securelan.androidclient.model.ChatLine
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.MainUiState
import com.shterneregen.securelan.androidclient.model.PeerRole
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.ui.AndroidClipboard
import com.shterneregen.securelan.androidclient.ui.AndroidUiFormatters

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
                viewModel.startDiscovery()
            }
            SecureLanAndroidApp(viewModel)
        }
    }
}

@Composable
private fun SecureLanAndroidApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val colorScheme = if (state.darkThemeEnabled) SecureLanDarkColors else SecureLanLightColors
    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = state,
                onNicknameChange = viewModel::updateNickname,
                onPasswordChange = viewModel::updateSessionPassword,
                onDarkThemeChange = viewModel::updateDarkThemeEnabled,
                onPeerSelected = viewModel::selectPeer,
                onConnect = viewModel::connectSelectedPeer,
                onDisconnect = viewModel::disconnect,
                onInputChange = viewModel::updateInputMessage,
                onSendMessage = viewModel::sendTextMessage,
                onFileSelected = viewModel::selectFile,
                onSendFile = viewModel::sendSelectedFile,
                onStartFileReceiver = viewModel::startFileReceiver,
                onStopFileReceiver = viewModel::stopFileReceiver,
            )
        }
    }
}

private enum class AppDestination(val label: String) {
    CONNECTION("Connection"),
    CHAT("Chat"),
    FILES("Files"),
    SETTINGS("Settings"),
}

@Composable
private fun MainScreen(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onFileSelected: (android.net.Uri) -> Unit,
    onSendFile: () -> Unit,
    onStartFileReceiver: () -> Unit,
    onStopFileReceiver: () -> Unit,
) {
    var selectedDestination by remember { mutableStateOf(AppDestination.CONNECTION) }
    var logsVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFileSelected(uri)
    }
    val friendlyError = state.error?.toFriendlyError()

    if (logsVisible) {
        LogsDialog(logs = state.logs, onDismiss = { logsVisible = false })
    }

    LaunchedEffect(friendlyError) {
        if (friendlyError != null) {
            val result = snackbarHostState.showSnackbar(
                message = friendlyError.title,
                actionLabel = "Logs",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                logsVisible = true
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                state = state,
                friendlyError = friendlyError,
                onOpenLogs = { logsVisible = true },
            )
        },
        bottomBar = {
            AppNavigationBar(
                selectedDestination = selectedDestination,
                onDestinationSelected = { selectedDestination = it },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars.exclude(WindowInsets.ime),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedDestination) {
                AppDestination.CONNECTION -> ConnectionScreen(
                    state = state,
                    onNicknameChange = onNicknameChange,
                    onPasswordChange = onPasswordChange,
                    onPeerSelected = onPeerSelected,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                )
                AppDestination.CHAT -> ChatScreen(
                    state = state,
                    onInputChange = onInputChange,
                    onSendMessage = onSendMessage,
                    onGoToConnection = { selectedDestination = AppDestination.CONNECTION },
                )
                AppDestination.FILES -> FilesScreen(
                    state = state,
                    onPeerSelected = onPeerSelected,
                    onPickFile = { filePicker.launch(arrayOf("*/*")) },
                    onSendFile = onSendFile,
                    onStartFileReceiver = onStartFileReceiver,
                    onStopFileReceiver = onStopFileReceiver,
                )
                AppDestination.SETTINGS -> SettingsScreen(
                    state = state,
                    onDarkThemeChange = onDarkThemeChange,
                    onOpenLogs = { logsVisible = true },
                )
            }
        }
    }
}

@Composable
private fun AppHeader(state: MainUiState, friendlyError: FriendlyError?, onOpenLogs: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "SecureLan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.connectionPeer?.let { "Target: ${it.nickname} · ${it.host}:${it.chatPort}" }
                            ?: "Encrypted LAN chat and file transfer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusText(text = if (state.connected) "Connected" else "Offline", active = state.connected)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip(
                    text = if (state.discoveryRunning) "Discovery on" else "Discovery off",
                    active = state.discoveryRunning,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    text = state.status.toFriendlyStatus(),
                    active = state.connected || state.discoveryRunning,
                    modifier = Modifier.weight(1.35f),
                )
            }
            if (friendlyError != null) {
                FriendlyErrorBanner(error = friendlyError, onOpenLogs = onOpenLogs)
            }
        }
    }
}

@Composable
private fun FriendlyErrorBanner(error: FriendlyError, onOpenLogs: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(error.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(error.message, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onOpenLogs) { Text("Logs") }
        }
    }
}

@Composable
private fun StatusChip(text: String, active: Boolean, modifier: Modifier = Modifier) {
    val background = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.semantics {
            contentDescription = text
            stateDescription = if (active) "active" else "inactive"
        },
        color = background.copy(alpha = 0.9f),
        contentColor = foreground,
        shape = RoundedCornerShape(100.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = active)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusText(text: String, active: Boolean) {
    Row(
        modifier = Modifier.semantics {
            contentDescription = "Connection status: $text"
            stateDescription = if (active) "connected" else "offline"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(active = active)
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppNavigationBar(selectedDestination: AppDestination, onDestinationSelected: (AppDestination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.navIcon(), contentDescription = null) },
                label = null,
                modifier = Modifier.semantics {
                    contentDescription = "Open ${destination.label}"
                    stateDescription = if (selectedDestination == destination) "selected" else "not selected"
                },
            )
        }
    }
}

private fun AppDestination.navIcon(): ImageVector = when (this) {
    AppDestination.CONNECTION -> Icons.Outlined.Devices
    AppDestination.CHAT -> Icons.Outlined.Chat
    AppDestination.FILES -> Icons.Outlined.Description
    AppDestination.SETTINGS -> Icons.Outlined.Settings
}

@Composable
private fun ConnectionScreen(
    state: MainUiState,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    var helpVisible by remember { mutableStateOf(false) }
    if (helpVisible) {
        AlertDialog(
            onDismissRequest = { helpVisible = false },
            title = { Text("Connection help") },
            text = {
                Text(
                    "Choose a discovered desktop server, enter the nickname and the same session password as on desktop, then connect. If connection fails, check firewall, VPN, guest Wi‑Fi, and that the desktop room is still open.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { Button(onClick = { helpVisible = false }) { Text("Got it") } },
        )
    }
    ScreenLazyColumn {
        item {
            SectionCard(
                title = "Connection",
                subtitle = state.connectionPeer?.let { "Target: ${it.nickname} · ${it.host}:${it.chatPort}" }
                    ?: "Choose a server peer and connect.",
                trailing = { OutlinedButton(onClick = { helpVisible = true }) { Text("?") } },
            ) {
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = onNicknameChange,
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.connected && !state.connecting,
                )
                OutlinedTextField(
                    value = state.sessionPassword,
                    onValueChange = onPasswordChange,
                    label = { Text("Session password") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Session password field" },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.connected && !state.connecting,
                )
                CompactSelectedPeerSummary(state.connectionPeer)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onConnect,
                        enabled = !state.connected && !state.connecting && state.connectionPeer != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.connecting) "Connecting…" else "Connect")
                    }
                    OutlinedButton(onClick = onDisconnect, enabled = state.connected, modifier = Modifier.weight(1f)) {
                        Text("Disconnect")
                    }
                }
                if (!state.connected && state.connectionPeer == null) {
                    DisabledReason("Connect becomes available after a server peer is discovered and selected.")
                }
                if (state.connected) {
                    DisabledReason("Identity fields are locked while connected.")
                }
            }
        }
        item {
            SectionCard(
                title = "Peers",
                subtitle = if (state.peers.isEmpty()) "No discovered peers yet." else "${state.peers.size} peer(s) available.",
            ) {
                if (state.peers.isEmpty()) {
                    EmptyState(
                        title = "No peers available",
                        message = "Keep discovery on and check that desktop SecureLan is open on the same Wi‑Fi.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.peers.forEach { peer ->
                            CompactPeerChoice(
                                peer = peer,
                                selected = state.selectedPeer?.peerId == peer.peerId,
                                onClick = { onPeerSelected(peer) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionGuidance(state: MainUiState) {
    when {
        state.connected -> InfoBox(
            title = "Session active",
            message = "Messages are encrypted end-to-end for the current desktop connection.",
            positive = true,
        )
        state.connecting -> InfoBox(
            title = "Connecting",
            message = "Keep both devices on the same network while SecureLan completes the handshake.",
            positive = true,
        )
        state.peers.isEmpty() -> TroubleshootingBox()
        state.connectionPeer != null -> InfoBox(
            title = "Ready to connect",
            message = "If connection fails, check that the desktop room is still open and that the password matches.",
            positive = true,
        )
    }
}

@Composable
private fun PeersScreen(state: MainUiState, onPeerSelected: (DiscoveredPeer) -> Unit) {
    ScreenLazyColumn {
        item {
            ScreenIntroCard(
                title = "Peers",
                message = "SecureLan listens for desktop peers on your local network and lets you choose a server target.",
            )
        }
        if (state.peers.isEmpty()) {
            item { TroubleshootingBox() }
        } else {
            items(state.peers, key = { it.peerId }) { peer ->
                PeerRow(
                    peer = peer,
                    selected = state.selectedPeer?.peerId == peer.peerId,
                    onClick = { onPeerSelected(peer) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatScreen(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onGoToConnection: () -> Unit,
) {
    val chatListState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            chatListState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .imeNestedScroll(),
            state = chatListState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.connected) "No messages yet" else "Connect first",
                        message = if (state.connected) {
                            "Your secure session is ready. Send the first message to start the conversation."
                        } else {
                            "Open Connection, choose a desktop server, and connect before sending messages."
                        },
                    )
                }
            } else {
                items(state.messages) { line -> ChatBubble(line) }
            }
        }
        ChatInputBar(
            state = state,
            onInputChange = onInputChange,
            onSendMessage = onSendMessage,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .imePadding(),
        )
    }
}

@Composable
private fun ChatStateHeader(state: MainUiState, onGoToConnection: () -> Unit) {
    SectionCard(
        title = "Chat",
        subtitle = if (state.connected) "End-to-end encrypted session is active." else "Messaging is available after connection.",
        trailing = {
            if (!state.connected) {
                OutlinedButton(onClick = onGoToConnection) { Text("Connect") }
            }
        },
    ) {
        StatusChip(
            text = if (state.connected) "Ready to send secure messages" else "Offline — connect to start messaging",
            active = state.connected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChatInputBar(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = state.inputMessage,
                    onValueChange = onInputChange,
                    placeholder = { Text(if (state.connected) "Type a secure message" else "Connect first") },
                    modifier = Modifier.weight(1f),
                    enabled = state.connected,
                    minLines = 1,
                    maxLines = 4,
                )
                Button(
                    onClick = onSendMessage,
                    enabled = state.connected && state.inputMessage.isNotBlank(),
                    modifier = Modifier.height(56.dp),
                ) {
                    Text("Send")
                }
            }
            if (!state.connected) {
                DisabledReason("Connect to a desktop peer before sending messages.")
            }
        }
    }
}

@Composable
private fun FilesScreen(
    state: MainUiState,
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onPickFile: () -> Unit,
    onSendFile: () -> Unit,
    onStartFileReceiver: () -> Unit,
    onStopFileReceiver: () -> Unit,
) {
    ScreenLazyColumn {
        item {
            ScreenIntroCard(
                title = "Files",
                message = "Send one selected document to a peer or listen for an encrypted desktop-to-Android transfer.",
            )
        }
        item {
            SectionCard(
                title = "Send target",
                subtitle = state.selectedPeer?.let { "${it.nickname} · ${it.host}:${it.filePort}" } ?: "Select a peer before sending files.",
            ) {
                if (state.peers.isEmpty()) {
                    EmptyState(
                        title = "No peers available",
                        message = "Keep discovery on and check that desktop SecureLan is open on the same Wi‑Fi.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.peers.forEach { peer ->
                            CompactPeerChoice(
                                peer = peer,
                                selected = state.selectedPeer?.peerId == peer.peerId,
                                onClick = { onPeerSelected(peer) },
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Selected file", subtitle = "Choose a local document to send.") {
                SelectedFileCard(state)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onPickFile, modifier = Modifier.weight(1f)) { Text("Pick file") }
                    FilledTonalButton(
                        onClick = onSendFile,
                        enabled = state.selectedPeer != null && state.selectedFile != null && !state.fileProgress.active,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Send file")
                    }
                }
                FileSendHint(state)
            }
        }
        item {
            SectionCard(title = "Receive from desktop", subtitle = "Start a listener when the desktop sends a file to this phone.") {
                ReceiveListenerStatus(state)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onStartFileReceiver,
                        enabled = !state.fileReceiverRunning && state.sessionPassword.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Receive")
                    }
                    OutlinedButton(onClick = onStopFileReceiver, enabled = state.fileReceiverRunning, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                }
                if (state.sessionPassword.isBlank()) {
                    DisabledReason("Enter the session password on Connection before receiving files.")
                }
            }
        }
        if (state.fileProgress.active || state.fileProgress.bytesSent > 0 || state.fileProgress.error != null) {
            item {
                TransferProgressCard(
                    title = if (state.fileProgress.active) "Sending ${state.fileProgress.fileName.ifBlank { "file" }}" else "Last sent file",
                    progress = state.fileProgress.percent,
                    currentBytes = state.fileProgress.bytesSent,
                    totalBytes = state.fileProgress.totalBytes,
                    error = state.fileProgress.error,
                )
            }
        }
        if (state.incomingFileProgress.active || state.incomingFileProgress.bytesReceived > 0 || state.incomingFileProgress.error != null) {
            item {
                TransferProgressCard(
                    title = if (state.incomingFileProgress.active) {
                        "Receiving ${state.incomingFileProgress.fileName.ifBlank { "file" }}"
                    } else {
                        "Incoming file"
                    },
                    progress = state.incomingFileProgress.percent,
                    currentBytes = state.incomingFileProgress.bytesReceived,
                    totalBytes = state.incomingFileProgress.totalBytes,
                    error = state.incomingFileProgress.error,
                    completedPath = state.incomingFileProgress.completedPath,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: MainUiState, onDarkThemeChange: (Boolean) -> Unit, onOpenLogs: () -> Unit) {
    ScreenLazyColumn {
        item {
            ScreenIntroCard(
                title = "Settings",
                message = "Theme, diagnostics, and quick app state details for troubleshooting LAN sessions.",
            )
        }
        item {
            SectionCard(title = "Appearance", subtitle = "Choose the local app theme.") {
                SettingsRow(
                    title = "Dark theme",
                    subtitle = if (state.darkThemeEnabled) "Enabled" else "Disabled",
                    trailing = { Switch(checked = state.darkThemeEnabled, onCheckedChange = onDarkThemeChange) },
                )
            }
        }
        item {
            SectionCard(title = "Diagnostics", subtitle = "Useful when discovery, connection, or file transfer fails.") {
                SettingsRow(
                    title = "Logs",
                    subtitle = "${state.logs.size} entries available to view or copy.",
                    trailing = { OutlinedButton(onClick = onOpenLogs, enabled = state.logs.isNotEmpty()) { Text("Open") } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                SettingsRow(
                    title = "Network ports",
                    subtitle = "Chat ${SecureLanPorts.DEFAULT_CHAT_PORT} · Files ${SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT} · Discovery ${SecureLanPorts.DEFAULT_DISCOVERY_PORT}",
                )
            }
        }
        item {
            SectionCard(title = "Current session", subtitle = "Read-only summary.") {
                SettingsRow(title = "Discovery", subtitle = if (state.discoveryRunning) "Listening for peers" else "Stopped")
                SettingsRow(title = "Connection", subtitle = if (state.connected) "Connected as ${state.nickname}" else "Offline")
                SettingsRow(title = "Selected peer", subtitle = state.selectedPeer?.let { "${it.nickname} · ${it.host}:${it.chatPort}" } ?: "None")
            }
        }
    }
}

@Composable
private fun LogsDialog(logs: List<AppLogEntry>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val logText = remember(logs) { logs.joinToString(separator = "\n") { AndroidUiFormatters.formatLogEntry(it) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SecureLan logs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Plain-text diagnostic log. Select any part of it, or copy the full log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "No log entries yet.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = logText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { AndroidClipboard.copyLogs(context, logText) }, enabled = logText.isNotBlank()) {
                Text("Copy all")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SelectedPeerSummary(peer: DiscoveredPeer?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = peer != null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer?.nickname ?: "No server peer selected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = peer?.let { "${it.host}:${it.chatPort} · file ${it.filePort}" }
                        ?: "Open Peers after discovery finds your desktop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CompactSelectedPeerSummary(peer: DiscoveredPeer?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = peer != null)
            Text(
                text = peer?.let { "${it.nickname} · ${it.host}:${it.chatPort}" } ?: "No server peer selected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PeerRow(peer: DiscoveredPeer, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Peer ${peer.nickname}, ${peer.role.name.lowercase()}, ${if (selected) "selected" else "not selected"}"
                stateDescription = if (selected) "selected" else "not selected"
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarLetter(peer.nickname)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(peer.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PeerRolePill(peer.role, selected)
                }
                Text(
                    AndroidUiFormatters.peerEndpointSummary(peer),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (selected) "Selected" else "Select",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.86f),
            )
        }
    }
}

@Composable
private fun PeerRolePill(role: PeerRole, selected: Boolean) {
    val label = if (role == PeerRole.SERVER) "Server" else "Client"
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(100.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(line: ChatLine) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val alignment = if (line.outbound) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (line.outbound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (line.outbound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = if (line.outbound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(20.dp))
                .background(bubbleColor)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { AndroidClipboard.copyMessage(context, line.text) },
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (line.outbound) "You" else line.sender,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.76f),
                )
                Text(
                    text = AndroidUiFormatters.formatTimestamp(line.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.68f),
                )
            }
            LinkifiedText(
                text = line.text,
                textColor = textColor,
                linkColor = linkColor,
                style = MaterialTheme.typography.bodyMedium,
                onOpenUri = uriHandler::openUri,
            )
        }
    }
}

@Composable
private fun LinkifiedText(text: String, textColor: Color, linkColor: Color, style: TextStyle, onOpenUri: (String) -> Unit) {
    val annotatedText = remember(text, linkColor) {
        buildAnnotatedString {
            var currentIndex = 0
            UrlRegex.findAll(text).forEach { match ->
                append(text.substring(currentIndex, match.range.first))
                val rawUrl = match.value.trimEnd('.', ',', ';', ':', ')', ']', '}')
                val trailing = match.value.substring(rawUrl.length)
                val url = rawUrl.withUrlScheme()
                pushStringAnnotation(tag = UrlAnnotationTag, annotation = url)
                pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                append(rawUrl)
                pop()
                pop()
                append(trailing)
                currentIndex = match.range.last + 1
            }
            append(text.substring(currentIndex))
        }
    }
    ClickableText(
        text = annotatedText,
        style = style.merge(color = textColor),
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = UrlAnnotationTag, start = offset, end = offset)
                .firstOrNull()
                ?.let { onOpenUri(it.item) }
        },
    )
}

@Composable
private fun SelectedFileCard(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Selected file", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = state.selectedFile?.name ?: "No file selected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.selectedFile?.let { AndroidUiFormatters.formatBytes(it.size) } ?: "Pick a file from Android document picker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileSendHint(state: MainUiState) {
    when {
        state.selectedPeer == null -> DisabledReason("Select a send target above before sending files.")
        state.selectedFile == null -> DisabledReason("Choose a file before sending.")
        state.fileProgress.active -> DisabledReason("Wait until the current transfer finishes.")
        else -> InfoBox("Ready to send", "The selected file will be encrypted and sent to ${state.selectedPeer.nickname}.", positive = true)
    }
}

@Composable
private fun CompactPeerChoice(peer: DiscoveredPeer, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Send target ${peer.nickname}, ${if (selected) "selected" else "not selected"}"
                stateDescription = if (selected) "selected" else "not selected"
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = selected)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(peer.nickname, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${peer.host}:${peer.filePort} · chat ${peer.chatPort}", style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (selected) "Selected" else "Select", style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
private fun ReceiveListenerStatus(state: MainUiState) {
    InfoBox(
        title = if (state.fileReceiverRunning) "Listening for incoming files" else "Receiver stopped",
        message = if (state.fileReceiverRunning) {
            "Desktop can send files to this phone using file port ${SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT + 1}."
        } else {
            "Start receiving before sending a desktop-to-Android file transfer."
        },
        positive = state.fileReceiverRunning,
    )
}

@Composable
private fun TransferProgressCard(
    title: String,
    progress: Float,
    currentBytes: Long,
    totalBytes: Long,
    error: String?,
    completedPath: String? = null,
) {
    SectionCard(title = title, subtitle = error?.toFriendlyError()?.title ?: "Transfer progress") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progress", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = title
                        stateDescription = "${(progress * 100).toInt()} percent"
                    },
            )
            Text(
                text = "${AndroidUiFormatters.formatBytes(currentBytes)} / ${AndroidUiFormatters.formatBytes(totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { InfoBox("Transfer needs attention", it.toFriendlyError().message, positive = false) }
            completedPath?.let {
                Text(
                    text = "Saved to: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
private fun ScreenLazyColumn(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun ScreenIntroCard(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun InfoBox(title: String, message: String, positive: Boolean) {
    val color = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.72f),
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TroubleshootingBox() {
    SectionCard(title = "No peers yet", subtitle = "Discovery can take a few seconds on some networks.") {
        Text(
            text = "Check that the desktop app is open, both devices use the same Wi‑Fi, discovery is enabled, and VPN, firewall, guest Wi‑Fi, or client isolation are not blocking LAN traffic.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DisabledReason(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (active) SuccessGreen else MaterialTheme.colorScheme.outline)
            .semantics {
                contentDescription = if (active) "Active status" else "Inactive status"
                stateDescription = if (active) "active" else "inactive"
            },
    )
}

@Composable
private fun AvatarLetter(name: String) {
    Surface(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?", fontWeight = FontWeight.Bold)
        }
    }
}

private data class FriendlyError(val title: String, val message: String)

private fun String.toFriendlyError(): FriendlyError {
    val lower = lowercase()
    return when {
        "data_too_large" in lower || "too large" in lower -> FriendlyError(
            title = "This transfer cannot be completed",
            message = "The selected payload is too large for the current secure handshake. Try a smaller file or check desktop compatibility.",
        )
        "econnrefused" in lower || "connection refused" in lower || "failed to connect" in lower -> FriendlyError(
            title = "Desktop is not accepting connections",
            message = "Make sure the desktop app is open, the room is running, and firewall or VPN settings are not blocking LAN traffic.",
        )
        "password" in lower || "handshake" in lower || "rsa" in lower || "decrypt" in lower -> FriendlyError(
            title = "Secure handshake failed",
            message = "Check that the session password matches the desktop room and try reconnecting.",
        )
        "network" in lower || "timeout" in lower || "unreachable" in lower -> FriendlyError(
            title = "Network connection problem",
            message = "Keep both devices on the same Wi‑Fi and check VPN, hotspot, guest network, or firewall settings.",
        )
        else -> FriendlyError(
            title = "Action needs attention",
            message = take(140).ifBlank { "Open diagnostics logs for details." },
        )
    }
}

private fun String.toFriendlyStatus(): String = when {
    startsWith("Found ") -> this
    contains("Connected", ignoreCase = true) -> this
    contains("Connecting", ignoreCase = true) -> this
    contains("Receiving", ignoreCase = true) -> this
    contains("File receiver", ignoreCase = true) -> this
    contains("Listening", ignoreCase = true) -> "Listening for peers"
    contains("failed", ignoreCase = true) -> "Needs attention"
    else -> this.ifBlank { "Ready" }
}

private fun String.withUrlScheme(): String = if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
    this
} else {
    "https://$this"
}

private const val UrlAnnotationTag = "URL"

private val UrlRegex = Regex("""(?i)\b((?:https?://|www\.)[^\s<>()]+|[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+[^\s<>()]*)""")

private val SecureLanDarkColors = darkColorScheme(
    primary = Color(0xFF9AD6FF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF0E3952),
    onPrimaryContainer = Color(0xFFD3EDFF),
    secondary = Color(0xFFC8DAE8),
    onSecondary = Color(0xFF22323D),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFDDEAF4),
    background = Color(0xFF0E1419),
    onBackground = Color(0xFFE3E8ED),
    surface = Color(0xFF141B21),
    onSurface = Color(0xFFE3E8ED),
    surfaceVariant = Color(0xFF3F4850),
    onSurfaceVariant = Color(0xFFC0C8D0),
    outline = Color(0xFF89939C),
    outlineVariant = Color(0xFF3F4850),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val SecureLanLightColors = lightColorScheme(
    primary = Color(0xFF00658F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF50606B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E4F1),
    onSecondaryContainer = Color(0xFF0C1D27),
    background = Color(0xFFF7FAFD),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val SuccessGreen = Color(0xFF35D07F)
