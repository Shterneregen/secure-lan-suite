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
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.ui.components.CalmFocusButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.components.ComposeAdvancedPane
import com.shterneregen.securelan.desktop.compose.ui.components.ConnectionHubStatusMessage

@Composable
internal fun CenterPanelCreateRoom(
    state: ComposeStatusConnectionState,
    localNetworkInfo: String,
    onCreateRoom: (String, String, Boolean, Int, Int) -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var nickname by remember { mutableStateOf(state.nickname) }
    var password by remember { mutableStateOf(state.roomPasswordPlaceholder) }
    var discoverable by remember { mutableStateOf(if (state.localServerRunning) state.discoverable else true) }
    var serverChatPort by remember { mutableStateOf(state.serverChatPortText) }
    var serverFilePort by remember { mutableStateOf(state.serverFilePortText) }
    var advancedExpanded by remember { mutableStateOf(false) }

    val formStatus = state.copy(
        nickname = nickname,
        roomPasswordPlaceholder = password,
        serverChatPortText = serverChatPort,
        serverFilePortText = serverFilePort,
        discoverable = discoverable,
    )
    val hubState = ComposeConnectionHubState(
        statusState = formStatus,
        mode = ComposeConnectionHubMode.HOST,
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
            modifier = Modifier.widthIn(max = 440.dp, min = 320.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Create a secure room", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.onSurface)
                Text(
                    "Set your name and a shared password for trusted people on this LAN.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                CompactTextField(nickname, { nickname = it.filterNot(Char::isWhitespace) }, label = "Display name", modifier = Modifier.fillMaxWidth())
                CompactTextField(
                    password,
                    { password = it.filterNot(Char::isWhitespace) },
                    label = "Room password",
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = discoverable,
                        onCheckedChange = { discoverable = it },
                        enabled = hubState.discoverableToggleEnabled,
                    )
                    Text(hubState.visibilityToggleLabel, style = MaterialTheme.typography.body2)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                CalmFocusButton(
                    onClick = {
                        hubState.statusState.serverChatPort?.let { chat ->
                            hubState.statusState.serverFilePort?.let { file ->
                                onCreateRoom(nickname, password, discoverable, chat, file)
                            }
                        }
                    },
                    enabled = hubState.primaryActionEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(hubState.primaryActionLabel) }

                CompactButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    tone = CompactButtonTone.TERTIARY,
                ) { Text("Cancel") }

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
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        CompactTextField(
                            serverChatPort,
                            { serverChatPort = it },
                            label = "Chat port",
                            modifier = Modifier.weight(1f),
                        )
                        CompactTextField(
                            serverFilePort,
                            { serverFilePort = it },
                            label = "File port",
                            modifier = Modifier.weight(1f),
                        )
                    }

                }
            }
        }
    }
}
