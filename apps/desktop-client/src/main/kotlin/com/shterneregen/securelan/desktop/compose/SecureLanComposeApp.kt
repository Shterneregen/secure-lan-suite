package com.shterneregen.securelan.desktop.compose

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

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
            PreviewComposeShellContent(darkTheme, onThemeToggle)
        }
    }
}

@Composable
private fun PreviewComposeShellContent(
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    var requestedAppMode by remember { mutableStateOf(AppMode.WELCOME) }
    val previewProductState = ComposeProductScreenState.from(
        statusState = ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE,
        requestedAppMode = requestedAppMode,
        connectionHubMode = if (requestedAppMode == AppMode.JOIN_SETUP) ComposeConnectionHubMode.JOIN else ComposeConnectionHubMode.HOST,
    )
    SecureLanAppShell(
        shellState = ComposeAppShellState(
            productState = previewProductState,
            statusState = ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE,
        ),
        darkTheme = darkTheme,
        onThemeToggle = onThemeToggle,
    ) {
        if (previewProductState.appMode == AppMode.WELCOME) {
            ComposeOnboardingScreen(
                state = ComposeShellMetadata.DEFAULT_ONBOARDING_STATE,
                darkTheme = darkTheme,
                onThemeToggle = onThemeToggle,
                onHostRoom = { requestedAppMode = AppMode.HOST_SETUP },
                onJoinRoom = { requestedAppMode = AppMode.JOIN_SETUP },
            )
        } else {
            val previewPeerState = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE
            val previewChatState = ComposeShellMetadata.DEFAULT_CHAT_WORKSPACE_STATE
            ConnectionHubPreview(ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE)
            MainWorkspaceRow(
                parityState = ComposeShellMetadata.DEFAULT_WORKSPACE_PARITY_STATE,
                peersTooltip = previewPeerState.hint,
                chatTooltip = previewChatState.subtitle,
                rightColumnTitle = ComposeShellMetadata.DEFAULT_CONTEXT_PANEL_STATE.title,
                chatActions = {
                    CompactButton(onClick = {}, enabled = false) { Text("Voice call") }
                    CompactButton(onClick = {}, enabled = false) { Text("Video call") }
                    CompactButton(onClick = {}, enabled = false) { Text("End call") }
                },
                peersColumn = { PeerListPreviewCard(previewPeerState) },
                chatColumn = { ChatWorkspacePreviewCard(previewChatState) },
                actionsColumn = { responsiveState -> PreviewActionsColumn(responsiveState) },
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
    var requestedConnectionMode by remember { mutableStateOf(ComposeConnectionHubMode.HOST) }
    var requestedAppMode by remember { mutableStateOf(AppMode.WELCOME) }
    val inMessengerMode = hostAdapter.statusState.localServerRunning || hostAdapter.statusState.clientConnected
    val productState = ComposeProductScreenState.from(
        statusState = hostAdapter.statusState,
        requestedAppMode = requestedAppMode,
        connectionHubMode = requestedConnectionMode,
    )
    if (productState.appMode == AppMode.WELCOME) {
        SecureLanAppShell(
            shellState = ComposeAppShellState(productState = productState, statusState = hostAdapter.statusState),
            darkTheme = darkTheme,
            onThemeToggle = onThemeToggle,
        ) {
            ComposeOnboardingScreen(
                state = ComposeShellMetadata.DEFAULT_ONBOARDING_STATE,
                darkTheme = darkTheme,
                onThemeToggle = onThemeToggle,
                onHostRoom = {
                    requestedConnectionMode = ComposeConnectionHubMode.HOST
                    requestedAppMode = AppMode.HOST_SETUP
                },
                onJoinRoom = {
                    requestedConnectionMode = ComposeConnectionHubMode.JOIN
                    requestedAppMode = AppMode.JOIN_SETUP
                },
            )
        }
        return
    }
    if (!inMessengerMode && productState.connectionFlowActive) {
        SecureLanAppShell(
            shellState = ComposeAppShellState(productState = productState, statusState = hostAdapter.statusState),
            darkTheme = darkTheme,
            onThemeToggle = onThemeToggle,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.widthIn(min = 720.dp, max = 920.dp)) {
                    ConnectionHub(hostAdapter, initialMode = requestedConnectionMode)
                }
            }
        }
        return
    }

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
    val chatTooltip = peerState.selectedPeer?.let { "Actions on the right will target “${it.nickname}”. Text chat remains shared for now." }
        ?: "Connect to chat, then select a peer on the left for voice, video, and file actions."

    val messengerProductState = ComposeProductScreenState.from(
        statusState = hostAdapter.statusState,
        requestedAppMode = requestedAppMode,
        connectionHubMode = requestedConnectionMode,
        selectedPeer = peerState.selectedPeer,
    )
    SecureLanAppShell(
        shellState = ComposeAppShellState(
            productState = messengerProductState,
            statusState = hostAdapter.statusState,
            peerStatus = peerState.peerStatus,
        ),
        darkTheme = darkTheme,
        onThemeToggle = onThemeToggle,
    ) {
        MainWorkspaceRow(
            parityState = ComposeShellMetadata.DEFAULT_WORKSPACE_PARITY_STATE,
            peersTooltip = peerState.hint,
            chatTooltip = chatTooltip,
            rightColumnTitle = ComposeShellMetadata.DEFAULT_CONTEXT_PANEL_STATE.title,
            chatActions = {
                ChatCallActions(
                    hostAdapter = hostAdapter,
                    selectedPeer = peerState.selectedPeer,
                )
            },
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
            actionsColumn = { responsiveState -> LiveActionsColumn(hostAdapter, peerState, responsiveState) },
        )
    }
}

@Composable
private fun ComposeOnboardingScreen(
    state: ComposeOnboardingState,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onHostRoom: () -> Unit,
    onJoinRoom: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1.15f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, sectionBorderColor()),
                color = fieldBackgroundColor(),
            ) {
                Text(
                    text = state.brandGlyph,
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 18.dp),
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.primary,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(state.headline, style = MaterialTheme.typography.h4, color = MaterialTheme.colors.onSurface)
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.body,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.widthIn(max = 620.dp),
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                state.benefitChips.forEach { chip -> StatusChip(chip) }
            }
        }
        Card(
            modifier = Modifier.weight(0.85f).widthIn(min = 360.dp, max = 520.dp),
            shape = PanelShape,
            border = BorderStroke(1.dp, panelBorderColor()),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.surface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Start nearby", style = MaterialTheme.typography.h6)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onHostRoom,
                        modifier = Modifier.widthIn(min = 180.dp, max = 240.dp).heightIn(min = 40.dp),
                        shape = ButtonShape,
                    ) { Text(state.hostActionLabel) }
                    OutlinedButton(
                        onClick = onJoinRoom,
                        modifier = Modifier.widthIn(min = 180.dp, max = 240.dp).heightIn(min = 40.dp),
                        shape = ButtonShape,
                    ) { Text(state.joinActionLabel) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    state.secondaryLinks.forEach { link ->
                        Text(
                            text = link,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                        )
                    }
                }
                Divider(color = sectionBorderColor())
                Text(state.discoveryStatus, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.66f))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SectionShape,
                    border = BorderStroke(1.dp, sectionBorderColor()),
                    color = fieldBackgroundColor(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(state.emptyNearbyTitle, style = MaterialTheme.typography.subtitle2)
                        Text(
                            text = state.emptyNearbyDetail,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecureLanAppShell(
    shellState: ComposeAppShellState,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp, max = 56.dp),
            shape = PanelShape,
            border = BorderStroke(1.dp, panelBorderColor()),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.surface,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = shellState.currentContextLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.subtitle2,
                    )
                    Text(
                        text = shellState.primaryStatusDetail,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
                    )
                }
                StatusChip(shellState.globalStatusLabel)
                CompactButton(onClick = {}, enabled = false) { Text("Search") }
                CompactButton(onClick = {}, enabled = false) { Text("Settings") }
                if (shellState.rightActions.contains("Diagnostics")) {
                    CompactButton(onClick = {}, enabled = false) { Text("Diagnostics") }
                }
                ThemeToggleButton(darkTheme = darkTheme, onThemeToggle = onThemeToggle)
            }
        }
        content()
    }
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
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = PanelShape,
        border = BorderStroke(1.dp, panelBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val globalState = ComposeGlobalStatusIndicatorState(
                statusState = state,
                peerStatus = peerStatus,
                voiceStatus = voiceStatus,
                transferStatus = transferStatus,
            )
            StatusChip(globalState.label)
            Text(
                text = globalState.detailText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
            )
            ThemeToggleButton(darkTheme = darkTheme, onThemeToggle = onThemeToggle)
        }
    }
}

@Composable
private fun ThemeToggleButton(
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    Button(
        onClick = onThemeToggle,
        modifier = Modifier.heightIn(min = 26.dp),
        shape = ButtonShape,
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Text(if (darkTheme) "Dark theme" else "Light theme", style = MaterialTheme.typography.button)
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIndicator(text)
            Text(text, style = MaterialTheme.typography.caption)
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
    Canvas(modifier = Modifier.size(7.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2f, center = Offset(size.width / 2f, size.height / 2f))
    }
}

@Composable
private fun TitleWithHelp(
    title: String,
    tooltip: String,
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.subtitle2,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = titleStyle)
        HelpTooltip(tooltip)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HelpTooltip(text: String) {
    TooltipArea(
        tooltip = {
            Surface(
                shape = FieldShape,
                border = BorderStroke(1.dp, sectionBorderColor()),
                color = MaterialTheme.colors.surface,
            ) {
                Text(
                    text = text,
                    modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
                )
            }
        },
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, sectionBorderColor()),
            color = fieldBackgroundColor(),
        ) {
            Text(
                text = "?",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun ConnectionHub(
    hostAdapter: ComposeDesktopHostAdapter,
    initialMode: ComposeConnectionHubMode = ComposeConnectionHubMode.HOST,
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var nickname by remember { mutableStateOf(hostAdapter.statusState.nickname) }
    var password by remember { mutableStateOf(hostAdapter.currentRoomPassword) }
    var serverChatPort by remember { mutableStateOf(hostAdapter.statusState.serverChatPortText) }
    var serverFilePort by remember { mutableStateOf(hostAdapter.statusState.serverFilePortText) }
    var discoverable by remember { mutableStateOf(hostAdapter.statusState.discoverable) }
    var manualHost by remember { mutableStateOf(hostAdapter.statusState.manualHost) }
    var clientChatPort by remember { mutableStateOf(hostAdapter.statusState.clientChatPortText) }
    var clientFilePort by remember { mutableStateOf(hostAdapter.statusState.clientFilePortText) }

    LaunchedEffect(hostAdapter.statusState.localServerRunning, hostAdapter.statusState.clientConnected) {
        nickname = hostAdapter.statusState.nickname
        password = hostAdapter.currentRoomPassword
        serverChatPort = hostAdapter.statusState.serverChatPortText
        serverFilePort = hostAdapter.statusState.serverFilePortText
        discoverable = hostAdapter.statusState.discoverable
        manualHost = hostAdapter.statusState.manualHost
        clientChatPort = hostAdapter.statusState.clientChatPortText
        clientFilePort = hostAdapter.statusState.clientFilePortText
    }

    val statusState = hostAdapter.statusState.copy(
        nickname = nickname,
        roomPasswordPlaceholder = password,
        serverChatPortText = serverChatPort,
        serverFilePortText = serverFilePort,
        discoverable = discoverable,
        manualHost = manualHost,
        clientChatPortText = clientChatPort,
        clientFilePortText = clientFilePort,
    )
    val hubState = ComposeConnectionHubState(
        statusState = statusState,
        mode = mode,
        localNetworkInfo = hostAdapter.localNetworkInfo,
    )

    ConnectionHubContent(
        state = hubState,
        nickname = nickname,
        onNicknameChange = { nickname = it },
        password = password,
        onPasswordChange = { password = it },
        mode = mode,
        onModeChange = { mode = it },
        serverChatPort = serverChatPort,
        onServerChatPortChange = { serverChatPort = it },
        serverFilePort = serverFilePort,
        onServerFilePortChange = { serverFilePort = it },
        discoverable = discoverable,
        onDiscoverableChange = { discoverable = it },
        manualHost = manualHost,
        onManualHostChange = { manualHost = it },
        clientChatPort = clientChatPort,
        onClientChatPortChange = { clientChatPort = it },
        clientFilePort = clientFilePort,
        onClientFilePortChange = { clientFilePort = it },
        onOpenRoom = {
            statusState.serverChatPort?.let { chat ->
                statusState.serverFilePort?.let { file ->
                    hostAdapter.openRoom(nickname, password, chat, file, discoverable)
                }
            }
        },
        onStopHosting = { hostAdapter.stopHosting() },
        onConnect = {
            statusState.clientChatPort?.let { chat ->
                statusState.clientFilePort?.let { file ->
                    hostAdapter.connect(manualHost, nickname, password, chat, file)
                }
            }
        },
        onDisconnect = { hostAdapter.disconnect() },
        onSetDiscoverable = { hostAdapter.setDiscoverable(it) },
    )
}

@Composable
private fun ConnectionHubPreview(state: ComposeConnectionHubState) {
    var mode by remember { mutableStateOf(state.mode) }
    var nickname by remember { mutableStateOf(state.nickname) }
    var password by remember { mutableStateOf(state.password) }
    var serverChatPort by remember { mutableStateOf(state.statusState.serverChatPortText) }
    var serverFilePort by remember { mutableStateOf(state.statusState.serverFilePortText) }
    var discoverable by remember { mutableStateOf(state.statusState.discoverable) }
    var manualHost by remember { mutableStateOf(state.statusState.manualHost) }
    var clientChatPort by remember { mutableStateOf(state.statusState.clientChatPortText) }
    var clientFilePort by remember { mutableStateOf(state.statusState.clientFilePortText) }

    val statusState = state.statusState.copy(
        nickname = nickname,
        roomPasswordPlaceholder = password,
        serverChatPortText = serverChatPort,
        serverFilePortText = serverFilePort,
        discoverable = discoverable,
        manualHost = manualHost,
        clientChatPortText = clientChatPort,
        clientFilePortText = clientFilePort,
    )
    val hubState = state.copy(statusState = statusState, mode = mode)

    ConnectionHubContent(
        state = hubState,
        nickname = nickname,
        onNicknameChange = { nickname = it },
        password = password,
        onPasswordChange = { password = it },
        mode = mode,
        onModeChange = { mode = it },
        serverChatPort = serverChatPort,
        onServerChatPortChange = { serverChatPort = it },
        serverFilePort = serverFilePort,
        onServerFilePortChange = { serverFilePort = it },
        discoverable = discoverable,
        onDiscoverableChange = { discoverable = it },
        manualHost = manualHost,
        onManualHostChange = { manualHost = it },
        clientChatPort = clientChatPort,
        onClientChatPortChange = { clientChatPort = it },
        clientFilePort = clientFilePort,
        onClientFilePortChange = { clientFilePort = it },
        onOpenRoom = {},
        onStopHosting = {},
        onConnect = {},
        onDisconnect = {},
        onSetDiscoverable = {},
    )
}

@Composable
private fun MainWorkspaceRow(
    parityState: ComposeJavaFxWorkspaceParityState,
    peersTooltip: String? = null,
    chatTooltip: String? = null,
    rightColumnTitle: String = parityState.workspaceColumns[2].title,
    chatActions: @Composable RowScope.() -> Unit = {},
    peersColumn: @Composable () -> Unit,
    chatColumn: @Composable () -> Unit,
    actionsColumn: @Composable (ComposeContextPanelResponsiveState) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        val responsiveState = ComposeContextPanelResponsiveState.forWidth(maxWidth.value.toInt())
        var contextDrawerOpen by remember(responsiveState.mode) { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MainWorkspaceColumn(
                title = parityState.workspaceColumns[0].title,
                tooltip = peersTooltip,
                modifier = Modifier.weight(parityState.workspaceColumns[0].weight).fillMaxHeight(),
                content = peersColumn,
            )
            MainWorkspaceColumn(
                title = parityState.workspaceColumns[1].title,
                tooltip = chatTooltip,
                headerActions = {
                    chatActions()
                    if (responsiveState.drawerEntryVisible) {
                        CompactButton(
                            onClick = { contextDrawerOpen = true },
                            modifier = Modifier.semantics { contentDescription = responsiveState.drawerOpenContentDescription },
                        ) { Text("Context") }
                    }
                },
                modifier = Modifier.weight(parityState.workspaceColumns[1].weight).fillMaxHeight(),
                content = chatColumn,
            )
            if (responsiveState.inlinePanelVisible) {
                MainWorkspaceColumn(
                    title = rightColumnTitle,
                    modifier = Modifier.weight(parityState.workspaceColumns[2].weight).fillMaxHeight(),
                    content = { actionsColumn(responsiveState) },
                )
            }
        }
        if (responsiveState.drawerMode && contextDrawerOpen) {
            ContextAssistantDrawer(
                responsiveState = responsiveState,
                onClose = { contextDrawerOpen = false },
                content = { actionsColumn(responsiveState) },
            )
        }
    }
}

@Composable
private fun ContextAssistantDrawer(
    responsiveState: ComposeContextPanelResponsiveState,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    onClose()
                    true
                } else {
                    false
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(onClick = onClose),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(min = 320.dp, max = 380.dp)
                .semantics { contentDescription = responsiveState.drawerContentDescription },
            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
            border = BorderStroke(1.dp, panelBorderColor()),
            color = MaterialTheme.colors.surface,
            elevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Context Assistant", style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
                    CompactButton(
                        onClick = onClose,
                        modifier = Modifier.semantics { contentDescription = responsiveState.drawerCloseContentDescription },
                    ) { Text("Close") }
                }
                Text(
                    text = responsiveState.summary,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                )
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopStart) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun MainWorkspaceColumn(
    title: String,
    modifier: Modifier,
    tooltip: String? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
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
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (tooltip == null) {
                    Text(title, style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f))
                } else {
                    TitleWithHelp(
                        title = title,
                        tooltip = tooltip,
                        titleStyle = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.weight(1f),
                    )
                }
                headerActions()
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopStart) { content() }
        }
    }
}

@Composable
private fun LiveActionsColumn(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    responsiveState: ComposeContextPanelResponsiveState,
) {
    val quickActions = ComposeSelectedPeerQuickActionsState(
        peerListState = peerState,
        clientConnected = hostAdapter.statusState.clientConnected,
        hangUpReady = hostAdapter.mediaVoiceState.canHangUp || hostAdapter.experimentalVideoState.canHangUp,
    )
    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        senderId = hostAdapter.statusState.nickname,
        sessionPassword = hostAdapter.currentRoomPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles,
    )
    val voiceState = hostAdapter.mediaVoiceState.copy(peerListState = peerState)
    val videoState = hostAdapter.experimentalVideoState.copy(peerListState = peerState)
    val contextPanelState = when {
        voiceState.currentSession != null || videoState.currentSession != null || videoState.previewRunning -> ComposeContextPanelState.forCall(quickActions, voiceState, videoState)
        transferState.activeCount > 0 || transferState.waitingPromptCount > 0 -> ComposeContextPanelState.forTransfer(transferState, quickActions)
        peerState.selectedPeer != null -> ComposeContextPanelState.forPeer(quickActions, transferState)
        else -> ComposeContextPanelState.forRoom(peerState, transferState)
    }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContextPanelSummary(state = contextPanelState, responsiveState = responsiveState)
        contextPanelState.visibleCardsFor(responsiveState).forEach { card ->
            val expandedContent: (@Composable () -> Unit)? = when (card.kind) {
                ComposeContextPanelCardKind.TRANSFER_DETAILS -> {
                    { LiveFileTransferCard(hostAdapter, peerState) }
                }
                ComposeContextPanelCardKind.CALL_CONTROLS -> {
                    { CallControlsPanel(hostAdapter, quickActions, voiceState, videoState) }
                }
                ComposeContextPanelCardKind.QUICK_ACTIONS -> {
                    { PeerQuickActionsPanel(hostAdapter, quickActions) }
                }
                ComposeContextPanelCardKind.QUICK_SHARE -> {
                    { LiveQuickShareCard(hostAdapter) }
                }
                ComposeContextPanelCardKind.DIAGNOSTICS -> {
                    {
                        RuntimeDiagnosticsCardContent(
                            diagnosticsState = hostAdapter.diagnosticsState,
                            regressionState = hostAdapter.regressionReadinessState,
                            packagingState = hostAdapter.packagingReadinessState,
                        )
                    }
                }
                else -> null
            }
            ContextPanelCard(card, expandedContent)
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
private fun ContextPanelSummary(
    state: ComposeContextPanelState,
    responsiveState: ComposeContextPanelResponsiveState? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(state.title, style = MaterialTheme.typography.h6)
        Text(
            text = state.nextActionSummary,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
        )
        if (state.hiddenFeatureNames.isNotEmpty()) {
            Text(
                text = state.hiddenFeatureSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.48f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (responsiveState != null && responsiveState.mode != ComposeContextPanelResponsiveMode.FULL_PANEL) {
            Text(
                text = responsiveState.summary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.48f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ContextPanelCard(
    card: ComposeContextPanelCard,
    expandedContent: @Composable (() -> Unit)? = null,
) {
    var expanded by remember(card.kind, card.title, card.collapsed) { mutableStateOf(!card.collapsed) }
    val borderAlpha = if (card.primary) 0.30f else 0.14f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, (if (card.primary) MaterialTheme.colors.primary else sectionBorderColor()).copy(alpha = borderAlpha)),
        color = if (card.primary) MaterialTheme.colors.primary.copy(alpha = 0.08f) else fieldBackgroundColor().copy(alpha = 0.56f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(card.title, style = if (card.primary) MaterialTheme.typography.subtitle1 else MaterialTheme.typography.subtitle2)
                    if (!card.badge.isNullOrBlank()) {
                        Text(card.badge, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f))
                    }
                }
                if (expandedContent != null) {
                    Text(
                        text = if (expanded) "Hide" else "Show",
                        modifier = Modifier.clickable { expanded = !expanded }.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                    )
                }
            }
            Text(card.body, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f))
            if (!card.primaryAction.isNullOrBlank()) {
                Surface(shape = FieldShape, border = BorderStroke(1.dp, sectionBorderColor()), color = MaterialTheme.colors.surface.copy(alpha = 0.86f)) {
                    Text(
                        text = card.primaryAction,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                    )
                }
            }
            if (expanded && expandedContent != null) {
                expandedContent()
            }
        }
    }
}

@Composable
private fun PeerQuickActionsPanel(hostAdapter: ComposeDesktopHostAdapter, state: ComposeSelectedPeerQuickActionsState) {
    SelectedPeerQuickActions(
        attachEnabled = state.attachEnabled,
        voiceEnabled = state.voiceEnabled,
        videoEnabled = state.videoEnabled,
        hangUpEnabled = state.hangUpEnabled,
        onAttach = {},
        onVoice = {
            state.selectedPeer?.let { peer -> hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO) }
        },
        onVideo = {
            state.selectedPeer?.let { peer -> hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO_VIDEO) }
        },
        onHangUp = hostAdapter::closeRealtimeSession,
    )
}

@Composable
private fun CallControlsPanel(
    hostAdapter: ComposeDesktopHostAdapter,
    quickActions: ComposeSelectedPeerQuickActionsState,
    voiceState: ComposeMediaVoiceState,
    videoState: ComposeExperimentalVideoState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TransferInfoChip(voiceState.voiceStatusText)
            TransferInfoChip(videoState.previewStateLabel)
        }
        PeerQuickActionsPanel(hostAdapter, quickActions)
    }
}

@Composable
private fun HeaderCard(
    title: String,
    tooltip: String? = null,
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
            if (tooltip == null) {
                Text(title, style = MaterialTheme.typography.subtitle2)
            } else {
                TitleWithHelp(title = title, tooltip = tooltip)
            }
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
    placeholder: String? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface)
        }
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
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                            )
                        }
                        innerTextField()
                    }
                },
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
private fun SubtleContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = 0.55f)),
        color = fieldBackgroundColor().copy(alpha = 0.54f),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), content = content)
    }
}

@Composable
private fun PeerListContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Column(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun PeerListEmptyState(peerState: ComposePeerListState) {
    Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = peerState.emptyStateTitle,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
            )
            Text(
                text = peerState.emptyStateDetail,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Surface(
                shape = FieldShape,
                border = BorderStroke(1.dp, sectionBorderColor()),
                color = fieldBackgroundColor(),
            ) {
                Text(
                    text = peerState.emptyStateActionLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                )
            }
        }
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
private fun ChatCallActions(
    hostAdapter: ComposeDesktopHostAdapter,
    selectedPeer: ComposePeerListItem?,
) {
    val voiceEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.voiceCapable
    val videoEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.videoCapable
    val hangUpEnabled = selectedPeer?.realtimeCapable == true &&
        (hostAdapter.mediaVoiceState.canHangUp || hostAdapter.experimentalVideoState.canHangUp)

    CompactButton(
        onClick = {
            selectedPeer?.let { peer -> hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO) }
        },
        modifier = Modifier.defaultMinSize(minWidth = 0.dp),
        enabled = voiceEnabled,
    ) { Text("Voice") }
    CompactButton(
        onClick = {
            selectedPeer?.let { peer -> hostAdapter.startRealtimeSession(hostAdapter.statusState.nickname, peer.nickname, RtcSessionMode.AUDIO_VIDEO) }
        },
        modifier = Modifier.defaultMinSize(minWidth = 0.dp),
        enabled = videoEnabled,
    ) { Text("Video") }
    CompactButton(
        onClick = hostAdapter::closeRealtimeSession,
        modifier = Modifier.defaultMinSize(minWidth = 0.dp),
        enabled = hangUpEnabled,
    ) { Text("End") }
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
private fun PreviewActionsColumn(responsiveState: ComposeContextPanelResponsiveState) {
    val contextPanelState = ComposeShellMetadata.DEFAULT_CONTEXT_PANEL_STATE
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContextPanelSummary(contextPanelState, responsiveState)
        contextPanelState.visibleCardsFor(responsiveState).forEach { card ->
            ContextPanelCard(card)
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
private fun PeerActionReadinessCard(peerState: ComposePeerListState, hostAdapter: ComposeDesktopHostAdapter) {
    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        senderId = hostAdapter.statusState.nickname,
        sessionPassword = hostAdapter.currentRoomPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles,
    )
    PeerActionReadinessPreviewCard(transferState)
}

@Composable
private fun PeerActionReadinessPreviewCard(transferState: ComposeFileTransferState) {
    SubtleContentSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(transferState.peerListState.noPeerActionTitle, style = MaterialTheme.typography.subtitle2)
            Text(
                text = transferState.peerListState.noPeerActionDetail,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TransferInfoChip(transferState.transferCountSummary)
                TransferInfoChip(transferState.receiveModeShortLabel)
            }
        }
    }
}

@Composable
private fun SelectedPeerSummary(state: ComposeSelectedPeerQuickActionsState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.title, style = MaterialTheme.typography.h6)
        Text(
            text = state.meta,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun QuickSharePreviewCard(state: ComposeQuickShareState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.title, style = MaterialTheme.typography.h6)
        Text(
            text = state.subtitle,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
        )
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
            text = if (state.shareRowsDetailed.isEmpty()) state.emptySharesTitle else state.shareRowsDetailed.joinToString(" · ") { row -> "${row.typeLabel}: ${row.statusLabel}" },
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
    val listState = rememberLazyListState()

    LaunchedEffect(previewState.transcriptLines.size) {
        if (previewState.transcriptLines.isNotEmpty()) {
            listState.animateScrollToItem(previewState.transcriptLines.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SubtleContentSurface(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (previewState.transcriptLines.isEmpty()) {
                ChatTranscriptEmptyState(previewState, connected = false)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    items(previewState.messages.size) { index ->
                        ChatTranscriptLine(previewState.messages[index])
                    }
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
                label = "",
                modifier = Modifier.weight(1f),
                placeholder = "Type a message for the shared chat...",
            )
            CompactButton(onClick = {}, enabled = false) { Text(previewState.sendLabel) }
        }
        Text(
            text = "${previewState.transcriptSummary} · ${previewState.readinessSummary}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
    }
}

@Composable
private fun ConnectionHubContent(
    state: ComposeConnectionHubState,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    mode: ComposeConnectionHubMode,
    onModeChange: (ComposeConnectionHubMode) -> Unit,
    serverChatPort: String,
    onServerChatPortChange: (String) -> Unit,
    serverFilePort: String,
    onServerFilePortChange: (String) -> Unit,
    discoverable: Boolean,
    onDiscoverableChange: (Boolean) -> Unit,
    manualHost: String,
    onManualHostChange: (String) -> Unit,
    clientChatPort: String,
    onClientChatPortChange: (String) -> Unit,
    clientFilePort: String,
    onClientFilePortChange: (String) -> Unit,
    onOpenRoom: () -> Unit,
    onStopHosting: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSetDiscoverable: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 224.dp),
        shape = PanelShape,
        border = BorderStroke(1.dp, panelBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TitleWithHelp(
                    title = state.title,
                    tooltip = "Set your name and the shared room password, then open a room on this computer or join one on another computer.",
                    titleStyle = MaterialTheme.typography.subtitle2,
                )
                ConnectionStatusBadge(label = state.activeBadgeLabel ?: state.modeHint)
                Box(modifier = Modifier.weight(1f))
                if (state.copyRoomAddressEnabled) {
                    CompactButton(onClick = { copyToSystemClipboard(state.copyRoomAddressText) }) { Text("Copy room address") }
                }
            }

            ConnectionModeChooser(
                mode = mode,
                hostLabel = state.hostTabLabel,
                hostSubtitle = state.hostChoiceSubtitle,
                joinLabel = state.joinTabLabel,
                joinSubtitle = state.joinChoiceSubtitle,
                onModeChange = onModeChange,
            )

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactTextField(nickname, onNicknameChange, label = "Your name", modifier = Modifier.weight(1f))
                CompactTextField(
                    password,
                    onPasswordChange,
                    label = "Room password",
                    modifier = Modifier.weight(1f),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }

            ConnectionModeDetailsSurface(
                title = state.activeModeTitle,
                detail = state.activeModeDetail,
                summary = state.joinTargetSummary,
            ) {
                if (mode == ComposeConnectionHubMode.HOST) {
                    Text(
                        text = "Your display name and room password are set above. Discovery controls whether nearby trusted peers can find this room.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = discoverable,
                            onCheckedChange = {
                                onDiscoverableChange(it)
                                if (state.statusState.localServerRunning) {
                                    onSetDiscoverable(it)
                                }
                            },
                            enabled = state.discoverableToggleEnabled,
                        )
                        Text(state.discoverableLabel, style = MaterialTheme.typography.body2)
                    }
                } else {
                    Text(
                        text = "Nearby rooms will appear in the peer list. Use Advanced manual connection when a room is hidden or discovery is blocked.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                }
            }

            ComposeAdvancedPane(state.advancedSettingsTitle) {
                if (mode == ComposeConnectionHubMode.HOST) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CompactTextField(serverChatPort, onServerChatPortChange, label = "Chat port", modifier = Modifier.weight(1f))
                        CompactTextField(serverFilePort, onServerFilePortChange, label = "File port", modifier = Modifier.weight(1f))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CompactTextField(manualHost, onManualHostChange, label = "Host address", modifier = Modifier.weight(1.3f))
                        CompactTextField(clientChatPort, onClientChatPortChange, label = "Chat port", modifier = Modifier.weight(1f))
                        CompactTextField(clientFilePort, onClientFilePortChange, label = "File port", modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = when (mode) {
                        ComposeConnectionHubMode.HOST -> onOpenRoom
                        ComposeConnectionHubMode.JOIN -> onConnect
                    },
                    enabled = state.primaryActionEnabled,
                    shape = ButtonShape,
                    modifier = Modifier.widthIn(min = 140.dp),
                ) {
                    Text(state.primaryActionLabel)
                }
                OutlinedButton(
                    onClick = when (mode) {
                        ComposeConnectionHubMode.HOST -> onStopHosting
                        ComposeConnectionHubMode.JOIN -> onDisconnect
                    },
                    enabled = state.secondaryActionEnabled,
                    shape = ButtonShape,
                    modifier = Modifier.widthIn(min = 120.dp),
                ) {
                    Text(state.secondaryActionLabel)
                }
                Box(modifier = Modifier.weight(1f))
                Text(
                    text = state.networkInfoSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.CenterStart) {
                if (state.statusMessage != null) {
                    ConnectionHubStatusMessage(
                        text = state.statusMessage,
                        tone = state.statusMessageTone,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionModeChooser(
    mode: ComposeConnectionHubMode,
    hostLabel: String,
    hostSubtitle: String,
    joinLabel: String,
    joinSubtitle: String,
    onModeChange: (ComposeConnectionHubMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ConnectionModeChoiceCard(
            title = hostLabel,
            subtitle = hostSubtitle,
            selected = mode == ComposeConnectionHubMode.HOST,
            onClick = { onModeChange(ComposeConnectionHubMode.HOST) },
            modifier = Modifier.weight(1f),
        )
        ConnectionModeChoiceCard(
            title = joinLabel,
            subtitle = joinSubtitle,
            selected = mode == ComposeConnectionHubMode.JOIN,
            onClick = { onModeChange(ComposeConnectionHubMode.JOIN) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConnectionModeChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.72f) else sectionBorderColor()
    val background = if (selected) MaterialTheme.colors.primary.copy(alpha = if (MaterialTheme.colors.isLight) 0.10f else 0.18f) else fieldBackgroundColor()
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = SectionShape,
        border = BorderStroke(1.dp, border),
        color = background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selected) "●" else "○",
                style = MaterialTheme.typography.subtitle2,
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.42f),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.button, color = MaterialTheme.colors.onSurface.copy(alpha = 0.88f))
                Text(
                    subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                )
            }
        }
    }
}

@Composable
private fun ConnectionModeDetailsSurface(
    title: String,
    detail: String,
    summary: String,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor().copy(alpha = 0.62f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.subtitle2)
                    Text(
                        detail,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                    )
                }
                ConnectionStatusBadge(summary)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
private fun ConnectionStatusBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun ConnectionHubStatusMessage(
    text: String,
    tone: ComposeConnectionHubMessageTone,
) {
    val accent = when (tone) {
        ComposeConnectionHubMessageTone.INFO -> MaterialTheme.colors.primary
        ComposeConnectionHubMessageTone.SUCCESS -> Color(0xFF7FB4FF)
        ComposeConnectionHubMessageTone.ERROR -> MaterialTheme.colors.error
    }
    val backgroundAlpha = when (tone) {
        ComposeConnectionHubMessageTone.INFO -> if (MaterialTheme.colors.isLight) 0.08f else 0.12f
        ComposeConnectionHubMessageTone.SUCCESS -> if (MaterialTheme.colors.isLight) 0.09f else 0.13f
        ComposeConnectionHubMessageTone.ERROR -> if (MaterialTheme.colors.isLight) 0.08f else 0.14f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        color = accent.copy(alpha = backgroundAlpha),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.caption,
            color = accent,
        )
    }
}

@Composable
private fun ConnectionModeSelector(
    mode: ComposeConnectionHubMode,
    hostLabel: String,
    joinLabel: String,
    onModeChange: (ComposeConnectionHubMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            ConnectionModeSegment(
                label = hostLabel,
                selected = mode == ComposeConnectionHubMode.HOST,
                onClick = { onModeChange(ComposeConnectionHubMode.HOST) },
                modifier = Modifier.weight(1f),
            )
            ConnectionModeSegment(
                label = joinLabel,
                selected = mode == ComposeConnectionHubMode.JOIN,
                onClick = { onModeChange(ComposeConnectionHubMode.JOIN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConnectionModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.92f) else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.button, color = contentColor)
    }
}

@Composable
private fun ComposeAdvancedPane(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = SectionShape,
            border = BorderStroke(1.dp, sectionBorderColor()),
            color = if (MaterialTheme.colors.isLight) {
                MaterialTheme.colors.background
            } else {
                androidx.compose.ui.graphics.Color(0xFF182334)
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "▾" else "▸",
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
                Text(title, style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f))
            }
        }
        if (expanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SectionShape,
                border = BorderStroke(1.dp, sectionBorderColor()),
                color = MaterialTheme.colors.background.copy(alpha = 0.42f),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = { content() },
                )
            }
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
    PeerListContentSurface(modifier = Modifier.fillMaxSize()) {
        if (!peerState.hasAnyPeers) {
            PeerListEmptyState(peerState)
        } else {
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                PeerListGroup(
                    peers = peerState.onlinePeers,
                    sectionTitle = "Online",
                    peerState = peerState,
                    onPeerSelected = onPeerSelected,
                )
                PeerListGroup(
                    peers = peerState.offlinePeers,
                    sectionTitle = "Offline",
                    peerState = peerState,
                    onPeerSelected = onPeerSelected,
                )
            }
        }
    }
}

@Composable
private fun PeerListGroup(
    peers: List<ComposePeerListItem>,
    sectionTitle: String,
    peerState: ComposePeerListState,
    onPeerSelected: (String?) -> Unit,
) {
    if (peers.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PeerListSectionHeader(title = sectionTitle)
        peers.forEach { peer ->
            val index = peerState.visiblePeers.indexOfFirst { it.nickname == peer.nickname }
            PeerPreviewRow(
                peer = peer,
                selected = index == peerState.resolvedSelectedPeerIndex,
                onSelect = { onPeerSelected(peer.nickname) },
            )
        }
    }
}

@Composable
private fun PeerListSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

internal fun resolveAttachCandidatePeer(
    selectedPeer: ComposePeerListItem?,
    resolvePeer: (String) -> com.shterneregen.securelan.chat.discovery.DiscoveredPeer?,
): com.shterneregen.securelan.chat.discovery.DiscoveredPeer? = selectedPeer
    ?.takeIf { it.online && it.fileCapable }
    ?.let { selected -> resolvePeer(selected.nickname) }

@Composable
private fun LiveChatWorkspaceCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    var draftMessage by remember { mutableStateOf("") }
    val transcript = hostAdapter.chatMessages
    val selectedPeer = peerState.selectedPeer
    val selectedFilePeer = resolveAttachCandidatePeer(selectedPeer, hostAdapter::discoveredPeerFor)
    val attachmentTools = ComposeAttachmentToolsState(
        peerSelected = selectedPeer != null,
        fileTargetReady = selectedFilePeer != null,
    )
    val chatState = ComposeChatWorkspaceState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        draftMessage = draftMessage,
    )
    val listState = rememberLazyListState()

    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) {
            listState.animateScrollToItem(transcript.lastIndex)
        }
    }

    fun sendDraftMessage() {
        if (draftMessage.isNotBlank() && hostAdapter.chatConnected) {
            hostAdapter.sendMessage(draftMessage.trim())
            draftMessage = ""
        }
    }
    fun attachSelectedFile() {
        val peer = selectedFilePeer ?: return
        val path = openComposeFileChooser("Choose file to send to ${peer.nickname}") ?: return
        hostAdapter.sendFileToPeer(path, hostAdapter.statusState.nickname, peer, hostAdapter.currentRoomPassword)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hostAdapter.experimentalVideoState.previewRunning || hostAdapter.experimentalVideoState.currentSession != null) {
            ComposeVideoStage(hostAdapter.experimentalVideoState.copy(peerListState = peerState))
        }
        SubtleContentSurface(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val transferState = ComposeFileTransferState(
                statusState = hostAdapter.statusState,
                peerListState = peerState,
                entries = hostAdapter.transferEntries,
                incomingPrompts = hostAdapter.incomingTransferPrompts,
                autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles,
            )
            if (transcript.isEmpty() && transferState.chatAttachmentCards.isEmpty()) {
                ChatTranscriptEmptyState(chatState, hostAdapter.chatConnected)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    items(transcript.size) { index ->
                        ChatTranscriptLine(transcript[index], hostAdapter.statusState.nickname)
                    }
                    items(transferState.chatAttachmentCards.size) { index ->
                        ChatAttachmentCardRow(transferState.chatAttachmentCards[index])
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactButton(
                onClick = ::attachSelectedFile,
                enabled = hostAdapter.chatConnected && selectedFilePeer != null,
            ) { Text(attachmentTools.title) }
            CompactTextField(
                draftMessage,
                { draftMessage = it },
                label = "",
                modifier = Modifier.weight(1f),
                onSubmit = ::sendDraftMessage,
                placeholder = "Type a message for the shared chat...",
            )
            CompactButton(
                onClick = ::sendDraftMessage,
                enabled = draftMessage.isNotBlank() && hostAdapter.chatConnected,
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun ChatTranscriptLine(message: ComposeChatMessage, localNickname: String = "") {
    val presentation = ComposeChatTranscriptLinePresentation.from(message.displayText, localNickname, message.timestamp)
    val accent = when (presentation.kind) {
        ComposeChatTranscriptLineKind.LOCAL -> MaterialTheme.colors.primary
        ComposeChatTranscriptLineKind.REMOTE -> MaterialTheme.colors.onSurface.copy(alpha = 0.82f)
        ComposeChatTranscriptLineKind.PRESENCE -> Color(0xFF8FB7FF)
        ComposeChatTranscriptLineKind.WARNING -> MaterialTheme.colors.error
        ComposeChatTranscriptLineKind.DIAGNOSTIC -> MaterialTheme.colors.onSurface.copy(alpha = 0.54f)
    }
    val bubbleColor = when (presentation.kind) {
        ComposeChatTranscriptLineKind.LOCAL -> MaterialTheme.colors.primary.copy(alpha = if (MaterialTheme.colors.isLight) 0.14f else 0.18f)
        ComposeChatTranscriptLineKind.REMOTE -> MaterialTheme.colors.onSurface.copy(alpha = if (MaterialTheme.colors.isLight) 0.06f else 0.08f)
        ComposeChatTranscriptLineKind.PRESENCE -> accent.copy(alpha = 0.08f)
        ComposeChatTranscriptLineKind.WARNING -> MaterialTheme.colors.error.copy(alpha = 0.10f)
        ComposeChatTranscriptLineKind.DIAGNOSTIC -> fieldBackgroundColor().copy(alpha = 0.36f)
    }
    val alignment = when (presentation.kind) {
        ComposeChatTranscriptLineKind.LOCAL -> Alignment.CenterEnd
        ComposeChatTranscriptLineKind.REMOTE -> Alignment.CenterStart
        else -> Alignment.Center
    }
    val bubbleWidth = when (presentation.kind) {
        ComposeChatTranscriptLineKind.LOCAL, ComposeChatTranscriptLineKind.REMOTE -> 0.82f
        else -> 0.96f
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            modifier = Modifier.fillMaxWidth(bubbleWidth),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = if (presentation.kind == ComposeChatTranscriptLineKind.REMOTE) 0.48f else 0.28f)),
            color = bubbleColor,
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = presentation.label,
                            style = MaterialTheme.typography.caption,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = presentation.displayTime,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.46f),
                    )
                }
                SelectionContainer {
                    Text(
                        text = presentation.body,
                        modifier = Modifier.fillMaxWidth(),
                        style = if (presentation.kind == ComposeChatTranscriptLineKind.REMOTE || presentation.kind == ComposeChatTranscriptLineKind.LOCAL) MaterialTheme.typography.body2 else MaterialTheme.typography.caption,
                        color = if (presentation.kind == ComposeChatTranscriptLineKind.WARNING) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.80f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatAttachmentCardRow(card: ComposeChatAttachmentCard) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = if (card.needsDecision) MaterialTheme.colors.primary.copy(alpha = 0.10f) else fieldBackgroundColor().copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(card.title, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                Text(card.progressLabel, style = MaterialTheme.typography.caption, color = if (card.failed) MaterialTheme.colors.error else MaterialTheme.colors.primary)
            }
            Text(card.subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
            if (!card.needsDecision) {
                LinearProgressIndicator(
                    progress = card.progressPercent.coerceIn(0, 100) / 100f,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = if (card.failed) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.10f),
                )
            }
        }
    }
}

@Composable
private fun ChatTranscriptEmptyState(chatState: ComposeChatWorkspaceState, connected: Boolean) {
    Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chatState.transcriptEmptyTitle,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
            )
            Text(
                text = if (connected) chatState.transcriptEmptyDetailConnected else chatState.transcriptEmptyDetailDisconnected,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun LiveFileTransferCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    var filePath by remember { mutableStateOf("") }
    val autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles
    val selectedPeer = peerState.selectedPeer
        ?.takeIf { it.online }
        ?.let { selected -> hostAdapter.discoveredPeerFor(selected.nickname) }
    val transferState = ComposeFileTransferState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        selectedFilePath = filePath,
        senderId = hostAdapter.statusState.nickname,
        sessionPassword = hostAdapter.currentRoomPassword,
        entries = hostAdapter.transferEntries,
        incomingPrompts = hostAdapter.incomingTransferPrompts,
        autoAcceptFiles = autoAcceptFiles,
    )

    SubtleContentSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TransferHeroPanel(transferState)
            ReceiveModePanel(
                transferState = transferState,
                autoAcceptFiles = autoAcceptFiles,
                onAutoAcceptChanged = hostAdapter::updateAutoAcceptIncomingFiles,
            )
            RecentTransfersPanel(transferState)
            val waitingPrompts = transferState.incomingPrompts.filter { it.waitingForDecision }
            val recentDecisions = transferState.incomingPrompts.filterNot { it.waitingForDecision }.takeLast(3)
            if (waitingPrompts.isNotEmpty() || recentDecisions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    waitingPrompts.forEach { prompt ->
                        IncomingTransferPromptRow(prompt, hostAdapter)
                    }
                    recentDecisions.forEach { prompt ->
                        Text(
                            "${prompt.statusLabel}: ${prompt.fileName} from ${prompt.senderId}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                        )
                    }
                }
            }
            SendEncryptedFilePanel(
                transferState = transferState,
                filePath = filePath,
                onFilePathChange = { filePath = it },
                onChooseFile = {
                    openComposeFileChooser("Choose file to send to ${transferState.selectedPeerName}")?.let { filePath = it.toString() }
                },
                onSend = {
                    val peer = selectedPeer ?: return@SendEncryptedFilePanel
                    hostAdapter.sendFileToPeer(Path.of(filePath), hostAdapter.statusState.nickname, peer, hostAdapter.currentRoomPassword)
                },
                sendEnabled = transferState.canSendSelectedFile && selectedPeer != null,
            )
            TransferDiagnosticsPanel(hostAdapter.transferDiagnostics)
        }
    }
}

@Composable
private fun TransferHeroPanel(transferState: ComposeFileTransferState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("Transfers", style = MaterialTheme.typography.subtitle1)
        Text(transferState.heroTitle, style = MaterialTheme.typography.subtitle2)
        if (transferState.heroSubtitle != transferState.peerListState.selectedPeerMeta) {
            Text(
                transferState.heroSubtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TransferInfoChip(transferState.transferCountSummary)
            TransferInfoChip(transferState.receiveModeShortLabel)
        }
    }
}

@Composable
private fun TransferInfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun RecentTransfersPanel(transferState: ComposeFileTransferState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Recent transfer activity", style = MaterialTheme.typography.subtitle2)
        if (transferState.recentEntryRows.isEmpty()) {
            Text(
                text = "No files sent or received yet. Transfer results will stay here after they finish.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
        } else {
            transferState.recentEntryRows.forEach { row -> TransferActivityRow(row) }
        }
    }
}

@Composable
private fun TransferActivityRow(row: ComposeTransferRow) {
    val accent = when {
        row.failed -> MaterialTheme.colors.error
        row.active -> MaterialTheme.colors.primary
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.title, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
            Text(row.progressLabel, style = MaterialTheme.typography.caption, color = accent)
        }
        LinearProgressIndicator(
            progress = row.percent / 100f,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = accent,
            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.10f),
        )
        Text(
            row.detail,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
        )
    }
}

@Composable
private fun SendEncryptedFilePanel(
    transferState: ComposeFileTransferState,
    filePath: String,
    onFilePathChange: (String) -> Unit,
    onChooseFile: () -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
) {
    ComposeAdvancedPane("Send encrypted file") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Send to ${transferState.selectedPeerName}", style = MaterialTheme.typography.subtitle2)
                Text(
                    transferState.targetSummary,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("File", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface)
                SelectedFileSummary(
                    filePath = filePath,
                    fallbackSummary = transferState.selectedFileSummary,
                    modifier = Modifier.weight(1f),
                )
                CompactButton(onClick = onChooseFile) { Text("Browse") }
            }
            Text(
                "Sender and encryption password are reused from the current room connection.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SectionShape,
                border = BorderStroke(1.dp, sectionBorderColor()),
                color = if (sendEnabled) MaterialTheme.colors.primary.copy(alpha = 0.08f) else fieldBackgroundColor(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        transferState.nextStepSummary,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
                        modifier = Modifier.weight(1f),
                    )
                    CompactButton(onClick = onSend, enabled = sendEnabled, modifier = Modifier.widthIn(min = 132.dp)) { Text("Send encrypted file") }
                }
            }
        }
    }
}

@Composable
private fun SelectedFileSummary(
    filePath: String,
    fallbackSummary: String,
    modifier: Modifier = Modifier,
) {
    val trimmedPath = filePath.trim()
    val fileName = trimmedPath
        .takeIf(String::isNotEmpty)
        ?.let { runCatching { Paths.get(it).fileName?.toString() ?: it }.getOrDefault(it) }
    Surface(
        modifier = modifier.heightIn(min = 34.dp),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = fileName ?: fallbackSummary,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = if (fileName == null) 0.58f else 0.86f),
                maxLines = 1,
            )
            if (fileName != null && trimmedPath != fileName) {
                Text(
                    text = trimmedPath,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TransferDiagnosticsPanel(diagnostics: List<String>) {
    val visibleDiagnostics = diagnostics.filterNot { it.isAutoAcceptToggleDiagnostic() }
    if (visibleDiagnostics.isEmpty()) {
        Text(
            "Diagnostics will appear here if a transfer starts, completes, or fails.",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
        )
    } else {
        ComposeAdvancedPane("Transfer diagnostics") {
            visibleDiagnostics.takeLast(4).forEach { diagnostic ->
                Text("• $diagnostic", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
        }
    }
}

private fun String.isAutoAcceptToggleDiagnostic(): Boolean =
    this == "Incoming file auto-accept enabled." ||
        this == "Incoming file auto-accept disabled; confirmation is required."

@Composable
private fun ReceiveModePanel(
    transferState: ComposeFileTransferState,
    autoAcceptFiles: Boolean,
    onAutoAcceptChanged: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = 0.72f)),
        color = if (autoAcceptFiles) MaterialTheme.colors.primary.copy(alpha = 0.08f) else fieldBackgroundColor().copy(alpha = 0.62f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(autoAcceptFiles, onAutoAcceptChanged)
            TitleWithHelp(
                title = transferState.receiveModeLabel,
                tooltip = transferState.receiveModeDescription,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IncomingTransferPromptRow(
    prompt: ComposeIncomingTransferPrompt,
    hostAdapter: ComposeDesktopHostAdapter,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(prompt.header, style = MaterialTheme.typography.subtitle2)
                Text(
                    "${prompt.fileName} · ${prompt.sizeLabel} · ${prompt.remoteAddress}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            CompactButton(onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, true) }, modifier = Modifier.widthIn(min = 76.dp)) { Text("Accept") }
            CompactButton(onClick = { hostAdapter.recordIncomingFileDecision(prompt.id, false) }, modifier = Modifier.widthIn(min = 76.dp)) { Text("Decline") }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickShareHeader(quickShareState)
            QuickShareServerPanel(
                state = quickShareState,
                port = quickSharePort,
                onPortChange = { quickSharePort = it },
                expirationMinutes = expirationMinutes,
                onExpirationMinutesChange = { expirationMinutes = it },
                accessLimit = accessLimit,
                onAccessLimitChange = { accessLimit = it },
                onStart = { quickShareState.port?.let(hostAdapter::startQuickShare) },
                onStop = { hostAdapter.stopQuickShare() },
                onCopyIndex = { copyToSystemClipboard(hostAdapter.quickShareLandingUrls.firstOrNull().orEmpty()) },
            )
            QuickShareCreateLinksPanel(
                state = quickShareState,
                filePath = filePath,
                onFilePathChange = { filePath = it },
                onChooseFile = {
                    openComposeFileChooser("Choose file to share by LAN browser link")?.let { filePath = it.toString() }
                },
                onCreateFile = {
                    val minutes = quickShareState.expirationMinutes ?: return@QuickShareCreateLinksPanel
                    val limit = quickShareState.accessLimit ?: return@QuickShareCreateLinksPanel
                    hostAdapter.createFileQuickShare(Path.of(filePath), minutes, limit)
                },
                textDraft = textDraft,
                onTextDraftChange = { textDraft = it },
                onCreateText = {
                    val minutes = quickShareState.expirationMinutes ?: return@QuickShareCreateLinksPanel
                    val limit = quickShareState.accessLimit ?: return@QuickShareCreateLinksPanel
                    hostAdapter.createTextQuickShare(textDraft, minutes, limit)
                },
            )
            QuickShareLinksPanel(
                state = quickShareState,
                onCopy = { copyToSystemClipboard(it) },
                onStop = { hostAdapter.stopQuickShareEntry(it) },
            )
            QuickShareDiagnosticsPanel(
                diagnostics = hostAdapter.quickShareDiagnostics,
                readinessSummary = quickShareState.readinessSummary,
            )
        }
    }
}

@Composable
private fun QuickShareHeader(state: ComposeQuickShareState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.title, style = MaterialTheme.typography.subtitle1)
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            QuickShareStatusPill(state.statusText, active = state.running)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.error.copy(alpha = 0.26f)),
            color = MaterialTheme.colors.error.copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.14f),
        ) {
            Text(
                text = state.trustedLanWarning,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
            )
        }
    }
}

@Composable
private fun QuickShareServerPanel(
    state: ComposeQuickShareState,
    port: String,
    onPortChange: (String) -> Unit,
    expirationMinutes: String,
    onExpirationMinutesChange: (String) -> Unit,
    accessLimit: String,
    onAccessLimitChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCopyIndex: () -> Unit,
) {
    QuickShareSection(title = "Server and link limits", subtitle = state.statusDetail) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactButton(onClick = onStart, enabled = state.canStartServer) { Text(state.serverActionLabel) }
            CompactButton(onClick = onStop, enabled = state.canStopServer) { Text("Stop server") }
            CompactButton(onClick = onCopyIndex, enabled = state.canCopyIndex) { Text("Copy index") }
        }
        QuickShareInfoLine(label = "Browser index", value = state.landingText)
        ComposeAdvancedPane("Advanced limits and port") {
            Text(
                text = "Defaults are enough for most trusted-LAN shares. Change these only for port conflicts or shorter access windows.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactTextField(port, onPortChange, label = "Port", modifier = Modifier.weight(1f))
                CompactTextField(expirationMinutes, onExpirationMinutesChange, label = "Minutes", modifier = Modifier.weight(1f))
            }
            CompactTextField(accessLimit, onAccessLimitChange, label = "Open limit", modifier = Modifier.fillMaxWidth())
            Text(state.policySummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
        }
    }
}

@Composable
private fun QuickShareCreateLinksPanel(
    state: ComposeQuickShareState,
    filePath: String,
    onFilePathChange: (String) -> Unit,
    onChooseFile: () -> Unit,
    onCreateFile: () -> Unit,
    textDraft: String,
    onTextDraftChange: (String) -> Unit,
    onCreateText: () -> Unit,
) {
    QuickShareSection(
        title = "Create a browser link",
        subtitle = "New file and text links use the same expiration and open limit above.",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            border = BorderStroke(1.dp, sectionBorderColor()),
            color = fieldBackgroundColor(),
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("File", style = MaterialTheme.typography.subtitle2)
                CompactTextField(
                    value = filePath,
                    onValueChange = onFilePathChange,
                    label = "Path",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Choose a local file",
                )
                Text(state.selectedFileLabel, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton(onClick = onChooseFile) { Text("Choose file") }
                    CompactButton(onClick = onCreateFile, enabled = state.canCreateFileShare) { Text(state.fileShareActionLabel) }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            border = BorderStroke(1.dp, sectionBorderColor()),
            color = fieldBackgroundColor(),
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Text", style = MaterialTheme.typography.subtitle2)
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = onTextDraftChange,
                    label = { Text("Text to share") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
                CompactButton(onClick = onCreateText, enabled = state.canCreateTextShare) { Text(state.textShareActionLabel) }
            }
        }
    }
}

@Composable
private fun QuickShareLinksPanel(
    state: ComposeQuickShareState,
    onCopy: (String) -> Unit,
    onStop: (String) -> Unit,
) {
    QuickShareSection(title = "Links", subtitle = "${state.activeShareCountLabel} · ${state.inactiveShareCountLabel}") {
        if (state.shareRowsDetailed.isEmpty()) {
            QuickShareEmptyState(state)
        } else {
            state.shareRowsDetailed.forEach { row ->
                QuickShareLinkRow(row = row, onCopy = onCopy, onStop = onStop)
            }
        }
    }
}

@Composable
private fun QuickShareLinkRow(
    row: ComposeQuickShareRow,
    onCopy: (String) -> Unit,
    onStop: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.title, style = MaterialTheme.typography.subtitle2)
                    Text(
                        text = "${row.typeLabel} · ${row.detail}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                    )
                }
                QuickShareStatusPill(row.statusLabel, active = row.active)
            }
            Text(row.url.ifBlank { "URL unavailable" }, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactButton(onClick = { onCopy(row.url) }, enabled = row.url.isNotBlank()) { Text("Copy link") }
                CompactButton(onClick = { onStop(row.id) }, enabled = row.active) { Text("Stop link") }
            }
        }
    }
}

@Composable
private fun QuickShareDiagnosticsPanel(
    diagnostics: List<String>,
    readinessSummary: String,
) {
    ComposeAdvancedPane("Readiness and diagnostics") {
        Text(readinessSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f))
        val recentDiagnostics = diagnostics.takeLast(4)
        if (recentDiagnostics.isEmpty()) {
            Text("No quick-share diagnostics yet.", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f))
        } else {
            recentDiagnostics.forEach { diagnostic ->
                Text("• $diagnostic", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
        }
    }
}

@Composable
private fun QuickShareSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = if (MaterialTheme.colors.isLight) MaterialTheme.colors.background else MaterialTheme.colors.background.copy(alpha = 0.52f),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Text(subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            content()
        }
    }
}

@Composable
private fun QuickShareInfoLine(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f))
            Text(value, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface)
        }
    }
}

@Composable
private fun QuickShareEmptyState(state: ComposeQuickShareState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(state.emptySharesTitle, style = MaterialTheme.typography.subtitle2)
            Text(state.emptySharesDetail, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            state.quickStartSteps.forEach { step ->
                Text(step, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
        }
    }
}

@Composable
private fun QuickShareStatusPill(text: String, active: Boolean) {
    val color = when {
        active -> MaterialTheme.colors.primary
        text.contains("expired", ignoreCase = true) || text.contains("limit", ignoreCase = true) -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
        text.contains("stopped", ignoreCase = true) -> MaterialTheme.colors.onSurface.copy(alpha = 0.52f)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.60f)
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.36f)),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = color,
        )
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
    val hideEnabled = !previewOnly && state.canHideMessage
    val inspectEnabled = !previewOnly && state.canInspectCover
    val extractEnabled = !previewOnly && state.canExtractMessage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SteganographyHeader(state)
            SteganographyStatusPanel(state)
            SteganographyHidePanel(
                state = state,
                coverPath = coverPath,
                onCoverPathChange = onCoverPathChange,
                onChooseCover = onChooseCover,
                outputPath = outputPath,
                onOutputPathChange = onOutputPathChange,
                onChooseOutput = onChooseOutput,
                message = message,
                onMessageChange = onMessageChange,
                password = password,
                onPasswordChange = onPasswordChange,
                encrypt = encrypt,
                onEncryptChange = onEncryptChange,
                onInspect = onInspect,
                onHide = onHide,
                inspectEnabled = inspectEnabled,
                hideEnabled = hideEnabled,
                previewOnly = previewOnly,
            )
            SteganographyExtractPanel(
                state = state,
                inputPath = inputPath,
                onInputPathChange = onInputPathChange,
                onChooseInput = onChooseInput,
                password = password,
                onPasswordChange = onPasswordChange,
                encryptedExtract = encryptedExtract,
                onEncryptedExtractChange = onEncryptedExtractChange,
                onExtract = onExtract,
                extractEnabled = extractEnabled,
                previewOnly = previewOnly,
            )
            SteganographyResultPanel(state)
            SteganographyFooterActions(
                enabled = !previewOnly,
                onClear = {
                    onCoverPathChange("")
                    onInputPathChange("")
                    onOutputPathChange("")
                    onMessageChange("")
                    onPasswordChange("")
                    onEncryptChange(false)
                    onEncryptedExtractChange(false)
                },
            )
        }
    }
}

@Composable
private fun SteganographyHeader(state: ComposeSteganographyState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(state.title, style = MaterialTheme.typography.subtitle1)
            Text(
                text = "Hide text inside an image, or extract text from a stego BMP.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SteganographyStatusPill(state.statusText)
            SteganographyStepChip("Cover", state.hasCover)
            SteganographyStepChip("Output", state.hasOutput)
            SteganographyStepChip("Message", state.hasMessage)
            SteganographyStepChip("Input", state.hasInput)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = 0.72f)),
            color = fieldBackgroundColor().copy(alpha = 0.62f),
        ) {
            Text(
                text = "Input can be PNG, BMP, JPG, or JPEG. Output is saved as BMP so the hidden message can be extracted later.",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun SteganographyStatusPill(text: String) {
    val color = when {
        text.contains("failed", ignoreCase = true) || text.contains("rejected", ignoreCase = true) -> MaterialTheme.colors.error
        text.contains("saved", ignoreCase = true) || text.contains("extracted", ignoreCase = true) || text.contains("ready", ignoreCase = true) -> MaterialTheme.colors.primary
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.58f)
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f)),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = color,
        )
    }
}

@Composable
private fun SteganographyStatusPanel(state: ComposeSteganographyState) {
    ComposeAdvancedPane("Readiness details") {
        Text(state.readinessSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
        if (state.passwordRequiredForHide || state.passwordRequiredForExtract) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SteganographyStepChip("Password", state.passwordReady)
            }
        }
    }
}

@Composable
private fun SteganographyStepChip(label: String, ready: Boolean) {
    val color = if (ready) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.52f)
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.32f)),
        color = color.copy(alpha = 0.10f),
    ) {
        Text(
            text = "${if (ready) "✓" else "•"} $label",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.caption,
            color = color,
        )
    }
}

@Composable
private fun SteganographyHidePanel(
    state: ComposeSteganographyState,
    coverPath: String,
    onCoverPathChange: (String) -> Unit,
    onChooseCover: () -> Unit,
    outputPath: String,
    onOutputPathChange: (String) -> Unit,
    onChooseOutput: () -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    encrypt: Boolean,
    onEncryptChange: (Boolean) -> Unit,
    onInspect: () -> Unit,
    onHide: () -> Unit,
    inspectEnabled: Boolean,
    hideEnabled: Boolean,
    previewOnly: Boolean,
) {
    SteganographySection(
        title = "Hide a message in an image",
        subtitle = "1) Choose a cover image, 2) confirm the output BMP, 3) enter the message, 4) save the stego BMP.",
    ) {
        SteganographyFileRow(
            value = coverPath,
            onValueChange = onCoverPathChange,
            label = "Cover image",
            placeholder = "PNG, BMP, JPG, or JPEG",
            buttonText = "Choose cover",
            onChoose = onChooseCover,
            enabled = !previewOnly,
        )
        SteganographyFileRow(
            value = outputPath,
            onValueChange = onOutputPathChange,
            label = "Save stego BMP as",
            placeholder = "Suggested after cover is selected",
            buttonText = "Save as",
            onChoose = onChooseOutput,
            enabled = !previewOnly,
        )
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Message to hide") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )
        SteganographyPasswordRow(
            checked = encrypt,
            onCheckedChange = onEncryptChange,
            label = "Protect hidden message with a password",
            password = password,
            onPasswordChange = onPasswordChange,
        )
        SteganographyActionHint(
            text = if (hideEnabled) {
                "Ready to write a new BMP with the hidden message. The original image is not overwritten."
            } else {
                state.blockedReasons.firstOrNull { reason ->
                    reason.contains("cover", ignoreCase = true) ||
                        reason.contains("output", ignoreCase = true) ||
                        reason.contains("text", ignoreCase = true) ||
                        reason.contains("password", ignoreCase = true)
                } ?: "Complete the hide-message fields to continue."
            },
            active = hideEnabled,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactButton(onClick = onChooseCover, enabled = !previewOnly) { Text("Choose cover") }
            CompactButton(onClick = onInspect, enabled = inspectEnabled) { Text("Check capacity") }
            CompactButton(onClick = onHide, enabled = hideEnabled, modifier = Modifier.widthIn(min = 104.dp)) { Text("Save BMP") }
        }
    }
}

@Composable
private fun SteganographyExtractPanel(
    state: ComposeSteganographyState,
    inputPath: String,
    onInputPathChange: (String) -> Unit,
    onChooseInput: () -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    encryptedExtract: Boolean,
    onEncryptedExtractChange: (Boolean) -> Unit,
    onExtract: () -> Unit,
    extractEnabled: Boolean,
    previewOnly: Boolean,
) {
    SteganographySection(
        title = "Extract a hidden message",
        subtitle = "Choose a stego BMP produced by this tool. Enable password mode only if the message was protected.",
    ) {
        SteganographyFileRow(
            value = inputPath,
            onValueChange = onInputPathChange,
            label = "Stego BMP input",
            placeholder = "BMP with hidden text",
            buttonText = "Choose BMP",
            onChoose = onChooseInput,
            enabled = !previewOnly,
        )
        SteganographyPasswordRow(
            checked = encryptedExtract,
            onCheckedChange = onEncryptedExtractChange,
            label = "This message needs a password",
            password = password,
            onPasswordChange = onPasswordChange,
        )
        SteganographyActionHint(
            text = if (extractEnabled) {
                "Ready to extract. The hidden text will appear in the result area below."
            } else {
                state.blockedReasons.firstOrNull { reason ->
                    reason.contains("input", ignoreCase = true) || reason.contains("password", ignoreCase = true)
                } ?: "Choose a stego BMP to extract a message."
            },
            active = extractEnabled,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactButton(onClick = onChooseInput, enabled = !previewOnly) { Text("Choose stego BMP") }
            CompactButton(onClick = onExtract, enabled = extractEnabled, modifier = Modifier.widthIn(min = 112.dp)) { Text("Extract text") }
        }
    }
}

@Composable
private fun SteganographySection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor().copy(alpha = 0.62f),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.subtitle2)
                Text(subtitle, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
            content()
        }
    }
}

@Composable
private fun SteganographyFileRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    buttonText: String,
    onChoose: () -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CompactTextField(
                value = value,
                onValueChange = onValueChange,
                label = "",
                modifier = Modifier.weight(1f),
                placeholder = placeholder,
            )
            CompactButton(onClick = onChoose, enabled = enabled, modifier = Modifier.widthIn(min = 92.dp)) { Text(buttonText) }
        }
    }
}

@Composable
private fun SteganographyPasswordRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    password: String,
    onPasswordChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onCheckedChange)
            Text(label, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
        }
        if (checked) {
            CompactTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                placeholder = "Enter the shared stego password",
            )
        }
    }
}

@Composable
private fun SteganographyActionHint(text: String, active: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = 0.72f)),
        color = if (active) MaterialTheme.colors.primary.copy(alpha = 0.08f) else fieldBackgroundColor().copy(alpha = 0.62f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
        )
    }
}

@Composable
private fun SteganographyResultPanel(state: ComposeSteganographyState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Result and capacity", style = MaterialTheme.typography.subtitle2)
            Text(state.capacityText, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
            if (state.extractedMessage.isBlank()) {
                Text("Extracted text will appear here after a successful extraction.", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f))
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = 0.72f)),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.extractedSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary)
                        Text(state.extractedMessage, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SteganographyFooterActions(
    enabled: Boolean,
    onClear: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        CompactButton(onClick = onClear, enabled = enabled) { Text("Clear stego form") }
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
    val dialog = createNativeFileDialog(title, save).apply {
        filter?.let { chooserFilter ->
            filenameFilter = java.io.FilenameFilter { _, name -> chooserFilter.accepts(name) }
        }
        initialFile?.toAbsolutePath()?.normalize()?.let { path ->
            directory = path.parent?.toString()
            file = path.fileName?.toString()
        }
        isMultipleMode = false
    }

    return try {
        dialog.isVisible = true
        dialog.files.firstOrNull()?.toPath()?.toAbsolutePath()?.normalize()
    } finally {
        dialog.dispose()
    }
}

private fun createNativeFileDialog(title: String, save: Boolean): FileDialog {
    val parentWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
    val mode = if (save) FileDialog.SAVE else FileDialog.LOAD
    return when (parentWindow) {
        is Frame -> FileDialog(parentWindow, title, mode)
        is Dialog -> FileDialog(parentWindow, title, mode)
        else -> FileDialog(null as Frame?, title, mode)
    }
}

private fun ComposeFileChooserFilter.accepts(fileName: String): Boolean {
    if (extensions.isEmpty()) {
        return true
    }
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension.isNotBlank() && extensions.any { it.lowercase() == extension }
}

@Composable
private fun LiveAudioVideoDevicesCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val voiceState = hostAdapter.mediaVoiceState.copy(peerListState = peerState)
    val videoState = hostAdapter.experimentalVideoState.copy(peerListState = peerState)
    AudioVideoDevicesCardContent(
        voiceState = voiceState,
        videoState = videoState,
        onRefresh = hostAdapter::refreshMediaDevices,
        onMicrophoneSelected = hostAdapter::selectMicrophone,
        onSpeakerSelected = hostAdapter::selectSpeaker,
        onCameraSelected = hostAdapter::selectCamera,
        onTestMicrophone = { hostAdapter.testMicrophone() },
        onTestSpeaker = { hostAdapter.testSpeaker() },
        onTestCamera = { hostAdapter.testCamera() },
        onStartPreview = { hostAdapter.startCameraPreview() },
        onStopPreview = hostAdapter::closeCameraPreview,
        onStartVoice = {
            voiceState.selectedPeer?.let { peer ->
                hostAdapter.startRealtimeSession(
                    localPeer = hostAdapter.statusState.nickname,
                    remotePeer = peer.nickname,
                    mode = RtcSessionMode.AUDIO,
                )
            }
        },
        onStartVideo = {
            videoState.selectedPeer?.let { peer ->
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
private fun AudioVideoDevicesPreviewCard() {
    AudioVideoDevicesCardContent(
        voiceState = ComposeShellMetadata.DEFAULT_MEDIA_VOICE_STATE,
        videoState = ComposeShellMetadata.DEFAULT_VIDEO_STATE,
        onRefresh = {},
        onMicrophoneSelected = {},
        onSpeakerSelected = {},
        onCameraSelected = {},
        onTestMicrophone = {},
        onTestSpeaker = {},
        onTestCamera = {},
        onStartPreview = {},
        onStopPreview = {},
        onStartVoice = {},
        onStartVideo = {},
        onHangUp = {},
        diagnostics = emptyList(),
        previewOnly = true,
    )
}

@Composable
private fun AudioVideoDevicesCardContent(
    voiceState: ComposeMediaVoiceState,
    videoState: ComposeExperimentalVideoState,
    onRefresh: () -> Unit,
    onMicrophoneSelected: (String?) -> Unit,
    onSpeakerSelected: (String?) -> Unit,
    onCameraSelected: (String?) -> Unit,
    onTestMicrophone: () -> Unit,
    onTestSpeaker: () -> Unit,
    onTestCamera: () -> Unit,
    onStartPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onStartVoice: () -> Unit,
    onStartVideo: () -> Unit,
    onHangUp: () -> Unit,
    diagnostics: List<String>,
    previewOnly: Boolean = false,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Audio and video setup", style = MaterialTheme.typography.h6)
                Text(
                    text = "Choose your microphone, speakers, and camera before starting a call. Use the quick tests to confirm everything works.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(voiceState.permissionStatusLabel)
                StatusPill(videoState.permissionStatusLabel)
                StatusPill("Call target: ${voiceState.selectedPeerName}")
            }
            Button(onClick = onRefresh, enabled = !previewOnly && voiceState.canRefreshDevices, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh device list")
            }
            DeviceSettingsSection(
                title = "Microphone",
                description = "Speak a few words and watch the input meter. If it stays at 0%, choose another microphone or check system privacy settings.",
            ) {
                DeviceChoiceDropdown(
                    label = "Choose microphone",
                    choices = voiceState.microphones,
                    selected = voiceState.selectedMicrophone,
                    enabled = !previewOnly,
                    onSelected = onMicrophoneSelected,
                    helperText = voiceState.microphoneEmptyState,
                )
                AudioInputLevelMeter(percent = voiceState.localAudioPercent, label = voiceState.localAudioLabel)
                Text(voiceState.microphoneTestStatus, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onTestMicrophone, enabled = !previewOnly && voiceState.canTestMicrophone) { Text("Test microphone") }
                    Button(onClick = onStartVoice, enabled = !previewOnly && voiceState.canStartVoice) { Text(voiceState.startVoiceLabel) }
                    Button(onClick = onHangUp, enabled = !previewOnly && voiceState.canHangUp) { Text("Hang up") }
                }
            }
            DeviceSettingsSection(
                title = "Speakers",
                description = "Play a short test sound through the selected output. If you do not hear it, check volume, Bluetooth, or system output settings.",
            ) {
                DeviceChoiceDropdown(
                    label = "Choose speaker output",
                    choices = voiceState.outputDevices,
                    selected = voiceState.selectedOutputDevice,
                    enabled = !previewOnly,
                    onSelected = onSpeakerSelected,
                    helperText = voiceState.outputEmptyState,
                )
                Text(voiceState.speakerTestStatus, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
                Button(onClick = onTestSpeaker, enabled = !previewOnly && voiceState.canTestSpeaker) { Text("Test speakers") }
            }
            DeviceSettingsSection(
                title = "Camera",
                description = "Choose a camera, then start preview to check framing and lighting before a video call.",
            ) {
                StatusPill(videoState.previewStateLabel)
                DeviceChoiceDropdown(
                    label = "Choose camera",
                    choices = videoState.cameras,
                    selected = videoState.selectedCamera,
                    enabled = !previewOnly,
                    onSelected = onCameraSelected,
                    helperText = videoState.cameraEmptyState,
                )
                Text(videoState.previewActionHint, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
                CameraPreviewStatus(state = videoState)
                Text(videoState.cameraTestStatus, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onTestCamera, enabled = !previewOnly && videoState.canTestCamera) { Text("Test camera") }
                    Button(onClick = onStartPreview, enabled = !previewOnly && videoState.canStartPreview) { Text(videoState.startPreviewLabel) }
                    Button(onClick = onStopPreview, enabled = !previewOnly && videoState.canStopPreview) { Text(videoState.stopPreviewLabel) }
                    Button(onClick = onStartVideo, enabled = !previewOnly && videoState.canStartVideo) { Text(videoState.startVideoLabel) }
                    Button(onClick = onHangUp, enabled = !previewOnly && videoState.canHangUp) { Text("Hang up") }
                }
            }
            HelpNotice(
                title = "If access is blocked",
                body = "Allow camera and microphone access in browser or operating-system privacy settings, close other apps that may be using the device, then refresh this list.",
            )
            diagnostics.takeLast(3).forEach {
                Text("• $it", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            }
            Text(voiceState.readinessSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
            Text(videoState.readinessSummary, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
        }
    }
}

@Composable
private fun DeviceSettingsSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f), shape = SectionShape) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            Text(description, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
            content()
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.12f), shape = FieldShape) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun AudioInputLevelMeter(percent: Int, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Input level", style = MaterialTheme.typography.caption)
            Text("$percent%", style = MaterialTheme.typography.caption)
        }
        LinearProgressIndicator(progress = percent.coerceIn(0, 100) / 100f, modifier = Modifier.fillMaxWidth())
        Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
    }
}

@Composable
private fun CameraPreviewStatus(state: ComposeExperimentalVideoState) {
    Surface(color = MaterialTheme.colors.background.copy(alpha = 0.42f), shape = FieldShape) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val previewImage = remember(state.latestPreviewFrame) { state.latestPreviewFrame?.toPreviewImageBitmap() }
            if (previewImage != null) {
                Image(
                    bitmap = previewImage,
                    contentDescription = "Live camera preview from ${state.selectedCamera}",
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            } else {
                VideoSurfacePlaceholder(
                    title = state.previewStateLabel,
                    body = state.previewActionHint,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
            Text(state.previewStatus, style = MaterialTheme.typography.body2)
            Text(state.frameCaption, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
            Text(state.previewConfigurationLabel, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f))
        }
    }
}

private fun com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent.toPreviewImageBitmap(): ImageBitmap? {
    return try {
        val image = BufferedImage(width(), height(), BufferedImage.TYPE_INT_ARGB)
        val bgra = bgraPixels()
        var offset = 0
        for (y in 0 until height()) {
            for (x in 0 until width()) {
                val blue = bgra[offset].toInt() and 0xFF
                val green = bgra[offset + 1].toInt() and 0xFF
                val red = bgra[offset + 2].toInt() and 0xFF
                val alpha = bgra[offset + 3].toInt() and 0xFF
                image.setRGB(x, y, (alpha shl 24) or (red shl 16) or (green shl 8) or blue)
                offset += 4
            }
        }
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        org.jetbrains.skia.Image.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun HelpNotice(title: String, body: String) {
    Surface(color = MaterialTheme.colors.secondary.copy(alpha = 0.10f), shape = SectionShape) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Text(body, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
        }
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RuntimeDiagnosticsHero(diagnosticsState)
        RuntimeDiagnosticsChannels(diagnosticsState.channelCards)
        RuntimeDiagnosticsAlerts(diagnosticsState.alerts)
        RuntimeReadinessSection(regressionState, packagingState)
        ComposeAdvancedPane("Technical details") {
            DiagnosticsDetailLine("Fallback", diagnosticsState.fallbackStatus)
            DiagnosticsDetailLine("Connection", diagnosticsState.statusAdapterSummary)
            DiagnosticsDetailLine("Actions", diagnosticsState.connectionActionSummary)
            DiagnosticsDetailLine("Peer", diagnosticsState.selectedPeerSummary)
            DiagnosticsDetailLine("Channels", diagnosticsState.diagnosticChannelSummary)
            DiagnosticsDetailLine("Regression", regressionState.blockedSummary)
            DiagnosticsDetailLine("Packaging", packagingState.blockedSummary)
        }
    }
}

@Composable
private fun RuntimeDiagnosticsHero(state: ComposeDiagnosticsState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, runtimeStatusColor(state).copy(alpha = 0.28f)),
        color = runtimeStatusColor(state).copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.16f),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Runtime / Diagnostics", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = state.runtimeOverview,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                }
                RuntimeStatusPill(state.statusLabel, runtimeStatusColor(state))
            }
            RuntimeMetricRow(state)
        }
    }
}

@Composable
private fun RuntimeMetricRow(state: ComposeDiagnosticsState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RuntimeMetricTile(
            label = "Channels active",
            value = "${state.activeChannelCount}/${state.channelCards.size}",
            modifier = Modifier.weight(1f),
        )
        RuntimeMetricTile(
            label = "Events",
            value = state.totalDiagnosticMessages.toString(),
            modifier = Modifier.weight(1f),
        )
        RuntimeMetricTile(
            label = "Peers visible",
            value = state.peerListState.visiblePeers.size.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RuntimeMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 54.dp),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = 0.72f)),
        color = fieldBackgroundColor().copy(alpha = 0.72f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.subtitle2)
            Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
        }
    }
}

@Composable
private fun RuntimeDiagnosticsChannels(channels: List<ComposeDiagnosticChannel>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        channels.forEach { channel ->
            RuntimeDiagnosticChannelCard(channel)
        }
    }
}

@Composable
private fun RuntimeDiagnosticChannelCard(channel: ComposeDiagnosticChannel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SectionShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor().copy(alpha = if (channel.hasMessages) 0.78f else 0.48f),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(channel.title, style = MaterialTheme.typography.subtitle2)
                    Text(channel.description, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f))
                }
                RuntimeStatusPill(
                    text = channel.stateLabel,
                    color = if (channel.hasMessages) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.38f),
                )
            }
            if (channel.hasMessages) {
                channel.recentMessages.forEach { message ->
                    DiagnosticMessageRow(message)
                }
            } else {
                RuntimeEmptyState(channel.emptyState)
            }
        }
    }
}

@Composable
private fun RuntimeDiagnosticsAlerts(alerts: List<ComposeDiagnosticAlert>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Setup notes", style = MaterialTheme.typography.subtitle2)
        if (alerts.isEmpty()) {
            RuntimeEmptyState("No setup issues detected. The panel will keep collecting real runtime messages as activity happens.")
        } else {
            alerts.forEach { alert ->
                DiagnosticAlertRow(alert)
            }
        }
    }
}

@Composable
private fun DiagnosticAlertRow(alert: ComposeDiagnosticAlert) {
    val color = when (alert.kind) {
        ComposeDiagnosticAlertKind.ERROR -> MaterialTheme.colors.error
        ComposeDiagnosticAlertKind.WARNING -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
        ComposeDiagnosticAlertKind.INFO -> MaterialTheme.colors.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        color = color.copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.14f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(alert.title, style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.88f))
            Text(alert.message, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
        }
    }
}

@Composable
private fun RuntimeReadinessSection(
    regressionState: ComposeRegressionReadinessState,
    packagingState: ComposePackagingReadinessState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Validation readiness", style = MaterialTheme.typography.subtitle2)
        RuntimeReadinessRow(
            title = "Runtime checks",
            status = regressionState.runtimeEvidenceSummary,
            action = regressionState.nextActionSummary,
            ready = regressionState.allRuntimeValidated,
        )
        RuntimeReadinessRow(
            title = "Packaging checks",
            status = packagingState.artifactSummary,
            action = packagingState.promotionSummary,
            ready = packagingState.releaseValidationReady,
        )
    }
}

@Composable
private fun RuntimeReadinessRow(
    title: String,
    status: String,
    action: String,
    ready: Boolean,
) {
    val color = if (ready) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.42f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor()),
        color = fieldBackgroundColor().copy(alpha = 0.60f),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f))
                RuntimeStatusPill(if (ready) "Ready" else "Pending", color)
            }
            Text(status, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
            Text(action, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
        }
    }
}

@Composable
private fun RuntimeStatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), shape = FieldShape) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun DiagnosticMessageRow(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = if (MaterialTheme.colors.isLight) 0.035f else 0.055f),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun RuntimeEmptyState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        border = BorderStroke(1.dp, sectionBorderColor().copy(alpha = 0.46f)),
        color = MaterialTheme.colors.onSurface.copy(alpha = if (MaterialTheme.colors.isLight) 0.025f else 0.045f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
        )
    }
}

@Composable
private fun DiagnosticsDetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f))
        Text(value, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
    }
}

@Composable
private fun runtimeStatusColor(state: ComposeDiagnosticsState): Color = when {
    state.hasErrors -> MaterialTheme.colors.error
    state.hasWarnings -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
    state.alerts.isNotEmpty() -> MaterialTheme.colors.primary
    else -> MaterialTheme.colors.primary
}

private fun Color.copy(alpha: Float): Color = androidx.compose.ui.graphics.Color(
    red = red,
    green = green,
    blue = blue,
    alpha = alpha,
)

@Composable
private fun DeviceChoiceDropdown(
    label: String,
    choices: List<MediaDeviceChoice>,
    selected: MediaDeviceChoice,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
    helperText: String? = null,
) {
    var expanded by remember(label, selected.deviceId) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
        Box {
            Button(onClick = { expanded = true }, enabled = enabled && choices.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
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
        helperText?.let {
            Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f))
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

    PeerListContentSurface(modifier = Modifier.fillMaxSize()) {
        if (!previewState.hasAnyPeers) {
            PeerListEmptyState(previewState)
        } else {
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                PeerListGroup(
                    peers = previewState.onlinePeers,
                    sectionTitle = "Online",
                    peerState = previewState,
                    onPeerSelected = { nickname ->
                        selectedPeerIndex = previewState.visiblePeers.indexOfFirst { it.nickname == nickname }
                    },
                )
                PeerListGroup(
                    peers = previewState.offlinePeers,
                    sectionTitle = "Offline",
                    peerState = previewState,
                    onPeerSelected = { nickname ->
                        selectedPeerIndex = previewState.visiblePeers.indexOfFirst { it.nickname == nickname }
                    },
                )
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

