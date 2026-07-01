package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.ui.components.CalmFocusButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.components.ComposeAdvancedPane
import com.shterneregen.securelan.desktop.compose.ui.components.ConnectionHubStatusMessage

@Composable
internal fun CenterPanelDiscoverJoin(
    state: ComposeStatusConnectionState,
    localNetworkInfo: String,
    nearbyTargets: List<ComposeConnectionJoinTarget>,
    selectedTarget: ComposeConnectionJoinTarget?,
    onJoinRoom: (String, String, String, Int, Int) -> Unit,
    onCreateRoomInstead: () -> Unit,
    onTargetSelected: (ComposeConnectionJoinTarget?) -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var nickname by remember { mutableStateOf(state.nickname) }
    var password by remember { mutableStateOf(state.roomPasswordPlaceholder) }
    var manualHost by remember { mutableStateOf(selectedTarget?.host ?: state.manualHost) }
    var clientChatPort by remember { mutableStateOf(selectedTarget?.chatPortText ?: state.clientChatPortText) }
    var clientFilePort by remember { mutableStateOf(selectedTarget?.filePortText ?: state.clientFilePortText) }
    var advancedExpanded by remember { mutableStateOf(selectedTarget == null) }

    LaunchedEffect(selectedTarget) {
        selectedTarget?.let {
            manualHost = it.host
            clientChatPort = it.chatPortText
            clientFilePort = it.filePortText
        }
    }

    val formStatus = state.copy(
        nickname = nickname,
        roomPasswordPlaceholder = password,
        manualHost = manualHost,
        clientChatPortText = clientChatPort,
        clientFilePortText = clientFilePort,
    )
    val hubState = ComposeConnectionHubState(
        statusState = formStatus,
        mode = ComposeConnectionHubMode.JOIN,
        localNetworkInfo = localNetworkInfo,
    )

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = tokens.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.lg, Alignment.Top),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp, min = 320.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Join a secure room", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.onSurface)
                Text(
                    "Choose a nearby room or enter an address below.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }

            if (nearbyTargets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    nearbyTargets.take(5).forEach { target ->
                        CenterPanelRoomRow(
                            target = target,
                            selected = selectedTarget?.nickname == target.nickname,
                            onSelected = { onTargetSelected(if (selectedTarget?.nickname == target.nickname) null else target) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                CompactTextField(nickname, { nickname = it }, label = "Display name", modifier = Modifier.fillMaxWidth())
                CompactTextField(
                    password,
                    { password = it },
                    label = "Room password",
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                CalmFocusButton(
                    onClick = {
                        hubState.statusState.clientChatPort?.let { chat ->
                            hubState.statusState.clientFilePort?.let { file ->
                                onJoinRoom(nickname, password, manualHost, chat, file)
                            }
                        }
                    },
                    enabled = hubState.primaryActionEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(hubState.primaryActionLabel) }

                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    CompactButton(onClick = onCreateRoomInstead) { Text("Create room instead") }
                    CompactButton(onClick = onBack, tone = CompactButtonTone.TERTIARY) { Text("Cancel") }
                }

                val blockedReason = hubState.blockedReason
                if (blockedReason != null) {
                    ConnectionHubStatusMessage(text = blockedReason, tone = hubState.statusMessageTone)
                }
            }

            ComposeAdvancedPane(
                title = hubState.advancedSettingsTitle,
                expanded = advancedExpanded,
                onExpandedChange = { advancedExpanded = it },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    CompactTextField(
                        manualHost,
                        { manualHost = it },
                        label = "Room address",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        CompactTextField(
                            clientChatPort,
                            { clientChatPort = it },
                            label = "Chat port",
                            modifier = Modifier.weight(1f),
                        )
                        CompactTextField(
                            clientFilePort,
                            { clientFilePort = it },
                            label = "File port",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
