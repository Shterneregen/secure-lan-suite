package com.shterneregen.securelan.androidclient.ui.connection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.MainViewModel
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.call.CallPhase
import com.shterneregen.securelan.androidclient.model.MainUiState
import com.shterneregen.securelan.androidclient.network.discovery.AndroidLanNetworkMonitor
import com.shterneregen.securelan.androidclient.ui.qr.QrScanScreen
import com.shterneregen.securelan.androidclient.ui.qr.renderQrCode
import com.shterneregen.securelan.chat.connection.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.LanAddressPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shterneregen.securelan.androidclient.ui.appStringResource as stringResource

/** Temporary import/scanner/invitation surfaces leave the communication workspace owned by its ViewModel. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomConnectionTools(viewModel: MainViewModel, state: MainUiState) {
    var paste by remember { mutableStateOf(false) }
    var scan by remember { mutableStateOf(false) }
    var invite by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    val calls by viewModel.callState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.connected && !state.hosting) {
                OutlinedButton(onClick = { paste = true }) {
                    Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.room_paste))
                }
                OutlinedButton(onClick = { scan = true }, enabled = calls.phase in setOf(CallPhase.IDLE, CallPhase.ENDED)) {
                    Icon(Icons.Outlined.QrCodeScanner, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.room_scan))
                }
            } else {
                TextButton(onClick = { paste = true }) { Text(stringResource(R.string.room_paste)) }
            }
            if (state.hosting) FilledTonalButton(onClick = { invite = true }) {
                Icon(Icons.Outlined.QrCode, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.room_invite))
            }
        }
        if (state.hosting) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val visibilityLabel = stringResource(R.string.room_discoverable)
            Switch(state.hostDiscoverable, viewModel::setHostDiscoverable, modifier = Modifier.semantics { contentDescription = visibilityLabel })
            Text(stringResource(R.string.room_discoverable))
        }
        if (state.hosting && state.hostLegacyNsdScopeUnavailable) {
            TextButton(onClick = { details = !details }) { Text(stringResource(R.string.room_network_details)) }
            if (details) Text(stringResource(R.string.room_legacy_scope))
        }
        if (state.roomDiscovery.providers.any { it.availability in setOf(com.shterneregen.securelan.chat.discovery.ProviderAvailability.FAILED,
                com.shterneregen.securelan.chat.discovery.ProviderAvailability.UNAVAILABLE) }) Text(stringResource(R.string.room_discovery_partial))
    }
    if (scan) QrScanScreen(onResult = { scan = false; viewModel.importConnectionUri(it) }, onPaste = { scan = false; paste = true }, onClose = { scan = false })
    if (paste) {
        var raw by remember { mutableStateOf("") }
        val clipboard = LocalClipboardManager.current
        AlertDialog(onDismissRequest = { paste = false }, title = { Text(stringResource(R.string.room_paste)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(raw, { raw = it.take(4097) }, label = { Text(stringResource(R.string.room_link)) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { raw = clipboard.getText()?.text.orEmpty().take(4097) }) { Text(stringResource(R.string.room_paste_clipboard)) }
            }
        }, confirmButton = { TextButton(onClick = { viewModel.importConnectionUri(raw); paste = false }) { Text(stringResource(R.string.room_review)) } },
            dismissButton = { TextButton(onClick = { paste = false }) { Text(stringResource(R.string.room_close)) } })
    }
    if (invite) RoomInvitationDialog(state) { invite = false }
}

/** Always composed, including over an active room: an external intent cannot disconnect that room. */
@Composable
fun PendingRoomConnectionDialog(viewModel: MainViewModel, state: MainUiState) {
    val draft = state.pendingConnection
    if (draft == null && state.connectionImportError == null) return
    val context = LocalContext.current
    val bindings by produceState(emptyList<com.shterneregen.securelan.common.net.interfaces.NetworkBinding>(), draft) {
        value = withContext(Dispatchers.IO) { runCatching { AndroidLanNetworkMonitor(context).snapshot() }.getOrDefault(emptyList()) }
    }
    val local = draft?.let { d -> bindings.any { it.prefix.contains(d.primaryAddress) } } == true
    val broadcast = draft?.let { d -> bindings.any { it.prefix.isDirectedBroadcast(d.primaryAddress) } } == true
    var confirmed by remember(draft) { mutableStateOf(false) }
    var passwordVisible by remember(draft?.roomInstanceIdHint, draft?.peerIdHint, draft?.password) { mutableStateOf(false) }
    val occupied = state.connected || state.hosting || state.hostingStarting
    AlertDialog(onDismissRequest = viewModel::dismissConnectionImport, title = { Text(stringResource(R.string.room_review)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.connectionImportError != null) Text(stringResource(when (state.connectionImportError) {
                ConnectionUriError.UnsupportedVersion, ConnectionUriError.UnsupportedProtocol -> R.string.room_link_unsupported
                else -> R.string.room_link_invalid
            }))
            if (draft != null) {
                Text(draft.displayNameHint ?: stringResource(R.string.room_invite))
                Text("${draft.primaryAddress.value}:${draft.chatPort}")
                if (draft.alternativeAddresses.isNotEmpty()) (listOf(draft.primaryAddress) + draft.alternativeAddresses).forEach { address ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(address == draft.primaryAddress, { viewModel.chooseImportedAddress(address) }, modifier = Modifier.semantics { contentDescription = address.value })
                        Text(address.value)
                    }
                }
                if (occupied) Text(stringResource(R.string.room_leave_first)) else {
                    OutlinedTextField(state.nickname, viewModel::updateNickname, label = { Text(stringResource(R.string.nickname)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(state.importedRoomPassword, viewModel::updateImportedRoomPassword,
                        label = { Text(stringResource(R.string.session_password)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    stringResource(if (passwordVisible) R.string.hide_password else R.string.show_password))
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (!local) Row {
                        val label = stringResource(R.string.room_external_confirm)
                        Checkbox(confirmed, { confirmed = it }, modifier = Modifier.semantics { contentDescription = label })
                        Text(stringResource(R.string.room_external_confirm))
                    }
                    if (broadcast) Text(stringResource(R.string.room_link_invalid))
                    Text(stringResource(if (draft.password == null) R.string.room_link_hint else R.string.room_link_password_included))
                }
            }
        }
    }, confirmButton = {
        if (draft != null) {
            if (occupied) TextButton(onClick = { if (state.hosting) viewModel.stopHosting() else viewModel.disconnect() }) { Text(stringResource(R.string.room_leave_action)) }
            else TextButton(onClick = viewModel::confirmImportedConnection, enabled = state.connectionImportError == null && !state.connecting && state.importedRoomPassword.isNotBlank() && !broadcast && (local || confirmed)) {
                Text(stringResource(R.string.connect))
            }
        }
    }, dismissButton = { TextButton(onClick = viewModel::dismissConnectionImport) { Text(stringResource(R.string.room_close)) } })
}

@Composable
private fun RoomInvitationDialog(state: MainUiState, onClose: () -> Unit) {
    val addresses = state.localRoomAddresses.mapNotNull(IpLiteral::parse).filter { LanAddressPolicy.portable(it) }
    var chosen by remember { mutableStateOf<IpLiteral?>(null) }
    val address = chosen?.takeIf { it in addresses } ?: addresses.firstOrNull()
    val uri = remember(state.hosting, state.hostRoomInstanceId, address, state.hostedFilePort, state.hostChatPort, state.hostedPeerId, state.sessionPassword) {
        if (!state.hosting || address == null || state.hostRoomInstanceId == null) null else runCatching {
            RoomConnectionUriCodec.encode(RoomConnectionDescriptor(address, state.hostChatPort.toInt(),
                state.hostedFilePort, peerIdHint = state.hostedPeerId, roomInstanceIdHint = state.hostRoomInstanceId, password = state.sessionPassword))
        }.getOrNull()
    }
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = null
        value = withContext(Dispatchers.Default) { uri?.let { runCatching { renderQrCode(it) }.getOrNull() } }
    }
    val clipboard = LocalClipboardManager.current
    var previous by remember { mutableStateOf(uri) }
    var changed by remember { mutableStateOf(false) }
    LaunchedEffect(uri) { if (previous != null && previous != uri) changed = true; previous = uri }
    AlertDialog(onDismissRequest = onClose, title = { Text(stringResource(R.string.room_invite)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.room_invite_hint))
            if (changed) Text(stringResource(R.string.room_address_changed))
            if (uri == null) Text(stringResource(R.string.room_no_address))
            if (uri != null) {
                bitmap?.let { Image(it, stringResource(R.string.room_invite), Modifier.fillMaxWidth().aspectRatio(1f), filterQuality = FilterQuality.None) }
                Text("${address?.value}:${state.hostChatPort}")
                if (addresses.size > 1) addresses.forEach { candidate -> Row {
                    RadioButton(candidate == address, { chosen = candidate }, modifier = Modifier.semantics { contentDescription = candidate.value }); Text(candidate.value)
                } }
            }
        }
    }, confirmButton = { TextButton(onClick = { uri?.let { clipboard.setText(AnnotatedString(it)) } }, enabled = uri != null) { Text(stringResource(R.string.room_copy)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.room_close)) } })
}
