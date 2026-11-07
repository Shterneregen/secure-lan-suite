package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
import com.shterneregen.securelan.desktop.compose.ui.components.*
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

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
    joinTargetName: String? = null,
    joinTargetSource: String? = null,
    showAddressEditor: Boolean = true,
    addressFocusRequest: Int = 0,
    onChangeRoom: () -> Unit = {},
    onImportRoomLink: (() -> Unit)? = null,
    connectionFailure: String? = null,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val host = mode == ComposeConnectionHubMode.HOST
    val busy = actionInFlight != null || state.statusState.operationInProgress
    val enabled = state.credentialFieldsEnabled && !busy
    val chatInvalid = if (host) state.statusState.serverChatPort == null else state.statusState.clientChatPort == null
    val fileInvalid = if (host) state.statusState.serverFilePort == null else state.statusState.clientFilePort == null
    val samePorts = !chatInvalid && !fileInvalid && if (host) state.statusState.serverChatPort == state.statusState.serverFilePort
        else state.statusState.clientChatPort == state.statusState.clientFilePort
    var advancedExpanded by remember(mode) { mutableStateOf(false) }
    LaunchedEffect(mode, chatInvalid, fileInvalid, samePorts) {
        if (chatInvalid || fileInvalid || samePorts) advancedExpanded = true
    }
    val submit = {
        if (state.primaryActionEnabled && !busy && password.isNotBlank() && !samePorts) {
            if (host) onOpenRoom() else onConnect()
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        if (onImportRoomLink != null) FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
        ) {
            TextButton(onClick = onImportRoomLink, enabled = !busy) {
                Icon(SecureLanIcons.Link, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join via room link")
            }
            Text(if (host) "Import a room link to join instead of hosting." else "Review an invitation before filling the form.",
                Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
        }
        if (!host) ConnectionFormCard("selected-room") {
            BoxWithConstraints {
                val summary: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Icon(SecureLanIcons.DeviceNetwork, null, Modifier.size(28.dp), tint = tokens.colors.textSecondary)
                        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                        Text("Selected room: ${joinTargetName ?: manualHost.ifBlank { "Enter an address" }}", style = MaterialTheme.typography.subtitle1)
                        if (manualHost.isNotBlank()) Text("$manualHost:$clientChatPort | files $clientFilePort",
                            style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                        if (joinTargetSource != null) Text(joinTargetSource, style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                        }
                    }
                }
                if (maxWidth >= 600.dp) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    Box(Modifier.weight(1f)) { summary() }
                    CompactButton(onClick = onChangeRoom, enabled = enabled, tone = CompactButtonTone.SECONDARY) { Text("Change room") }
                } else Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    summary()
                    CompactButton(onClick = onChangeRoom, enabled = enabled, tone = CompactButtonTone.SECONDARY) { Text("Change room") }
                }
            }
            if (showAddressEditor) {
                val addressFocus = remember { FocusRequester() }
                LaunchedEffect(addressFocusRequest) { if (addressFocusRequest > 0) addressFocus.requestFocus() }
                ConnectionFormField(manualHost, onManualHostChange, "Room address", enabled = enabled,
                    inputModifier = Modifier.focusRequester(addressFocus), onSubmit = submit)
            }
        }
        ConnectionFormCard(if (host) "host-connection-form" else "join-connection-form") {
            if (host) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    Icon(SecureLanIcons.DeviceNetwork, null, Modifier.size(28.dp), tint = tokens.colors.textSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                        Text("Host a secure room", style = MaterialTheme.typography.h6)
                        Text("Create a password-protected room on this device.", style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                    }
                }
                Divider(color = tokens.colors.borderSubtle)
            }
            BoxWithConstraints {
                val nameField: @Composable (Modifier) -> Unit = { modifier ->
                    ConnectionFormField(nickname, { onNicknameChange(it.filterNot(Char::isWhitespace)) }, "Your name", modifier,
                        enabled = enabled, onSubmit = submit, trailingContent = {
                            CompactIconButton(onClick = onGenerateNickname, icon = SecureLanIcons.GenerateNickname,
                                contentDescription = "Generate a new nickname", enabled = enabled)
                        })
                }
                val passwordField: @Composable (Modifier) -> Unit = { modifier ->
                    ConnectionFormField(password, { onPasswordChange(it.filterNot(Char::isWhitespace)) }, "Room password", modifier,
                        enabled = enabled, onSubmit = submit,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingContent = {
                            CompactIconButton(onClick = { onPasswordVisibleChange(!passwordVisible) },
                                icon = if (passwordVisible) SecureLanIcons.VisibilityOff else SecureLanIcons.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password", enabled = enabled)
                        })
                }
                if (maxWidth >= 600.dp) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                    nameField(Modifier.weight(1f)); passwordField(Modifier.weight(1f))
                } else Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    nameField(Modifier.fillMaxWidth()); passwordField(Modifier.fillMaxWidth())
                }
            }
            BoxWithConstraints {
                val action: @Composable () -> Unit = {
                    CalmFocusButton(onClick = submit, enabled = state.primaryActionEnabled && !busy && password.isNotBlank() && !samePorts,
                        modifier = Modifier.widthIn(min = 210.dp).heightIn(min = 44.dp)) {
                        Icon(SecureLanIcons.RunCheck, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) { if (host) "Opening room..." else "Joining room..." } else state.primaryActionLabel)
                    }
                }
                val visibility: @Composable () -> Unit = {
                    Row(Modifier.toggleable(value = discoverable, enabled = !busy, role = Role.Switch, onValueChange = {
                        onDiscoverableChange(it)
                        if (state.statusState.localServerRunning) onSetDiscoverable(it)
                    }), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                        Switch(discoverable, onCheckedChange = null, enabled = !busy)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                            Text("Show room on local network", style = MaterialTheme.typography.subtitle2)
                            Text("People on your LAN can discover this room. A password is still required. Hidden rooms can be joined by address or invitation.",
                                style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                        }
                    }
                }
                if (host && maxWidth >= 700.dp) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    action(); Box(Modifier.weight(1f)) { visibility() }
                } else Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    action(); if (host) visibility()
                }
            }
            if (state.secondaryActionEnabled) CompactButton(
                onClick = if (host) onStopHosting else onDisconnect, enabled = !busy, tone = CompactButtonTone.DESTRUCTIVE,
            ) { Text(state.secondaryActionLabel) }
            if (connectionFailure != null) Text(connectionFailure, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.body2, color = MaterialTheme.colors.error)
            val blocked = state.blockedReason
            if (blocked != null && !chatInvalid && !fileInvalid) Text(blocked, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
            if (password.isBlank()) Text("Enter the room password to continue.", style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
            ComposeAdvancedPane(title = state.advancedSettingsTitle, expanded = advancedExpanded, onExpandedChange = { advancedExpanded = it }) {
                BoxWithConstraints {
                    val chatField: @Composable (Modifier) -> Unit = { modifier ->
                        ConnectionFormField(if (host) serverChatPort else clientChatPort,
                            if (host) onServerChatPortChange else onClientChatPortChange, "Chat port", modifier, enabled = enabled,
                            error = if (chatInvalid) "Enter a number from 1 to 65535." else null)
                    }
                    val fileField: @Composable (Modifier) -> Unit = { modifier ->
                        ConnectionFormField(if (host) serverFilePort else clientFilePort,
                            if (host) onServerFilePortChange else onClientFilePortChange, "File port", modifier, enabled = enabled,
                            error = if (fileInvalid) "Enter a number from 1 to 65535." else if (samePorts) "Choose a different port from the chat port." else null)
                    }
                    if (maxWidth >= 420.dp) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                        chatField(Modifier.weight(1f)); fileField(Modifier.weight(1f))
                    } else Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        chatField(Modifier.fillMaxWidth()); fileField(Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Text(when {
            state.statusState.localServerRunning -> "Room is running."
            state.statusState.clientConnected -> "Room is connected."
            host -> "Room is not active yet."
            else -> "Check the selected room and password, then choose Join room."
        },
            style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
    }
}

@Composable
private fun ConnectionFormCard(tag: String, content: @Composable ColumnScope.() -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(Modifier.fillMaxWidth().testTag(tag), shape = RoundedCornerShape(tokens.radius.medium),
        color = tokens.colors.surfaceLevel2, border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.md), content = content)
    }
}

@Composable
private fun ConnectionFormField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    enabled: Boolean, visualTransformation: VisualTransformation = VisualTransformation.None,
    inputModifier: Modifier = Modifier, onSubmit: (() -> Unit)? = null, error: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val errorVisibility = remember { BringIntoViewRequester() }
    LaunchedEffect(error) { if (error != null) errorVisibility.bringIntoView() }
    Column(modifier.bringIntoViewRequester(errorVisibility), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        Text(label, style = MaterialTheme.typography.subtitle2)
        CompactTextField(value, onValueChange, label = "", modifier = Modifier.fillMaxWidth(), enabled = enabled,
            visualTransformation = visualTransformation, trailingContent = trailingContent, outlined = true,
            onSubmit = onSubmit, inputModifier = inputModifier.testTag("connection-$label").semantics {
                contentDescription = label
                if (error != null) this.error(error)
            })
        if (error != null) Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
    }
}
