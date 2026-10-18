package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.chat.connection.RoomConnectionDescriptor
import com.shterneregen.securelan.chat.connection.RoomConnectionUriCodec
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.LanAddressPolicy
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.qr.renderQrCode
import com.shterneregen.securelan.desktop.network.discovery.DesktopNetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun RoomInvitationDialog(hostAdapter: ComposeDesktopHostAdapter, onClose: () -> Unit) {
    val host = hostAdapter.hostedRoomAdvertisement
    val password = hostAdapter.currentRoomPassword
    val addresses by produceState(emptyList<com.shterneregen.securelan.common.net.address.IpLiteral>()) {
        while (true) {
            value = withContext(Dispatchers.IO) { runCatching { NetworkBindingSelector.select(DesktopNetworkMonitor().snapshot())
                .map { it.address }.filter { LanAddressPolicy.portable(it) } }.getOrDefault(emptyList()) }
            delay(5000)
        }
    }
    var selected by remember { mutableStateOf<com.shterneregen.securelan.common.net.address.IpLiteral?>(null) }
    val address = selected?.takeIf { it in addresses } ?: addresses.firstOrNull()
    val uri = remember(host, address, password) { if (host == null || address == null) null else runCatching {
        RoomConnectionUriCodec.encode(RoomConnectionDescriptor(address, host.chatPort, host.activeFilePort, host.peerId, host.roomInstanceId, host.displayName, password = password))
    }.getOrNull() }
    val qr by produceState<Result<ImageBitmap>?>(null, uri) {
        value = null
        value = withContext(Dispatchers.Default) { uri?.let { runCatching { renderQrCode(it) } } }
    }
    var previousUri by remember { mutableStateOf(uri) }
    var changed by remember { mutableStateOf(false) }
    LaunchedEffect(uri) { if (previousUri != null && previousUri != uri) changed = true; previousUri = uri }
    val clipboard = LocalClipboardManager.current
    DialogWindow(onCloseRequest = onClose, title = "Invite to room", state = rememberDialogState(size = DpSize(400.dp, 470.dp)),
        onPreviewKeyEvent = { if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) { onClose(); true } else false }) {
        RoomInvitationContent(addresses, address, host?.chatPort, uri, qr, changed, onAddressSelected = { selected = it },
            onCopy = { uri?.let { clipboard.setText(AnnotatedString(it)) } }, onClose = onClose)
    }
}

/** Keeps actions visible while network notices and additional addresses can scroll. */
@Composable
internal fun RoomInvitationContent(
    addresses: List<IpLiteral>,
    address: IpLiteral?,
    chatPort: Int?,
    uri: String?,
    qr: Result<ImageBitmap>?,
    changed: Boolean,
    onAddressSelected: (IpLiteral) -> Unit,
    onCopy: () -> Unit,
    onClose: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val copyFocus = remember { FocusRequester() }
    LaunchedEffect(uri != null) { if (uri != null) copyFocus.requestFocus() }
    Surface {
        Column(Modifier.fillMaxSize().padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("The QR and link include the room password. Share only with people you trust.", style = MaterialTheme.typography.body2)
                if (changed && chatPort != null) Text("The room address changed. Copy the updated link.")
                if (chatPort == null) Text("The room has stopped.")
                else if (address == null) Text("No suitable LAN address. Connect to Wi-Fi or Ethernet.")
                else if (uri == null) Text("Unable to create the invitation. Check the room password and its length.", color = MaterialTheme.colors.error)
                if (uri != null) {
                    qr?.getOrNull()?.let { Image(it, "Room invitation QR", Modifier.widthIn(max = 256.dp).fillMaxWidth().aspectRatio(1f), filterQuality = FilterQuality.None) }
                    if (qr == null) Text("Preparing QR…")
                    if (qr?.isFailure == true) Text("Unable to generate QR. Copy the room link instead.")
                    Text("${address?.value}:$chatPort", style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                    if (addresses.size > 1) addresses.forEach { candidate ->
                        Row(Modifier.fillMaxWidth().selectable(candidate == address, role = Role.RadioButton, onClick = { onAddressSelected(candidate) }),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(candidate == address, onClick = null)
                            Text(candidate.value, style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("Close") }
                Button(onClick = onCopy, enabled = uri != null, modifier = Modifier.focusRequester(copyFocus)) { Text("Copy room link") }
            }
        }
    }
}
