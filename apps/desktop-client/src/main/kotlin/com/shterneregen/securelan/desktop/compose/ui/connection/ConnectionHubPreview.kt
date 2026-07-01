package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState

@Composable
internal fun ConnectionHubPreview(
    state: ComposeConnectionHubState,
    wrapInCard: Boolean = true,
) {
    var mode by remember { mutableStateOf(state.mode) }
    var nickname by remember { mutableStateOf(state.nickname) }
    var password by remember { mutableStateOf(state.password) }
    var passwordVisible by remember { mutableStateOf(false) }
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

    val content: @Composable () -> Unit = {
        ConnectionHubContent(
            state = hubState,
            actionInFlight = null,
            nickname = nickname,
            onNicknameChange = { nickname = it },
            password = password,
            onPasswordChange = { password = it },
            passwordVisible = passwordVisible,
            onPasswordVisibleChange = { passwordVisible = it },
            onGenerateNickname = {},
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
            onOpenRoom = {},
            onStopHosting = {},
            onConnect = {},
            onDisconnect = {},
            onSetDiscoverable = {},
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
