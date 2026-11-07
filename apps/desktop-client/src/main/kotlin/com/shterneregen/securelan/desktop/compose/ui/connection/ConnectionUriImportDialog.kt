package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.chat.connection.*
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.*
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.network.discovery.DesktopNetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Dimension

@Composable
internal fun ConnectionUriImportDialog(onUse: (RoomConnectionDescriptor) -> Unit, onClose: () -> Unit) {
    DialogWindow(onCloseRequest = onClose, title = "Import room link", resizable = true,
        state = rememberDialogState(size = DpSize(700.dp, 640.dp))) {
        LaunchedEffect(window) { window.minimumSize = Dimension(360, 360) }
        ConnectionUriImportContent(onUse, onClose)
    }
}

/** Local validation/prefill only. No address probes or connection occurs before the later Join action. */
@Composable
internal fun ConnectionUriImportContent(
    onUse: (RoomConnectionDescriptor) -> Unit,
    onClose: () -> Unit,
    loadBindings: suspend () -> List<NetworkBinding> = {
        withContext(Dispatchers.IO) { runCatching { NetworkBindingSelector.select(DesktopNetworkMonitor().snapshot()) }.getOrDefault(emptyList()) }
    },
) {
    val tokens = LocalSecureLanDesignTokens.current
    val inputFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { inputFocus.requestFocus() }
    var raw by remember { mutableStateOf("") }
    var linkVisible by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val result = remember(raw) { if (raw.isBlank()) null else RoomConnectionUriCodec.decode(raw.trim()) }
    val descriptor = (result as? ConnectionUriParseResult.Success)?.descriptor
    var selected by remember(descriptor) { mutableStateOf(descriptor?.primaryAddress) }
    val bindings by produceState<List<NetworkBinding>?>(null) { value = loadBindings() }
    val local = selected?.let { ip -> bindings.orEmpty().any { it.prefix.contains(ip) && !it.prefix.isDirectedBroadcast(ip) } } == true
    val broadcast = selected?.let { ip -> bindings.orEmpty().any { it.prefix.isDirectedBroadcast(ip) } } == true
    var confirmedExternal by remember(descriptor, selected) { mutableStateOf(false) }
    val canUse = descriptor != null && selected != null && bindings != null && !broadcast && (local || confirmedExternal)
    Surface(Modifier.fillMaxSize().onPreviewKeyEvent {
        if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) { onClose(); true } else false
    }, color = MaterialTheme.colors.background, shape = RoundedCornerShape(tokens.radius.large),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
        Column(Modifier.fillMaxSize().padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm), verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(tokens.radius.medium), color = tokens.colors.surfaceLevel2) {
                    Icon(SecureLanIcons.Link, null, Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colors.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    Text("Import room link", style = MaterialTheme.typography.h5)
                    Text("Paste a SecureLan invitation to review it before joining.", style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                }
                CompactIconButton(onClick = onClose, icon = SecureLanIcons.Close, contentDescription = "Close import room link")
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    Text("Room link", style = MaterialTheme.typography.subtitle2)
                    CompactTextField(raw, { raw = it.take(4097) }, label = "", modifier = Modifier.fillMaxWidth(), outlined = true,
                        placeholder = "securelan://join?...",
                        visualTransformation = if (linkVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        inputModifier = Modifier.focusRequester(inputFocus).testTag("room-link-input").semantics { contentDescription = "Room link" },
                        trailingContent = {
                            CompactIconButton(onClick = { linkVisible = !linkVisible },
                                icon = if (linkVisible) SecureLanIcons.VisibilityOff else SecureLanIcons.Visibility,
                                contentDescription = if (linkVisible) "Hide room link" else "Show room link")
                        })
                    Text("Room links may contain a password. Keep them private.", style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                    TextButton(onClick = { raw = clipboard.getText()?.text.orEmpty().take(4097); linkVisible = false; inputFocus.requestFocus() }) {
                        Icon(SecureLanIcons.Copy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Paste from clipboard")
                    }
                }
                if (result is ConnectionUriParseResult.Invalid) Text(connectionUriMessage(result.error),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colors.error, style = MaterialTheme.typography.body2)
                if (descriptor != null) Surface(Modifier.fillMaxWidth().testTag("invitation-preview"),
                    shape = RoundedCornerShape(tokens.radius.medium), color = tokens.colors.surfaceLevel2,
                    border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
                    Column(Modifier.padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                            Text("Invitation preview", style = MaterialTheme.typography.subtitle1)
                            Text(if (descriptor.password == null) "Password not included" else "Password included",
                                style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                        ImportPreviewValue("Room name", descriptor.displayNameHint ?: "Not specified")
                        ImportPreviewValue("Room address", selected?.value.orEmpty())
                        ImportPreviewValue("Chat port", descriptor.chatPort.toString())
                        ImportPreviewValue("File port", descriptor.filePortHint?.toString() ?: "Use configured default")
                        val addresses = listOf(descriptor.primaryAddress) + descriptor.alternativeAddresses
                        if (addresses.size > 1) {
                            Divider(color = tokens.colors.borderSubtle)
                            Text("Choose an address", style = MaterialTheme.typography.subtitle2)
                            addresses.forEach { ip ->
                                Row(Modifier.fillMaxWidth().selectable(selected == ip, role = Role.RadioButton, onClick = { selected = ip })
                                    .semantics { contentDescription = "Use address ${ip.value}" }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected == ip, onClick = null)
                                    Text(ip.value, style = MaterialTheme.typography.body2)
                                }
                            }
                        }
                        Divider(color = tokens.colors.borderSubtle)
                        Text(if (descriptor.password == null) "Enter the room password after importing."
                            else "The room password is included and will be filled in for Join.",
                            style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                        Text("Invitation details are supplied by the sender. Importing does not verify their identity or connect to the room.",
                            style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
                        when {
                            bindings == null -> Text("Checking local network information...", style = MaterialTheme.typography.body2)
                            broadcast -> Text("A network broadcast address cannot be used.", Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colors.error)
                            !local -> {
                                Text(if (bindings.isNullOrEmpty()) "Local network information is unavailable. Confirm the destination before importing."
                                    else "This address is outside the detected local networks.", style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                                Row(Modifier.fillMaxWidth().toggleable(confirmedExternal, role = Role.Checkbox, onValueChange = { confirmedExternal = it })
                                    .semantics { contentDescription = "Confirm address outside detected LAN prefixes" }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(confirmedExternal, onCheckedChange = null)
                                    Text("I confirm this destination address.", style = MaterialTheme.typography.body2)
                                }
                            }
                        }
                    }
                } else if (raw.isBlank()) Text("Paste a room link to see its room name, destination and ports here.",
                    style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
            }
            Divider(color = tokens.colors.borderSubtle)
            Row(Modifier.fillMaxWidth().testTag("import-actions"), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                CompactButton(onClick = onClose, tone = CompactButtonTone.TERTIARY) { Text("Cancel") }
                Spacer(Modifier.weight(1f))
                CalmFocusButton(onClick = { if (canUse) onUse(descriptor.copy(primaryAddress = selected!!)) }, enabled = canUse) {
                    Text("Use for Join")
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewValue(label: String, value: String) {
    val tokens = LocalSecureLanDesignTokens.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Text(label, Modifier.width(110.dp), style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.body2)
    }
}

private fun connectionUriMessage(error: ConnectionUriError): String = when (error) {
    ConnectionUriError.InputTooLong -> "The room link is too long. Copy it again from the host."
    ConnectionUriError.UnsupportedVersion, ConnectionUriError.UnsupportedProtocol -> "This room link requires a newer client."
    ConnectionUriError.InvalidAddress, ConnectionUriError.UnsupportedAddressScope -> "The link needs a portable numeric LAN address."
    ConnectionUriError.InvalidPort -> "The room link contains an invalid port. Copy it again from the host."
    ConnectionUriError.WrongScheme -> "Paste a SecureLan room link."
    else -> "The room link is incomplete or invalid. Copy it again from the host."
}
