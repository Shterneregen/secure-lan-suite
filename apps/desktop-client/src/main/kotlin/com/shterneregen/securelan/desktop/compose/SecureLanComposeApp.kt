package com.shterneregen.securelan.desktop.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val PanelShape = RoundedCornerShape(16.dp)
private val SectionShape = RoundedCornerShape(12.dp)
private val FieldShape = RoundedCornerShape(10.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

@Composable
fun SecureLanComposeApp(hostAdapter: ComposeDesktopHostAdapter? = null) {
    var darkTheme by remember { mutableStateOf(true) }

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

@Composable
private fun ComposeShellContent(
    hostAdapter: ComposeDesktopHostAdapter? = null,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (hostAdapter != null) {
            LiveComposeShellContent(hostAdapter, darkTheme, onThemeToggle)
        } else {
            ComposeStatusBar(
                state = ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE,
                darkTheme = darkTheme,
                onThemeToggle = onThemeToggle,
            )
            ComposeConnectionHeaderPreview(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE)
            MainWorkspaceRow(
                parityState = ComposeShellMetadata.DEFAULT_WORKSPACE_PARITY_STATE,
                peersColumn = { PeerListPreviewCard(ComposeShellMetadata.DEFAULT_PEER_LIST_STATE) },
                chatColumn = { ChatWorkspacePreviewCard(ComposeShellMetadata.DEFAULT_CHAT_WORKSPACE_STATE) },
                actionsColumn = { PreviewActionsColumn() },
            )
        }
    }
}

@Composable
private fun LiveComposeShellContent(
    hostAdapter: ComposeDesktopHostAdapter,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    var selectedPeerKey by remember { mutableStateOf<String?>(null) }
    var selectedTargetKind by remember { mutableStateOf<ComposePeerTargetCommandKind?>(null) }
    val peers = hostAdapter.visiblePeerItems.map { peer -> ComposePeerListItem.fromPeer(peer, hostAdapter.chatConnected) }
    val defaultSelectedPeerIndex = if (selectedPeerKey == null) {
        peers.indexOfFirst { it.online }.takeIf { it >= 0 } ?: peers.indices.firstOrNull() ?: -1
    } else {
        -1
    }
    val peerState = ComposePeerListState(
        peers = peers,
        selectedPeerIndex = defaultSelectedPeerIndex,
        selectedPeerNickname = selectedPeerKey,
        selectedTargetKind = selectedTargetKind,
    )
    if (selectedPeerKey != null && peerState.selectedPeer == null) {
        selectedPeerKey = null
        selectedTargetKind = null
    }

    ComposeStatusBar(
        state = hostAdapter.statusState,
        peerStatus = peerState.peerStatus,
        darkTheme = darkTheme,
        onThemeToggle = onThemeToggle,
    )
    ComposeConnectionHeader(hostAdapter)
    MainWorkspaceRow(
        parityState = ComposeShellMetadata.DEFAULT_WORKSPACE_PARITY_STATE,
        peersColumn = {
            LivePeerListCard(
                hostAdapter = hostAdapter,
                peerState = peerState,
                onPeerSelected = { key ->
                    selectedPeerKey = key
                    selectedTargetKind = null
                },
                onTargetKindSelected = { kind -> selectedTargetKind = kind },
                onManualPeersCleared = {
                    selectedPeerKey = null
                    selectedTargetKind = null
                },
            )
        },
        chatColumn = { LiveChatWorkspaceCard(hostAdapter, peerState) },
        actionsColumn = { LiveActionsColumn(hostAdapter, peerState) },
    )
}

@Composable
private fun ComposeStatusBar(
    state: ComposeStatusConnectionState,
    peerStatus: String = "Peer not selected",
    voiceStatus: String = "Voice idle",
    transferStatus: String = "Transfers idle",
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, panelBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(state.serverStatus)
            StatusChip(state.connectionStatus)
            StatusChip(peerStatus)
            StatusChip(voiceStatus)
            StatusChip(transferStatus)
            Box(modifier = Modifier.weight(1f))
            CompactButton(onClick = onThemeToggle) { Text(if (darkTheme) "Dark theme" else "Light theme") }
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = if (MaterialTheme.colors.isLight) MaterialTheme.colors.background else MaterialTheme.colors.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIndicator(text)
            Text(text, style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
private fun StatusIndicator(text: String) {
    val color = when {
        text.contains("running", ignoreCase = true) || text.contains("connected", ignoreCase = true) -> MaterialTheme.colors.primary
        text.contains("error", ignoreCase = true) || text.contains("failed", ignoreCase = true) -> MaterialTheme.colors.error
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
    }
    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2f, center = Offset(size.width / 2f, size.height / 2f))
    }
}

@Composable
private fun ComposeConnectionHeader(hostAdapter: ComposeDesktopHostAdapter) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) { ServerQuickPanel(hostAdapter) }
        Box(modifier = Modifier.weight(0.85f)) { ManualConnectionPanel(hostAdapter) }
    }
}

@Composable
private fun ComposeConnectionHeaderPreview(state: ComposeStatusConnectionState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) { ServerQuickPanelPreview(state) }
        Box(modifier = Modifier.weight(0.85f)) { ManualConnectionPanelPreview(state) }
    }
}

@Composable
private fun MainWorkspaceRow(
    parityState: ComposeJavaFxWorkspaceParityState,
    peersColumn: @Composable () -> Unit,
    chatColumn: @Composable () -> Unit,
    actionsColumn: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MainWorkspaceColumn(
            title = parityState.workspaceColumns[0].title,
            modifier = Modifier.weight(parityState.workspaceColumns[0].weight).fillMaxHeight(),
            content = peersColumn,
        )
        MainWorkspaceColumn(
            title = parityState.workspaceColumns[1].title,
            modifier = Modifier.weight(parityState.workspaceColumns[1].weight).fillMaxHeight(),
            content = chatColumn,
        )
        MainWorkspaceColumn(
            title = parityState.workspaceColumns[2].title,
            modifier = Modifier.weight(parityState.workspaceColumns[2].weight).fillMaxHeight(),
            content = actionsColumn,
        )
    }
}

@Composable
private fun MainWorkspaceColumn(
    title: String,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = PanelShape,
        border = BorderStroke(1.dp, panelBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) { content() }
        }
    }
}

@Composable
private fun LiveActionsColumn(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val presentation = ComposeShellMetadata.DEFAULT_ACTIONS_PRESENTATION_STATE
    val quickActions = ComposeSelectedPeerQuickActionsState(
        peerListState = peerState,
        clientConnected = hostAdapter.statusState.clientConnected,
        hangUpReady = hostAdapter.mediaVoiceState.canHangUp || hostAdapter.experimentalVideoState.canHangUp,
    )
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.SELECTED_PEER)) {
            SelectedPeerQuickActionsCard(
                state = quickActions,
                onAttach = {},
                onVoice = {
                    peerState.selectedPeer?.let { peer ->
                        hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO)
                    }
                },
                onVideo = {
                    peerState.selectedPeer?.let { peer ->
                        hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO_VIDEO)
                    }
                },
                onHangUp = hostAdapter::closeRealtimeSession,
            )
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.TRANSFERS)) {
            LiveFileTransferCard(hostAdapter, peerState)
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.QUICK_SHARE)) {
            LiveQuickShareCard(hostAdapter)
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.STEGANOGRAPHY)) {
            LiveSteganographyCard(hostAdapter)
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.MEDIA_DEVICES)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LiveMediaVoiceCard(hostAdapter, peerState)
                LiveExperimentalVideoCard(hostAdapter, peerState)
            }
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.RUNTIME_DIAGNOSTICS)) {
            LiveRuntimeDiagnosticsCard(hostAdapter)
        }
    }
}

@Composable
private fun ActionsColumnSection(
    presentation: ComposeActionsSectionPresentation,
    content: @Composable () -> Unit,
) {
    var expanded by remember(presentation.kind) { mutableStateOf(presentation.expandedByDefault) }
    val shape = RoundedCornerShape(12.dp)
    val borderColor = MaterialTheme.colors.onSurface.copy(alpha = if (expanded) 0.18f else 0.12f)
    val headerColor = MaterialTheme.colors.surface.copy(alpha = if (expanded) 0.98f else 0.86f)
    val contentColor = MaterialTheme.colors.background.copy(alpha = 0.55f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        elevation = 0.dp,
        backgroundColor = if (expanded) contentColor else headerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "▾" else "▸",
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
                Text(presentation.title, style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f))
                Text(
                    text = if (expanded) "Hide" else "Show",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                )
            }
            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun HeaderCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(1.dp, panelBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            content()
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    onSubmit: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface)
        Surface(
            modifier = Modifier.weight(1f).heightIn(min = 34.dp),
            shape = FieldShape,
            border = BorderStroke(1.dp, sectionBorderColor()),
            color = fieldBackgroundColor(),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onSubmit == null) {
                            Modifier
                        } else {
                            Modifier.onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                    onSubmit()
                                    true
                                } else {
                                    false
                                }
                            }
                        },
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                visualTransformation = visualTransformation,
            )
        }
    }
}

@Composable
private fun ContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp), content = content)
    }
}

@Composable
private fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 30.dp),
        shape = ButtonShape,
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        content = content,
    )
}

@Composable
private fun panelBorderColor() = if (MaterialTheme.colors.isLight) {
    androidx.compose.ui.graphics.Color(0xFFD7DFEB)
} else {
    androidx.compose.ui.graphics.Color(0xFF243247)
}

@Composable
private fun sectionBorderColor() = if (MaterialTheme.colors.isLight) {
    androidx.compose.ui.graphics.Color(0xFFDFE6F1)
} else {
    androidx.compose.ui.graphics.Color(0xFF2A3950)
}

@Composable
private fun fieldBackgroundColor() = if (MaterialTheme.colors.isLight) {
    androidx.compose.ui.graphics.Color.White
} else {
    androidx.compose.ui.graphics.Color(0xFF0F1723)
}

@Composable
private fun SelectedPeerQuickActionsCard(
    state: ComposeSelectedPeerQuickActionsState,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    onVideo: () -> Unit,
    onHangUp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(state.title, style = MaterialTheme.typography.subtitle1)
        Text(
            text = state.meta,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
        )
        Text(
            text = state.readinessLabel,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
        )
        SelectedPeerQuickActions(
            attachEnabled = state.attachEnabled,
            voiceEnabled = state.voiceEnabled,
            videoEnabled = state.videoEnabled,
            hangUpEnabled = state.hangUpEnabled,
            onAttach = onAttach,
            onVoice = onVoice,
            onVideo = onVideo,
            onHangUp = onHangUp,
        )
    }
}

@Composable
private fun PreviewActionsColumn() {
    val presentation = ComposeShellMetadata.DEFAULT_ACTIONS_PRESENTATION_STATE
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.SELECTED_PEER)) {
            SelectedPeerQuickActionsCard(
                state = ComposeShellMetadata.DEFAULT_SELECTED_PEER_QUICK_ACTIONS_STATE,
                onAttach = {},
                onVoice = {},
                onVideo = {},
                onHangUp = {},
            )
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.TRANSFERS)) {
            FileTransferPreviewCard(ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE)
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.QUICK_SHARE)) {
            QuickSharePreviewCard(ComposeShellMetadata.DEFAULT_QUICK_SHARE_STATE)
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.STEGANOGRAPHY)) {
            SteganographyPreviewCard(ComposeShellMetadata.DEFAULT_STEGO_STATE)
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.MEDIA_DEVICES)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MediaVoicePreviewCard(ComposeShellMetadata.DEFAULT_MEDIA_VOICE_STATE)
                ExperimentalVideoPreviewCard(ComposeShellMetadata.DEFAULT_VIDEO_STATE)
            }
        }
        ActionsColumnSection(presentation.section(ComposeActionsSectionKind.RUNTIME_DIAGNOSTICS)) {
            RuntimeDiagnosticsPreviewCard()
        }
    }
}

@Composable
private fun FileTransferPreviewCard(state: ComposeFileTransferState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Transfers", style = MaterialTheme.typography.subtitle1)
        Text(
            text = state.hint,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
        Text(
            text = if (state.entryRows.isEmpty()) "Transfers will appear here." else state.entryRows.joinToString(" · "),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
    }
}

@Composable
private fun QuickSharePreviewCard(state: ComposeQuickShareState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("LAN browser quick share", style = MaterialTheme.typography.h6)
        Text(
            text = state.trustedLanWarning,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.error,
        )
        Text(state.statusText, style = MaterialTheme.typography.body2)
        Text(
            text = state.landingText,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
        Text(
            text = if (state.shareRows.isEmpty()) "No quick-share rows yet." else state.shareRows.joinToString(" · "),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
        Text(
            text = state.readinessSummary,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
    }
}

@Composable
private fun SelectedPeerQuickActions(
    attachEnabled: Boolean,
    voiceEnabled: Boolean,
    videoEnabled: Boolean,
    hangUpEnabled: Boolean,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    onVideo: () -> Unit,
    onHangUp: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAttach, enabled = attachEnabled) { Text("Attach") }
        Button(onClick = onVoice, enabled = voiceEnabled) { Text("Voice call") }
        Button(onClick = onVideo, enabled = videoEnabled) { Text("Video call") }
        Button(onClick = onHangUp, enabled = hangUpEnabled) { Text("End call") }
    }
}

@Composable
private fun ChatWorkspacePreviewCard(initialState: ComposeChatWorkspaceState) {
    var draftMessage by remember { mutableStateOf(initialState.draftMessage) }
    val previewState = initialState.copy(draftMessage = draftMessage)

    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Shared room activity", style = MaterialTheme.typography.h6)
            Text(
                text = "Connect to chat, then select a peer on the left for voice, video, and file actions.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            previewState.transcriptLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draftMessage,
                    onValueChange = { draftMessage = it },
                    label = { Text("Shared chat message") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {}, enabled = false) {
                    Text(previewState.sendLabel)
                }
            }
            Text(
                text = "${previewState.transcriptSummary} · ${previewState.readinessSummary}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

@Composable
private fun ServerQuickPanel(hostAdapter: ComposeDesktopHostAdapter) {
    var nickname by remember { mutableStateOf(hostAdapter.generateNickname()) }
    var roomPassword by remember { mutableStateOf(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.roomPasswordPlaceholder) }
    var serverChatPort by remember { mutableStateOf(hostAdapter.statusState.serverChatPortText) }
    var serverFilePort by remember { mutableStateOf(hostAdapter.statusState.serverFilePortText) }
    var discoverable by remember { mutableStateOf(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.discoverable) }
    val state = hostAdapter.statusState.copy(
        nickname = nickname,
        roomPasswordPlaceholder = roomPassword,
        serverChatPortText = serverChatPort,
        serverFilePortText = serverFilePort,
        discoverable = discoverable,
    )
    HeaderCard(title = "My profile") {
            Text(
                text = "Set your name and shared room password. Then open a room. Keep Discoverable enabled if peers should find it automatically on the LAN.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactTextField(nickname, { nickname = it }, label = "Your name", modifier = Modifier.weight(1f))
                CompactTextField(roomPassword, { roomPassword = it }, label = "Room password", modifier = Modifier.weight(1f), visualTransformation = PasswordVisualTransformation())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactButton(onClick = { state.serverChatPort?.let { chat -> state.serverFilePort?.let { file -> hostAdapter.openRoom(nickname, roomPassword, chat, file, discoverable) } } }, enabled = state.canOpenRoom) { Text("Open room") }
                CompactButton(onClick = { hostAdapter.stopHosting() }, enabled = state.localServerRunning) { Text("Stop hosting") }
                Checkbox(discoverable, { discoverable = it })
                Text("Discoverable", style = MaterialTheme.typography.body2)
            }
            ComposeAdvancedPane("Advanced network settings") {
                Text(
                    text = "Change these only if another app already uses the default ports.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CompactTextField(serverChatPort, { serverChatPort = it }, label = "Chat port", modifier = Modifier.weight(1f))
                    CompactTextField(serverFilePort, { serverFilePort = it }, label = "File port", modifier = Modifier.weight(1f))
                }
                Text(state.validationSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
    }
}

@Composable
private fun ManualConnectionPanel(hostAdapter: ComposeDesktopHostAdapter) {
    var nickname by remember { mutableStateOf(hostAdapter.statusState.nickname) }
    var roomPassword by remember { mutableStateOf(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.roomPasswordPlaceholder) }
    var manualHost by remember { mutableStateOf(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.manualHost) }
    var clientChatPort by remember { mutableStateOf(hostAdapter.statusState.clientChatPortText) }
    var clientFilePort by remember { mutableStateOf(hostAdapter.statusState.clientFilePortText) }
    val state = hostAdapter.statusState.copy(
        nickname = nickname,
        roomPasswordPlaceholder = roomPassword,
        manualHost = manualHost,
        clientChatPortText = clientChatPort,
        clientFilePortText = clientFilePort,
    )
    HeaderCard(title = "Manual connection") {
            Text(
                text = "Use this when a room was not discovered automatically. Usually you will select a discovered peer from the list.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            CompactTextField(manualHost, { manualHost = it }, label = "Host address", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactButton(onClick = { state.clientChatPort?.let { chat -> state.clientFilePort?.let { file -> hostAdapter.connect(manualHost, nickname, roomPassword, chat, file) } } }, enabled = state.canConnect) { Text("Connect") }
                CompactButton(onClick = { hostAdapter.disconnect() }, enabled = state.clientConnected) { Text("Disconnect") }
            }
            ComposeAdvancedPane("Advanced network settings") {
                Text(
                    text = "Use custom ports only when the host changed them in advanced settings.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CompactTextField(clientChatPort, { clientChatPort = it }, label = "Chat port", modifier = Modifier.weight(1f))
                    CompactTextField(clientFilePort, { clientFilePort = it }, label = "File port", modifier = Modifier.weight(1f))
                }
                Text(state.portSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
    }
}

@Composable
private fun ComposeAdvancedPane(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f))
            Text(
                text = if (expanded) "Hide" else "Show",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = { content() })
        }
    }
}

@Composable
private fun ServerQuickPanelPreview(state: ComposeStatusConnectionState) {
    HeaderCard(title = "My profile") {
            Text("Set your name and shared room password. Then open a room.", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactTextField(state.nickname, {}, label = "Your name", modifier = Modifier.weight(1f))
                CompactTextField(state.roomPasswordPlaceholder, {}, label = "Room password", modifier = Modifier.weight(1f), visualTransformation = PasswordVisualTransformation())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactButton(onClick = {}, enabled = state.canOpenRoom) { Text("Open room") }
                CompactButton(onClick = {}, enabled = state.localServerRunning) { Text("Stop hosting") }
                Checkbox(state.discoverable, {})
                Text("Discoverable", style = MaterialTheme.typography.body2)
            }
    }
}

@Composable
private fun ManualConnectionPanelPreview(state: ComposeStatusConnectionState) {
    HeaderCard(title = "Manual connection") {
            Text("Use this when a room was not discovered automatically.", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            CompactTextField(state.manualHost, {}, label = "Host address", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactButton(onClick = {}, enabled = state.canConnect) { Text("Connect") }
                CompactButton(onClick = {}, enabled = state.clientConnected) { Text("Disconnect") }
            }
    }
}

@Composable
private fun LivePeerListCard(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    onPeerSelected: (String?) -> Unit,
    onTargetKindSelected: (ComposePeerTargetCommandKind?) -> Unit,
    onManualPeersCleared: () -> Unit,
) {
    val visiblePeerItems = peerState.visiblePeers

    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(peerState.title, style = MaterialTheme.typography.subtitle1)
            Text(
                text = peerState.hint,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            ContentSurface(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (visiblePeerItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                        Text(
                            text = "Peers will appear here when they join the chat.",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        visiblePeerItems.forEachIndexed { index, peer ->
                            PeerPreviewRow(
                                peer = peer,
                                selected = index == peerState.resolvedSelectedPeerIndex,
                                onSelect = { onPeerSelected(peer.nickname) },
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Peers: ${visiblePeerItems.size}", style = MaterialTheme.typography.subtitle2)
                Text(peerState.selectedPeerTitle, style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
private fun LiveChatWorkspaceCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    var draftMessage by remember { mutableStateOf("") }
    val transcript = hostAdapter.chatTranscript.takeLast(20)
    val selectedPeer = peerState.selectedPeer
    fun sendDraftMessage() {
        if (draftMessage.isNotBlank() && hostAdapter.chatConnected) {
            hostAdapter.sendMessage(draftMessage.trim())
            draftMessage = ""
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Shared room activity", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = selectedPeer?.let { "Actions on the right will target “${it.nickname}”. Text chat remains shared for now." }
                            ?: "Connect to chat, then select a peer on the left for voice, video, and file actions.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                }
                CompactButton(
                    onClick = {
                        selectedPeer?.let { peer -> hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO) }
                    },
                    enabled = hostAdapter.chatConnected && selectedPeer?.online == true,
                ) { Text("Voice call") }
                CompactButton(
                    onClick = {
                        selectedPeer?.let { peer -> hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO_VIDEO) }
                    },
                    enabled = hostAdapter.chatConnected && selectedPeer?.online == true,
                ) { Text("Video call") }
                CompactButton(
                    onClick = hostAdapter::closeRealtimeSession,
                    enabled = hostAdapter.mediaVoiceState.canHangUp || hostAdapter.experimentalVideoState.canHangUp,
                ) { Text("End call") }
            }
            if (hostAdapter.experimentalVideoState.previewRunning || hostAdapter.experimentalVideoState.currentSession != null) {
                ComposeVideoStage(hostAdapter.experimentalVideoState.copy(peerListState = peerState))
            }
            ContentSurface(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (transcript.isEmpty()) {
                    Text(
                        text = "No messages yet. Connect to a room to send and receive chat messages.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                    )
                } else {
                    transcript.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactButton(onClick = {}, enabled = false) { Text("Attach") }
                CompactTextField(
                    draftMessage,
                    { draftMessage = it },
                    label = "Message",
                    modifier = Modifier.weight(1f),
                    onSubmit = ::sendDraftMessage,
                )
                CompactButton(
                    onClick = ::sendDraftMessage,
                    enabled = draftMessage.isNotBlank() && hostAdapter.chatConnected,
                ) {
                    Text("Send")
                }
            }
            Text(
                text = if (hostAdapter.chatConnected) "Connected — ready to send and receive messages." else "Not connected — open a room or connect to a peer first.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

@Composable
private fun LiveFileTransferCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    var filePath by remember { mutableStateOf("") }
    var senderId by remember { mutableStateOf(hostAdapter.statusState.nickname) }
    var sessionPassword by remember { mutableStateOf("") }
    var autoAcceptFiles by remember { mutableStateOf(false) }
    val selectedPeer = peerState.selectedPeer
        ?.takeIf { it.online && it.discovered }
        ?.let { selected ->
            hostAdapter.visiblePeerItems.firstOrNull { peer -> peer.nickname().equals(selected.nickname, ignoreCase = true) && peer.discovered() }
                ?.let { peer ->
                    com.shterneregen.securelan.chat.discovery.DiscoveredPeer(
                        peer.peerId() ?: "peer-${peer.nickname().lowercase()}",
                        peer.nickname(),
                        peer.host().orEmpty(),
                        peer.chatPort(),
                        peer.filePort(),
                        peer.lastSeen() ?: java.time.Instant.now(),
                    )
                }
        }
    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        selectedFilePath = filePath,
        senderId = senderId,
        sessionPassword = sessionPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = autoAcceptFiles,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Transfers", style = MaterialTheme.typography.subtitle1)
            Text(
                text = transferState.hint,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(autoAcceptFiles, { autoAcceptFiles = it })
                Text("Accept files without confirmation", style = MaterialTheme.typography.body2)
            }
            ContentSurface(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (transferState.entryRows.isEmpty()) "Transfers will appear here." else transferState.entryRows.joinToString("\n"),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
            ComposeAdvancedPane("Send encrypted file") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CompactTextField(filePath, { filePath = it }, label = "File", modifier = Modifier.weight(1f))
                        CompactButton(onClick = { openComposeFileChooser("Choose file to send to ${transferState.selectedPeerName}")?.let { filePath = it.toString() } }) { Text("Choose") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CompactTextField(senderId, { senderId = it }, label = "Sender", modifier = Modifier.weight(1f))
                        CompactTextField(sessionPassword, { sessionPassword = it }, label = "Password", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.weight(1f))
                    }
                    CompactButton(
                        onClick = {
                            val peer = selectedPeer ?: return@CompactButton
                            hostAdapter.sendFileToPeer(Path.of(filePath), senderId, peer, sessionPassword)
                        },
                        enabled = transferState.canSendSelectedFile && selectedPeer != null,
                    ) { Text("Send encrypted file") }
                }
            }
            transferState.incomingPrompts.forEach { prompt ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(prompt.content, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
                    Button(onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, true) }) { Text("Accept") }
                    Button(onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, false) }) { Text("Decline") }
                }
            }
            hostAdapter.transferDiagnostics.takeLast(2).forEach { diagnostic ->
                Text("• $diagnostic", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
            Text(
                text = transferState.readinessSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

@Composable
private fun LiveQuickShareCard(hostAdapter: ComposeDesktopHostAdapter) {
    var quickSharePort by remember { mutableStateOf(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.serverFilePortText.replace("5051", "5053")) }
    var filePath by remember { mutableStateOf("") }
    var textDraft by remember { mutableStateOf("SecureLanSuite quick-share text") }
    var expirationMinutes by remember { mutableStateOf("10") }
    var accessLimit by remember { mutableStateOf("3") }
    val quickShareState = ComposeQuickShareState(
        running = hostAdapter.quickShareRunning,
        portText = quickSharePort,
        selectedFilePath = filePath,
        textDraft = textDraft,
        expirationMinutesText = expirationMinutes,
        accessLimitText = accessLimit,
        entries = hostAdapter.quickShareEntries,
        landingUrls = hostAdapter.quickShareLandingUrls,
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("LAN browser quick share", style = MaterialTheme.typography.h6)
            Text(
                text = quickShareState.trustedLanWarning,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = quickSharePort,
                    onValueChange = { quickSharePort = it },
                    label = { Text("Port") },
                    modifier = Modifier.weight(0.5f),
                )
                OutlinedTextField(
                    value = expirationMinutes,
                    onValueChange = { expirationMinutes = it },
                    label = { Text("Minutes") },
                    modifier = Modifier.weight(0.5f),
                )
                OutlinedTextField(
                    value = accessLimit,
                    onValueChange = { accessLimit = it },
                    label = { Text("Access limit") },
                    modifier = Modifier.weight(0.5f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { quickShareState.port?.let(hostAdapter::startQuickShare) },
                    enabled = quickShareState.canStartServer,
                ) { Text("Start share server") }
                Button(
                    onClick = { hostAdapter.stopQuickShare() },
                    enabled = quickShareState.canStopServer,
              ) { Text("Stop share server") }
                Button(
                    onClick = { copyToSystemClipboard(hostAdapter.quickShareLandingUrls.firstOrNull().orEmpty()) },
                    enabled = quickShareState.canCopyIndex,
                ) { Text("Copy index link") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    label = { Text("File quick-share path") },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { openComposeFileChooser("Choose file to share by LAN browser link")?.let { filePath = it.toString() } },
                ) { Text("Choose file") }
                Button(
                    onClick = {
                        hostAdapter.createFileQuickShare(
                            Path.of(filePath),
                            quickShareState.expirationMinutes ?: return@Button,
                            quickShareState.accessLimit ?: return@Button,
                        )
                    },
                    enabled = quickShareState.canCreateFileShare,
                ) { Text("Share file") }
            }
            OutlinedTextField(
                value = textDraft,
                onValueChange = { textDraft = it },
                label = { Text("Text quick-share") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    hostAdapter.createTextQuickShare(
                        textDraft,
                        quickShareState.expirationMinutes ?: return@Button,
                        quickShareState.accessLimit ?: return@Button,
                    )
                },
                enabled = quickShareState.canCreateTextShare,
            ) { Text("Share text") }
            Text(hostAdapter.quickShareStatus, style = MaterialTheme.typography.body2)
            Text(
                text = hostAdapter.quickShareLanding,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            if (quickShareState.shareRows.isEmpty()) {
                Text(
                    text = "No quick-share rows yet.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                )
            } else {
                quickShareState.shareRows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
                        val entry = hostAdapter.quickShareEntries.getOrNull(index)
                        Button(
                            onClick = { entry?.let { hostAdapter.stopQuickShareEntry(it.id()) } },
                            enabled = entry?.active() == true,
                        ) { Text("Stop") }
                    }
                }
            }
            hostAdapter.quickShareDiagnostics.takeLast(2).forEach { diagnostic ->
                Text("• $diagnostic", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
            Text(
                text = quickShareState.readinessSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

@Composable
private fun LiveSteganographyCard(hostAdapter: ComposeDesktopHostAdapter) {
    var coverPath by remember { mutableStateOf(hostAdapter.stegoState.coverPathText) }
    var inputPath by remember { mutableStateOf(hostAdapter.stegoState.inputPathText) }
    var outputPath by remember { mutableStateOf(hostAdapter.stegoState.outputPathText) }
    var message by remember { mutableStateOf(hostAdapter.stegoState.messageDraft) }
    var password by remember { mutableStateOf("") }
    var encrypt by remember { mutableStateOf(false) }
    var encryptedExtract by remember { mutableStateOf(false) }
    val stegoState = hostAdapter.stegoState.copy(
        coverPathText = coverPath,
        inputPathText = inputPath,
        outputPathText = outputPath,
        messageDraft = message,
        passwordDraft = password,
        encryptPayload = encrypt,
        encryptedExtract = encryptedExtract,
    )
    fun selectCover(path: Path) {
        val normalized = path.toAbsolutePath().normalize()
        coverPath = normalized.toString()
        if (outputPath.isBlank()) {
            outputPath = DesktopMainViewHelpers.suggestedStegoOutputPath(normalized).toString()
        }
        hostAdapter.inspectStegoCover(normalized)
    }

    SteganographyCardContent(
        state = stegoState,
        coverPath = coverPath,
        onCoverPathChange = { coverPath = it },
        onChooseCover = {
            openComposeFileChooser("Choose cover image", ComposeImageFiles)?.let(::selectCover)
        },
        inputPath = inputPath,
        onInputPathChange = { inputPath = it },
        onChooseInput = {
            openComposeFileChooser("Choose image with hidden message", ComposeImageFiles)?.let { path ->
                val normalized = path.toAbsolutePath().normalize()
                inputPath = normalized.toString()
            }
        },
        outputPath = outputPath,
        onOutputPathChange = { outputPath = it },
        onChooseOutput = {
            val initial = coverPath.trim().takeIf(String::isNotEmpty)?.let { DesktopMainViewHelpers.suggestedStegoOutputPath(Path.of(it)) }
            openComposeFileChooser("Save stego BMP image", ComposeBmpFiles, save = true, initialFile = initial)?.let { path ->
                outputPath = DesktopMainViewHelpers.ensureBmpExtension(path.toAbsolutePath().normalize()).toString()
            }
        },
        message = message,
        onMessageChange = { message = it },
        password = password,
        onPasswordChange = { password = it },
        encrypt = encrypt,
        onEncryptChange = { encrypt = it },
        encryptedExtract = encryptedExtract,
        onEncryptedExtractChange = { encryptedExtract = it },
        onInspect = { coverPath.trim().takeIf(String::isNotEmpty)?.let { path -> hostAdapter.inspectStegoCover(Path.of(path)) } },
        onHide = {
            val output = outputPath.ifBlank { coverPath.trim().takeIf(String::isNotEmpty)?.let { Path.of(it) }?.let { path ->
                com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers.suggestedStegoOutputPath(path).toString()
            }.orEmpty() }
            if (coverPath.isNotBlank() && output.isNotBlank()) {
                hostAdapter.hideStegoMessage(Path.of(coverPath), Path.of(output), message, password.takeIf { encrypt })
            }
        },
        onExtract = { inputPath.trim().takeIf(String::isNotEmpty)?.let { path -> hostAdapter.extractStegoMessage(Path.of(path), password.takeIf { encryptedExtract }) } },
    )
}

@Composable
private fun ComposeVideoStage(state: ComposeExperimentalVideoState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.stageTitle, style = MaterialTheme.typography.subtitle1)
                    Text(state.frameCaption, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
                }
                Text(state.stageBadge, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Participants: ${state.selectedPeerName}", style = MaterialTheme.typography.caption)
                Text("Media: ${state.mediaLabel}", style = MaterialTheme.typography.caption)
                Text("Preview: ${state.previewStatus}", style = MaterialTheme.typography.caption)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VideoSurfacePlaceholder(
                    title = "Remote video",
                    body = state.frameCaption,
                    modifier = Modifier.weight(1f).height(180.dp),
                )
                VideoSurfacePlaceholder(
                    title = "Local preview",
                    body = state.previewStatus,
                    modifier = Modifier.width(220.dp).height(150.dp),
                )
            }
        }
    }
}

@Composable
private fun VideoSurfacePlaceholder(
    title: String,
    body: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.background,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            Text(body, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f))
        }
    }
}

@Composable
private fun SteganographyPreviewCard(initialState: ComposeSteganographyState) {
    var coverPath by remember { mutableStateOf(initialState.coverPathText) }
    var inputPath by remember { mutableStateOf(initialState.inputPathText) }
    var outputPath by remember { mutableStateOf(initialState.outputPathText) }
    var message by remember { mutableStateOf(initialState.messageDraft) }
    var password by remember { mutableStateOf(initialState.passwordDraft) }
    var encrypt by remember { mutableStateOf(initialState.encryptPayload) }
    var encryptedExtract by remember { mutableStateOf(initialState.encryptedExtract) }
    val state = initialState.copy(
        coverPathText = coverPath,
        inputPathText = inputPath,
        outputPathText = outputPath,
        messageDraft = message,
        passwordDraft = password,
        encryptPayload = encrypt,
        encryptedExtract = encryptedExtract,
    )
    SteganographyCardContent(
        state = state,
        coverPath = coverPath,
        onCoverPathChange = { coverPath = it },
        onChooseCover = {},
        inputPath = inputPath,
        onInputPathChange = { inputPath = it },
        onChooseInput = {},
        outputPath = outputPath,
        onOutputPathChange = { outputPath = it },
        onChooseOutput = {},
        message = message,
        onMessageChange = { message = it },
        password = password,
        onPasswordChange = { password = it },
        encrypt = encrypt,
        onEncryptChange = { encrypt = it },
        encryptedExtract = encryptedExtract,
        onEncryptedExtractChange = { encryptedExtract = it },
        onInspect = {},
        onHide = {},
        onExtract = {},
        previewOnly = true,
    )
}

@Composable
private fun SteganographyCardContent(
    state: ComposeSteganographyState,
    coverPath: String,
    onCoverPathChange: (String) -> Unit,
    onChooseCover: () -> Unit,
    inputPath: String,
    onInputPathChange: (String) -> Unit,
    onChooseInput: () -> Unit,
    outputPath: String,
    onOutputPathChange: (String) -> Unit,
    onChooseOutput: () -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    encrypt: Boolean,
    onEncryptChange: (Boolean) -> Unit,
    encryptedExtract: Boolean,
    onEncryptedExtractChange: (Boolean) -> Unit,
    onInspect: () -> Unit,
    onHide: () -> Unit,
    onExtract: () -> Unit,
    previewOnly: Boolean = false,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.title, style = MaterialTheme.typography.h6)
            Text(
                text = "Choose PNG, BMP, JPG, or JPEG images. Non-BMP cover images are converted to BMP for output.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(coverPath, onCoverPathChange, label = { Text("Cover image path") }, modifier = Modifier.weight(1f))
                Button(onClick = onChooseCover, enabled = !previewOnly) { Text("Cover BMP") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(outputPath, onOutputPathChange, label = { Text("Output BMP path") }, modifier = Modifier.weight(1f))
                Button(onClick = onChooseOutput, enabled = !previewOnly) { Text("Save as") }
            }
            OutlinedTextField(message, onMessageChange, label = { Text("Message to hide") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(encrypt, onEncryptChange)
                Text("Encrypt with password")
            }
            OutlinedTextField(password, onPasswordChange, label = { Text("Stego password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onInspect, enabled = !previewOnly && state.canInspectCover) { Text("Inspect BMP") }
                Button(onClick = onHide, enabled = !previewOnly && state.canHideMessage) { Text(state.hideLabel) }
                Button(
                    onClick = {
                        onCoverPathChange("")
                        onInputPathChange("")
                        onOutputPathChange("")
                        onMessageChange("")
                        onPasswordChange("")
                        onEncryptChange(false)
                        onEncryptedExtractChange(false)
                    },
                    enabled = !previewOnly,
                ) { Text("Clear") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(inputPath, onInputPathChange, label = { Text("Input BMP with payload") }, modifier = Modifier.weight(1f))
                Button(onClick = onChooseInput, enabled = !previewOnly) { Text("Stego BMP") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(encryptedExtract, onEncryptedExtractChange)
                Text("Extract encrypted message")
                Button(onClick = onExtract, enabled = !previewOnly && state.canExtractMessage) { Text(state.extractLabel) }
            }
            Text(state.capacityText, style = MaterialTheme.typography.caption)
            Text(state.statusText, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            Text(state.extractedSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            Text(state.readinessSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
        }
    }
}

private data class ComposeFileChooserFilter(
    val description: String,
    val extensions: Array<String>,
)

private val ComposeImageFiles = ComposeFileChooserFilter("Image files", arrayOf("bmp", "png", "jpg", "jpeg", "gif"))
private val ComposeBmpFiles = ComposeFileChooserFilter("BMP images", arrayOf("bmp"))

private fun openComposeFileChooser(
    title: String,
    filter: ComposeFileChooserFilter? = null,
    save: Boolean = false,
    initialFile: Path? = null,
): Path? {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.FILES_ONLY
        filter?.let { fileFilter = FileNameExtensionFilter(it.description, *it.extensions) }
        initialFile?.let { path ->
            val normalized = path.toAbsolutePath().normalize()
            val parent = normalized.parent
            if (parent != null) {
                currentDirectory = parent.toFile()
            }
            selectedFile = File(normalized.fileName.toString())
        }
    }
    val result = if (save) chooser.showSaveDialog(null) else chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath().toAbsolutePath().normalize() else null
}

@Composable
private fun LiveMediaVoiceCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val state = hostAdapter.mediaVoiceState.copy(peerListState = peerState)
    MediaVoiceCardContent(
        state = state,
        onRefresh = hostAdapter::refreshMediaDevices,
        onMicrophoneSelected = hostAdapter::selectMicrophone,
        onTestMicrophone = { hostAdapter.testMicrophone() },
        onStartVoice = {
            state.selectedPeer?.let { peer ->
                hostAdapter.startRealtimeSession(
                    localPeer = hostAdapter.statusState.nickname,
                    remotePeer = peer.nickname,
                    mode = RtcSessionMode.AUDIO,
                )
            }
        },
        onHangUp = hostAdapter::closeRealtimeSession,
        diagnostics = hostAdapter.realtimeDiagnostics,
    )
}

@Composable
private fun MediaVoicePreviewCard(initialState: ComposeMediaVoiceState) {
    MediaVoiceCardContent(initialState, {}, {}, {}, {}, {}, emptyList(), previewOnly = true)
}

@Composable
private fun MediaVoiceCardContent(
    state: ComposeMediaVoiceState,
    onRefresh: () -> Unit,
    onMicrophoneSelected: (String?) -> Unit,
    onTestMicrophone: () -> Unit,
    onStartVoice: () -> Unit,
    onHangUp: () -> Unit,
    diagnostics: List<String>,
    previewOnly: Boolean = false,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.title, style = MaterialTheme.typography.h6)
            Text("Runtime: ${state.runtimeLabel} · Target: ${state.selectedPeerName}", style = MaterialTheme.typography.body2)
            DeviceChoiceDropdown(
                label = "Microphone",
                choices = state.microphones,
                selected = state.selectedMicrophone,
                enabled = !previewOnly,
                onSelected = onMicrophoneSelected,
            )
            Text(state.voiceStatusText, style = MaterialTheme.typography.body2)
            Text("${state.localAudioLabel} · ${state.remoteAudioLabel}", style = MaterialTheme.typography.caption)
            Text(state.microphoneTestStatus, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh, enabled = !previewOnly && state.canRefreshDevices) { Text("Refresh devices") }
                Button(onClick = onTestMicrophone, enabled = !previewOnly && state.canTestMicrophone) { Text("Test mic") }
                Button(onClick = onStartVoice, enabled = !previewOnly && state.canStartVoice) { Text(state.startVoiceLabel) }
                Button(onClick = onHangUp, enabled = !previewOnly && state.canHangUp) { Text("Hang up") }
            }
            diagnostics.takeLast(4).forEach { Text("• $it", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)) }
            Text(state.readinessSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
        }
    }
}

@Composable
private fun LiveExperimentalVideoCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val state = hostAdapter.experimentalVideoState.copy(peerListState = peerState)
    ExperimentalVideoCardContent(
        state = state,
        onRefresh = hostAdapter::refreshMediaDevices,
        onCameraSelected = hostAdapter::selectCamera,
        onTestCamera = { hostAdapter.testCamera() },
        onStartPreview = { hostAdapter.startCameraPreview() },
        onStopPreview = hostAdapter::closeCameraPreview,
        onStartVideo = {
            state.selectedPeer?.let { peer ->
                hostAdapter.startRealtimeSession(
                    localPeer = hostAdapter.statusState.nickname,
                    remotePeer = peer.nickname,
                    mode = RtcSessionMode.AUDIO_VIDEO,
                )
            }
        },
        onHangUp = hostAdapter::closeRealtimeSession,
        diagnostics = hostAdapter.realtimeDiagnostics,
    )
}

@Composable
private fun ExperimentalVideoPreviewCard(initialState: ComposeExperimentalVideoState) {
    ExperimentalVideoCardContent(initialState, {}, {}, {}, {}, {}, {}, {}, emptyList(), previewOnly = true)
}

@Composable
private fun ExperimentalVideoCardContent(
    state: ComposeExperimentalVideoState,
    onRefresh: () -> Unit,
    onCameraSelected: (String?) -> Unit,
    onTestCamera: () -> Unit,
    onStartPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onStartVideo: () -> Unit,
    onHangUp: () -> Unit,
    diagnostics: List<String>,
    previewOnly: Boolean = false,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.title, style = MaterialTheme.typography.h6)
            Text("Runtime: ${state.runtimeLabel} · Target: ${state.selectedPeerName}", style = MaterialTheme.typography.body2)
            DeviceChoiceDropdown(
                label = "Camera",
                choices = state.cameras,
                selected = state.selectedCamera,
                enabled = !previewOnly,
                onSelected = onCameraSelected,
            )
            Text(state.previewConfigurationLabel, style = MaterialTheme.typography.caption)
            Text("${state.stageTitle} · ${state.stageBadge} · ${state.mediaLabel}", style = MaterialTheme.typography.body2)
            Text("${state.previewStatus} · ${state.frameCaption}", style = MaterialTheme.typography.caption)
            Text(state.cameraTestStatus, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh, enabled = !previewOnly && state.canRefreshCameras) { Text("Refresh cameras") }
                Button(onClick = onTestCamera, enabled = !previewOnly && state.canTestCamera) { Text("Test camera") }
                Button(onClick = onStartPreview, enabled = !previewOnly && state.canStartPreview) { Text("Start preview") }
                Button(onClick = onStopPreview, enabled = !previewOnly && state.canStopPreview) { Text("Stop preview") }
                Button(onClick = onStartVideo, enabled = !previewOnly && state.canStartVideo) { Text(state.startVideoLabel) }
                Button(onClick = onHangUp, enabled = !previewOnly && state.canHangUp) { Text("Hang up") }
            }
            diagnostics.takeLast(4).forEach { Text("• $it", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)) }
            Text(state.readinessSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
        }
    }
}

@Composable
private fun LiveRuntimeDiagnosticsCard(hostAdapter: ComposeDesktopHostAdapter) {
    RuntimeDiagnosticsCardContent(
        diagnosticsState = hostAdapter.diagnosticsState,
        regressionState = hostAdapter.regressionReadinessState,
        packagingState = hostAdapter.packagingReadinessState,
    )
}

@Composable
private fun RuntimeDiagnosticsPreviewCard() {
    RuntimeDiagnosticsCardContent(
        diagnosticsState = ComposeShellMetadata.DEFAULT_DIAGNOSTICS_STATE,
        regressionState = ComposeShellMetadata.DEFAULT_REGRESSION_STATE,
        packagingState = ComposeShellMetadata.DEFAULT_PACKAGING_STATE,
    )
}

@Composable
private fun RuntimeDiagnosticsCardContent(
    diagnosticsState: ComposeDiagnosticsState,
    regressionState: ComposeRegressionReadinessState,
    packagingState: ComposePackagingReadinessState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Runtime / Diagnostics", style = MaterialTheme.typography.h6)
            Text(diagnosticsState.diagnosticChannelSummary, style = MaterialTheme.typography.body2)
            Text(
                text = diagnosticsState.warningSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            Text(regressionState.summary, style = MaterialTheme.typography.body2)
            Text(
                text = regressionState.nextActionSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Text(packagingState.summary, style = MaterialTheme.typography.body2)
            Text(
                text = packagingState.promotionSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
        }
    }
}

@Composable
private fun DeviceChoiceDropdown(
    label: String,
    choices: List<MediaDeviceChoice>,
    selected: MediaDeviceChoice,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember(label, selected.deviceId) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
        Box {
            Button(onClick = { expanded = true }, enabled = enabled && choices.isNotEmpty()) {
                Text(selected.toString())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        onClick = {
                            onSelected(choice.deviceId)
                            expanded = false
                        },
                    ) {
                        Text(choice.toString())
                    }
                }
            }
        }
    }
}

private fun copyToSystemClipboard(text: String) {
    if (text.isBlank()) return
    runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}

@Composable
private fun PeerListPreviewCard(initialState: ComposePeerListState) {
    var selectedPeerIndex by remember { mutableStateOf(initialState.selectedPeerIndex) }
    val previewState = initialState.copy(selectedPeerIndex = selectedPeerIndex)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(previewState.title, style = MaterialTheme.typography.h6)
            Text(
                text = previewState.hint,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(0.44f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Peers: ${previewState.visiblePeers.size}", style = MaterialTheme.typography.subtitle1)
                    previewState.visiblePeers.forEachIndexed { index, peer ->
                        PeerPreviewRow(
                            peer = peer,
                            selected = index == selectedPeerIndex,
                            onSelect = { selectedPeerIndex = index },
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(0.56f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(previewState.selectedPeerTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = previewState.selectedPeerMeta,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
                    )
                    Text(
                        text = previewState.actionSummary,
                        style = MaterialTheme.typography.body2,
                    )
                    PeerTargetCommandButton(previewState.targetControlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET)) {}
                    PeerTargetCommandButton(previewState.targetControlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET)) {}
                    PeerTargetCommandButton(previewState.targetControlPlan.command(ComposePeerTargetCommandKind.VOICE_TARGET)) {}
                    PeerTargetCommandButton(previewState.targetControlPlan.command(ComposePeerTargetCommandKind.VIDEO_TARGET)) {}
                    Text(
                        text = previewState.peerStatus,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeerTargetCommandButton(
    command: ComposePeerTargetCommand,
    onCommand: (ComposePeerTargetCommand) -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCommand(command) },
        enabled = command.enabled,
    ) {
        Text(command.displayLabel)
    }
}

@Composable
private fun PeerPreviewRow(
    peer: ComposePeerListItem,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val statusColor =
        if (peer.online) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.42f)
    val surfaceColor = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.10f) else MaterialTheme.colors.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        color = surfaceColor,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("●", color = statusColor)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(peer.nickname, style = MaterialTheme.typography.body1)
                Text(
                    text = "${peer.availabilityLabel} · ${peer.listMeta}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                )
            }
        }
    }
}

