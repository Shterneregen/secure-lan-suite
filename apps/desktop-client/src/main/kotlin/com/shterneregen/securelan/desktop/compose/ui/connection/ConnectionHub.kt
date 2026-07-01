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
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var nickname by remember { mutableStateOf(hostAdapter.statusState.nickname) }
    var password by remember { mutableStateOf(hostAdapter.currentRoomPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var serverChatPort by remember { mutableStateOf(hostAdapter.statusState.serverChatPortText) }
    var serverFilePort by remember { mutableStateOf(hostAdapter.statusState.serverFilePortText) }
    var discoverable by remember { mutableStateOf(defaultHostDiscoverable(hostAdapter.statusState.localServerRunning, hostAdapter.statusState.discoverable)) }
    var manualHost by remember { mutableStateOf(hostAdapter.statusState.manualHost) }
    var clientChatPort by remember { mutableStateOf(hostAdapter.statusState.clientChatPortText) }
    var clientFilePort by remember { mutableStateOf(hostAdapter.statusState.clientFilePortText) }

    LaunchedEffect(initialMode) {
        mode = initialMode
    }

    LaunchedEffect(selectedJoinTarget) {
        val target = selectedJoinTarget ?: return@LaunchedEffect
        mode = ComposeConnectionHubMode.JOIN
        manualHost = target.host
        clientChatPort = target.chatPortText
        clientFilePort = target.filePortText
    }

    LaunchedEffect(hostAdapter.statusState.localServerRunning, hostAdapter.statusState.clientConnected) {
        nickname = hostAdapter.statusState.nickname
        password = hostAdapter.currentRoomPassword
        serverChatPort = hostAdapter.statusState.serverChatPortText
        serverFilePort = hostAdapter.statusState.serverFilePortText
        discoverable = defaultHostDiscoverable(hostAdapter.statusState.localServerRunning, hostAdapter.statusState.discoverable)
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
    var actionInFlight by remember { mutableStateOf<ComposeConnectionHubMode?>(null) }
    LaunchedEffect(hostAdapter.statusState.localServerRunning, hostAdapter.statusState.clientConnected, hostAdapter.adapterEvents.size) {
        actionInFlight = null
    }

    val content: @Composable () -> Unit = {
        ConnectionHubContent(
            state = hubState,
            actionInFlight = actionInFlight,
            nickname = nickname,
            onNicknameChange = { nickname = it },
            password = password,
            onPasswordChange = { password = it },
            passwordVisible = passwordVisible,
            onPasswordVisibleChange = { passwordVisible = it },
            onGenerateNickname = { nickname = hostAdapter.generateNickname() },
            mode = mode,
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
                actionInFlight = ComposeConnectionHubMode.HOST
                statusState.serverChatPort?.let { chat ->
                    statusState.serverFilePort?.let { file ->
                        hostAdapter.openRoom(nickname, password, chat, file, discoverable)
                    }
                }
            },
            onStopHosting = { hostAdapter.stopHosting() },
            onConnect = {
                actionInFlight = ComposeConnectionHubMode.JOIN
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

private fun defaultHostDiscoverable(localServerRunning: Boolean, currentDiscoverable: Boolean): Boolean =
    if (localServerRunning) currentDiscoverable else true
