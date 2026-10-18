package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.chat.connection.*
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import com.shterneregen.securelan.desktop.network.discovery.DesktopNetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ConnectionUriImportDialog(onUse: (RoomConnectionDescriptor) -> Unit, onClose: () -> Unit) {
    DialogWindow(onCloseRequest = onClose, title = "Import room link", state = rememberDialogState(size = DpSize(520.dp, 510.dp)),
        onPreviewKeyEvent = { if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) { onClose(); true } else false }) {
        ConnectionUriImportContent(onUse, onClose)
    }
}

/** Import is local validation/prefill only. Authentication remains an explicit Join action. */
@Composable
internal fun ConnectionUriImportContent(onUse: (RoomConnectionDescriptor) -> Unit, onClose: () -> Unit) {
    val inputFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { inputFocus.requestFocus() }
    var raw by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val result = remember(raw) { if (raw.isBlank()) null else RoomConnectionUriCodec.decode(raw.trim()) }
    val descriptor = (result as? ConnectionUriParseResult.Success)?.descriptor
    var selected by remember(descriptor) { mutableStateOf(descriptor?.primaryAddress) }
    val bindings by produceState(emptyList<com.shterneregen.securelan.common.net.interfaces.NetworkBinding>()) {
        value = withContext(Dispatchers.IO) { runCatching { NetworkBindingSelector.select(DesktopNetworkMonitor().snapshot()) }.getOrDefault(emptyList()) }
    }
    val local = selected?.let { ip -> bindings.any { it.prefix.contains(ip) && !it.prefix.isDirectedBroadcast(ip) } } == true
    val broadcast = selected?.let { ip -> bindings.any { it.prefix.isDirectedBroadcast(ip) } } == true
    var confirmedExternal by remember(selected) { mutableStateOf(false) }
        Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paste a SecureLan room link", style = MaterialTheme.typography.h6)
                OutlinedTextField(raw, { raw = it.take(4097) }, label = { Text("Room link") }, modifier = Modifier.fillMaxWidth().focusRequester(inputFocus))
                TextButton(onClick = { raw = clipboard.getText()?.text.orEmpty().take(4097) }) { Text("Paste from clipboard") }
                if (result is ConnectionUriParseResult.Invalid) Text(connectionUriMessage(result.error), color = MaterialTheme.colors.error)
                if (descriptor != null) {
                    Text(descriptor.displayNameHint ?: "Secure room")
                    (listOf(descriptor.primaryAddress) + descriptor.alternativeAddresses).forEach { ip ->
                        Row { RadioButton(selected == ip, onClick = { selected = ip }, modifier = Modifier.semantics { contentDescription = ip.value }); Text("${ip.value}:${descriptor.chatPort}") }
                    }
                    Text(if (descriptor.password == null) "Enter the room password after importing."
                        else "The room password is included and will be filled in for Join.")
                    if (!local) Row {
                        Checkbox(confirmedExternal, { confirmedExternal = it }, modifier = Modifier.semantics { contentDescription = "Confirm address outside detected LAN prefixes" })
                        Text("Advanced: I confirm this address outside the detected LAN prefixes.")
                    }
                    if (broadcast) Text("A network broadcast address cannot be used.", color = MaterialTheme.colors.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { descriptor?.let { onUse(it.copy(primaryAddress = selected!!)) } },
                        enabled = descriptor != null && !broadcast && (local || confirmedExternal)) { Text("Use for Join") }
                    TextButton(onClick = onClose) { Text("Cancel") }
                }
            }
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
