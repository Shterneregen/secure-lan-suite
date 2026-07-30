package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalReducedMotion
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
import com.shterneregen.securelan.desktop.compose.ui.components.*
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.util.MicrointeractionTone

@Composable
internal fun ConnectionHubContent(
    state: ComposeConnectionHubState,
    actionInFlight: ComposeConnectionHubMode?,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean = false,
    onPasswordVisibleChange: (Boolean) -> Unit = {},
    onGenerateNickname: () -> Unit = {},
    mode: ComposeConnectionHubMode,
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
    val tokens = LocalSecureLanDesignTokens.current
    val credentialFieldsEnabled = state.credentialFieldsEnabled && actionInFlight == null
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactTextField(
                nickname,
                onNicknameChange,
                label = "Your name",
                modifier = Modifier.weight(1f),
                enabled = credentialFieldsEnabled,
                trailingContent = {
                    CompactIconButton(
                        onClick = onGenerateNickname,
                        icon = SecureLanIcons.GenerateNickname,
                        contentDescription = "Generate a new nickname",
                        enabled = credentialFieldsEnabled,
                    )
                },
            )
            CompactTextField(
                password,
                onPasswordChange,
                label = "Room password",
                modifier = Modifier.weight(1f),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                enabled = credentialFieldsEnabled,
                trailingContent = {
                    CompactIconButton(
                        onClick = { onPasswordVisibleChange(!passwordVisible) },
                        icon = if (passwordVisible) SecureLanIcons.VisibilityOff else SecureLanIcons.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        enabled = credentialFieldsEnabled,
                    )
                },
            )
        }

        val reduced = LocalReducedMotion.current
        Crossfade(
            targetState = mode,
            animationSpec = motionTween(reduced),
            label = "ConnectionHubModeCrossfade",
        ) { currentMode ->
            val animatedState = state.copy(mode = currentMode)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalmFocusButton(
                        onClick = when (currentMode) {
                            ComposeConnectionHubMode.HOST -> onOpenRoom
                            ComposeConnectionHubMode.JOIN -> onConnect
                        },
                        enabled = animatedState.primaryActionEnabled,
                        modifier = Modifier.widthIn(min = 150.dp),
                    ) {
                        Text(animatedState.primaryActionLabel)
                    }
                    AnimatedVisibility(
                        visible = actionInFlight == currentMode,
                        enter = fadeIn(motionTween()) + expandHorizontally(motionTween(), expandFrom = Alignment.Start),
                        exit = shrinkHorizontally(motionTween(), shrinkTowards = Alignment.Start) + fadeOut(motionTween()),
                    ) {
                        MicroFeedbackPill(
                            text = when (currentMode) {
                                ComposeConnectionHubMode.HOST -> "Opening room…"
                                ComposeConnectionHubMode.JOIN -> "Joining room…"
                            },
                            tone = MicrointeractionTone.LOADING,
                        )
                    }
                    CompactButton(
                        onClick = when (currentMode) {
                            ComposeConnectionHubMode.HOST -> onStopHosting
                            ComposeConnectionHubMode.JOIN -> onDisconnect
                        },
                        enabled = animatedState.secondaryActionEnabled,
                        modifier = Modifier.widthIn(min = 150.dp),
                        tone = CompactButtonTone.DESTRUCTIVE,
                    ) {
                        Text(animatedState.secondaryActionLabel)
                    }
                    if (currentMode == ComposeConnectionHubMode.HOST) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Switch(
                                checked = discoverable,
                                onCheckedChange = {
                                    onDiscoverableChange(it)
                                    if (animatedState.statusState.localServerRunning) {
                                        onSetDiscoverable(it)
                                    }
                                },
                                enabled = animatedState.discoverableToggleEnabled,
                            )
                            Text(animatedState.discoverableCompactLabel, style = MaterialTheme.typography.body2)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    val statusMessage = animatedState.statusMessage
                    AnimatedVisibility(
                        visible = statusMessage != null,
                        enter = fadeIn(motionTween()) + slideInVertically(motionTween()) { it / 2 },
                        exit = slideOutVertically(motionTween()) { it / 2 } + fadeOut(motionTween()),
                    ) {
                        if (statusMessage != null) {
                            ConnectionHubStatusMessage(
                                text = statusMessage,
                                tone = animatedState.statusMessageTone,
                            )
                        }
                    }
                }

                ComposeAdvancedPane(
                    title = animatedState.advancedSettingsTitle,
                    bounded = false,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                        if (currentMode == ComposeConnectionHubMode.HOST) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactTextField(
                                    serverChatPort,
                                    onServerChatPortChange,
                                    label = "Chat port",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTextField(
                                    serverFilePort,
                                    onServerFilePortChange,
                                    label = "File port",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactTextField(
                                    manualHost,
                                    onManualHostChange,
                                    label = "Room address",
                                    modifier = Modifier.weight(1.3f)
                                )
                                CompactTextField(
                                    clientChatPort,
                                    onClientChatPortChange,
                                    label = "Chat port",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTextField(
                                    clientFilePort,
                                    onClientFilePortChange,
                                    label = "File port",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}
