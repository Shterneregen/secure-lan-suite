package com.shterneregen.securelan.androidclient

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.model.AppLogEntry
import com.shterneregen.securelan.androidclient.model.ChatLine
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.MainUiState
import com.shterneregen.securelan.androidclient.model.PeerRole
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
    var logsVisible by remember { mutableStateOf(false) }
    var connectionExpanded by remember { mutableStateOf(true) }
    var settingsExpanded by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFileSelected(uri)
    }
    if (logsVisible) {
        LogsDialog(logs = state.logs, onDismiss = { logsVisible = false })
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TopStatusCard(state)
        SetupSection(
            state = state,
            expanded = connectionExpanded,
            onExpandedChange = { connectionExpanded = it },
            onNicknameChange = onNicknameChange,
            onPasswordChange = onPasswordChange,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
        )
        PeerSection(state, onPeerSelected)
        ChatSection(state, onInputChange, onSendMessage)
        FileSection(
            state = state,
            onPickFile = { filePicker.launch(arrayOf("*/*")) },
            onSendFile = onSendFile,
            onStartFileReceiver = onStartFileReceiver,
            onStopFileReceiver = onStopFileReceiver,
        )
        SettingsPanel(
            state = state,
            expanded = settingsExpanded,
            onExpandedChange = { settingsExpanded = it },
            onDarkThemeChange = onDarkThemeChange,
            onOpenLogs = { logsVisible = true },
        )
    }
}

@Composable
private fun TopStatusCard(state: MainUiState) {
    val hasError = state.error != null
    GradientCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "SecureLan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Encrypted LAN chat and file transfer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactStatusPill(
                    text = if (state.discoveryRunning) "Discovery on" else "Discovery off",
                    positive = state.discoveryRunning,
                    modifier = Modifier.weight(1f),
                )
                CompactStatusPill(
                    text = if (state.connected) "Connected" else "Offline",
                    positive = state.connected,
                    modifier = Modifier.weight(1f),
                )
            }
            if (hasError) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CompactStatusPill(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    val background = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = modifier, color = background.copy(alpha = 0.94f), contentColor = foreground, shape = RoundedCornerShape(100.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = positive)
            Spacer(modifier = Modifier.size(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsPanel(
    state: MainUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onOpenLogs: () -> Unit,
) {
    SectionCard(
        title = "Settings",
        subtitle = "Theme and diagnostics controls.",
        trailing = {
            OutlinedButton(onClick = { onExpandedChange(!expanded) }) {
                Text(if (expanded) "Hide" else "Show")
            }
        },
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (state.darkThemeEnabled) "Dark theme is enabled" else "Light theme is enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.darkThemeEnabled, onCheckedChange = onDarkThemeChange)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Diagnostics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${state.logs.size} log entries available to view or copy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onOpenLogs) {
                    Text("Open logs")
                }
            }
        }
    }
}

@Composable
private fun LogsDialog(logs: List<AppLogEntry>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val logText = remember(logs) { logs.joinToString(separator = "\n") { AndroidUiFormatters.formatLogEntry(it) } }
    androidx.compose.material3.AlertDialog(
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
private fun SetupSection(
    state: MainUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    SectionCard(
        title = "Connection",
        subtitle = state.connectionPeer?.let { "Server: ${it.nickname} · ${it.host}:${it.chatPort}" } ?: "Server peer will be selected automatically from discovery.",
        trailing = {
            OutlinedButton(onClick = { onExpandedChange(!expanded) }) {
                Text(if (expanded) "Hide" else "Show")
            }
        },
    ) {
        if (expanded) {
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChange,
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.connected,
            )
            OutlinedTextField(
                value = state.sessionPassword,
                onValueChange = onPasswordChange,
                label = { Text("Session password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.connected,
            )
            SelectedPeerSummary(state.connectionPeer)
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
        }
    }
}

@Composable
private fun SelectedPeerSummary(peer: DiscoveredPeer?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(16.dp),
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
                    text = peer?.let { "${it.host}:${it.chatPort}" } ?: "Wait for discovery or check that desktop discovery is enabled",
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
private fun PeerSection(state: MainUiState, onPeerSelected: (DiscoveredPeer) -> Unit) {
    SectionCard(title = "Peers", subtitle = "${state.peers.size} server/client peer(s) available.") {
        if (state.peers.isEmpty()) {
            EmptyState(
                title = "No desktops yet",
                message = "Keep the desktop room open, enable discovery, and make sure both devices use the same Wi‑Fi.",
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.peers) { peer ->
                    PeerRow(
                        peer = peer,
                        selected = state.selectedPeer?.peerId == peer.peerId,
                        onClick = { onPeerSelected(peer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PeerRow(peer: DiscoveredPeer, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarLetter(peer.nickname)
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(peer.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = contentColor)
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
                text = if (selected) "Selected" else "Tap",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.82f),
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

@Composable
private fun ChatSection(state: MainUiState, onInputChange: (String) -> Unit, onSendMessage: () -> Unit) {
    val chatListState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            chatListState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    SectionCard(title = "Chat", subtitle = if (state.connected) "End-to-end encrypted session is active." else "Connect to a peer to start messaging.") {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(228.dp),
            state = chatListState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    EmptyState(
                        title = "No messages yet",
                        message = "Messages from you and your desktop peer will appear here.",
                    )
                }
            } else {
                items(state.messages) { line -> ChatBubble(line) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
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
                maxLines = 3,
            )
            Button(
                onClick = onSendMessage,
                enabled = state.connected && state.inputMessage.isNotBlank(),
                modifier = Modifier.height(56.dp),
            ) {
                Text("Send")
            }
        }
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
                .fillMaxWidth(0.84f)
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

private fun linkifiedMessage(text: String, linkColor: Color) = buildAnnotatedString {
    var currentIndex = 0
    UrlRegex.findAll(text).forEach { match ->
        append(text.substring(currentIndex, match.range.first))
        val rawUrl = match.value.trimEnd('.', ',', ';', ':', ')', ']', '}')
        val trailing = match.value.substring(rawUrl.length)
        pushStringAnnotation(tag = UrlAnnotationTag, annotation = rawUrl.withUrlScheme())
        pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            append(rawUrl)
        pop()
        pop()
        append(trailing)
        currentIndex = match.range.last + 1
    }
    append(text.substring(currentIndex))
}

private fun String.withUrlScheme(): String = if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
    this
} else {
    "https://$this"
}

private const val UrlAnnotationTag = "URL"

private val UrlRegex = Regex("""(?i)\b((?:https?://|www\.)[^\s<>()]+|[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+[^\s<>()]*)""")

@Composable
private fun FileSection(
    state: MainUiState,
    onPickFile: () -> Unit,
    onSendFile: () -> Unit,
    onStartFileReceiver: () -> Unit,
    onStopFileReceiver: () -> Unit,
) {
    SectionCard(title = "Files", subtitle = "Send one file to the selected peer or receive from desktop.") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
            shape = RoundedCornerShape(16.dp),
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
                state.selectedFile?.let {
                    Text(AndroidUiFormatters.formatBytes(it.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
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
        if (state.fileProgress.active || state.fileProgress.bytesSent > 0) {
            TransferProgress(
                title = if (state.fileProgress.active) "Sending file" else "Last sent file",
                progress = state.fileProgress.percent,
                currentBytes = state.fileProgress.bytesSent,
                totalBytes = state.fileProgress.totalBytes,
                error = state.fileProgress.error,
            )
        }
        if (state.incomingFileProgress.active || state.incomingFileProgress.bytesReceived > 0 || state.incomingFileProgress.error != null) {
            TransferProgress(
                title = if (state.incomingFileProgress.active) "Receiving ${state.incomingFileProgress.fileName.ifBlank { "file" }}" else "Incoming file",
                progress = state.incomingFileProgress.percent,
                currentBytes = state.incomingFileProgress.bytesReceived,
                totalBytes = state.incomingFileProgress.totalBytes,
                error = state.incomingFileProgress.error,
            )
            state.incomingFileProgress.completedPath?.let {
                Text(
                    text = "Saved to: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun TransferProgress(title: String, progress: Float, currentBytes: Long, totalBytes: Long, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            text = "${AndroidUiFormatters.formatBytes(currentBytes)} / ${AndroidUiFormatters.formatBytes(totalBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
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
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun GradientCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun InfoPill(text: String, positive: Boolean) {
    val background = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    val foreground = if (positive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(100.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(active = positive)
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (active) SuccessGreen else MaterialTheme.colorScheme.outline),
    )
}

@Composable
private fun AvatarLetter(name: String) {
    Surface(
        modifier = Modifier.size(42.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
