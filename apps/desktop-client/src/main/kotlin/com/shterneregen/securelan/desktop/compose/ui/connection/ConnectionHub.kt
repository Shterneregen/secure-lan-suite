package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget

@Composable
internal fun ConnectionHub(
    hostAdapter: ComposeDesktopHostAdapter,
    initialMode: ComposeConnectionHubMode = ComposeConnectionHubMode.HOST,
    selectedJoinTarget: ComposeConnectionJoinTarget? = null,
    wrapInCard: Boolean = true,
    onImportedTarget: (ComposeConnectionJoinTarget) -> Unit = {},
    formVisible: Boolean = true,
    landingContent: (@Composable (() -> Unit) -> Unit)? = null,
) {
    var routeKey by remember { mutableStateOf<String?>(null) }
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var nickname by remember { mutableStateOf(hostAdapter.statusState.nickname) }
    var password by remember { mutableStateOf(hostAdapter.currentRoomPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var serverChatPort by remember { mutableStateOf(hostAdapter.statusState.serverChatPortText) }
    var serverFilePort by remember { mutableStateOf(hostAdapter.statusState.serverFilePortText) }
    var discoverable by remember { mutableStateOf(hostAdapter.statusState.discoverable) }
    var manualHost by remember { mutableStateOf(hostAdapter.statusState.manualHost) }
    var clientChatPort by remember { mutableStateOf(hostAdapter.statusState.clientChatPortText) }
    var clientFilePort by remember { mutableStateOf(hostAdapter.statusState.clientFilePortText) }

    LaunchedEffect(initialMode) {
        mode = initialMode
    }

    LaunchedEffect(selectedJoinTarget) {
        val target = selectedJoinTarget ?: return@LaunchedEffect
        mode = ComposeConnectionHubMode.JOIN
        routeKey = target.routeKey
        manualHost = target.host
        clientChatPort = target.chatPortText
        clientFilePort = target.filePortText
    }

    LaunchedEffect(hostAdapter.statusState.localServerRunning, hostAdapter.statusState.clientConnected) {
        nickname = hostAdapter.statusState.nickname
        password = hostAdapter.currentRoomPassword
        serverChatPort = hostAdapter.statusState.serverChatPortText
        serverFilePort = hostAdapter.statusState.serverFilePortText
        discoverable = hostAdapter.statusState.discoverable
        if (selectedJoinTarget == null) {
            manualHost = hostAdapter.statusState.manualHost
            clientChatPort = hostAdapter.statusState.clientChatPortText
            clientFilePort = hostAdapter.statusState.clientFilePortText
        }
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
    var invitationTarget by remember { mutableStateOf<ComposeConnectionJoinTarget?>(null) }
    var targetName by remember { mutableStateOf(selectedJoinTarget?.nickname) }
    var targetSource by remember { mutableStateOf<String?>(null) }
    var showAddressEditor by remember { mutableStateOf(selectedJoinTarget == null) }
    var addressFocusRequest by remember { mutableStateOf(0) }
    LaunchedEffect(selectedJoinTarget) {
        selectedJoinTarget?.let {
            targetName = it.nickname
            targetSource = if (it == invitationTarget) "From invitation" else null
            showAddressEditor = false
        }
    }
    fun endpointEdited() {
        routeKey = null
        targetName = null
        targetSource = null
        invitationTarget = null
    }
    var attemptedMode by remember { mutableStateOf<ComposeConnectionHubMode?>(null) }
    var connectionFailure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mode, selectedJoinTarget) { connectionFailure = null }
    val actionInFlight = if (hostAdapter.statusState.operationInProgress) attemptedMode ?: mode else null
    LaunchedEffect(hostAdapter.statusState.operationInProgress, hostAdapter.statusState.localServerRunning,
        hostAdapter.statusState.clientConnected, attemptedMode) {
        if (attemptedMode != null && !hostAdapter.statusState.operationInProgress) {
            if (!hostAdapter.statusState.localServerRunning && !hostAdapter.statusState.clientConnected) {
                connectionFailure = if (attemptedMode == ComposeConnectionHubMode.HOST)
                    "Could not open the room. Check that the ports are available, then try again."
                else "Could not join the room. Check the address and password, and that the host is available, then try again."
            }
            attemptedMode = null
        }
    }

    var importOpen by remember { mutableStateOf(false) }
    if (importOpen) ConnectionUriImportDialog(onClose = { importOpen = false }, onUse = { descriptor ->
        mode = ComposeConnectionHubMode.JOIN
        routeKey = null
        manualHost = descriptor.primaryAddress.value
        clientChatPort = descriptor.chatPort.toString()
        descriptor.filePortHint?.let { clientFilePort = it.toString() }
        password = descriptor.password.orEmpty()
        passwordVisible = false
        importOpen = false
        targetName = descriptor.displayNameHint ?: "Room"
        targetSource = "From invitation"
        showAddressEditor = false
        connectionFailure = null
        val imported = ComposeConnectionJoinTarget(descriptor.displayNameHint ?: "Room", manualHost, clientChatPort, clientFilePort)
        invitationTarget = imported
        onImportedTarget(imported)
    })
    val content: @Composable () -> Unit = {
        androidx.compose.foundation.layout.Column {
            landingContent?.invoke { importOpen = true }
            if (formVisible) {
                ConnectionHubContent(
                    state = hubState,
                    actionInFlight = actionInFlight,
                    nickname = nickname,
                    onNicknameChange = { nickname = it; connectionFailure = null },
                    password = password,
                    onPasswordChange = { password = it; connectionFailure = null },
                    passwordVisible = passwordVisible,
                    onPasswordVisibleChange = { passwordVisible = it },
                    onGenerateNickname = { nickname = hostAdapter.generateNickname() },
                    mode = mode,
                    serverChatPort = serverChatPort,
                    onServerChatPortChange = { serverChatPort = it; connectionFailure = null },
                    serverFilePort = serverFilePort,
                    onServerFilePortChange = { serverFilePort = it; connectionFailure = null },
                    discoverable = discoverable,
                    onDiscoverableChange = { discoverable = it },
                    manualHost = manualHost,
                    onManualHostChange = { manualHost = it; endpointEdited(); connectionFailure = null },
                    clientChatPort = clientChatPort,
                    onClientChatPortChange = { clientChatPort = it; endpointEdited(); connectionFailure = null },
                    clientFilePort = clientFilePort,
                    onClientFilePortChange = { clientFilePort = it; endpointEdited(); connectionFailure = null },
                    onOpenRoom = {
                        attemptedMode = ComposeConnectionHubMode.HOST
                        connectionFailure = null
                        statusState.serverChatPort?.let { chat ->
                            statusState.serverFilePort?.let { file ->
                                hostAdapter.openRoom(nickname, password, chat, file, discoverable)
                            }
                        }
                    },
                    onStopHosting = { hostAdapter.stopHosting() },
                    onConnect = {
                        attemptedMode = ComposeConnectionHubMode.JOIN
                        connectionFailure = null
                        statusState.clientChatPort?.let { chat ->
                            statusState.clientFilePort?.let { file ->
                                hostAdapter.connect(manualHost, nickname, password, chat, file, routeKey)
                            }
                        }
                    },
                    onDisconnect = { hostAdapter.disconnect() },
                    onSetDiscoverable = { hostAdapter.setDiscoverable(it) },
                    joinTargetName = targetName,
                    joinTargetSource = targetSource,
                    showAddressEditor = showAddressEditor,
                    addressFocusRequest = addressFocusRequest,
                    onChangeRoom = { showAddressEditor = true; addressFocusRequest++ },
                    onImportRoomLink = { importOpen = true },
                    connectionFailure = connectionFailure,
                )
            }
        }
    }

    if (wrapInCard) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.large),
            border = androidx.compose.foundation.BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.surface,
        ) { content() }
    } else {
        content()
    }
}
