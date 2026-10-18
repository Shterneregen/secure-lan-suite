package com.shterneregen.securelan.androidclient.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.network.AndroidQuickShareItem
import kotlinx.coroutines.delay
import com.shterneregen.securelan.androidclient.ui.appStringResource as stringResource

/** Publishes trusted-LAN links and displays their access limits; lifecycle and IO belong to callbacks. */
@Composable
fun QuickShareScreen(
    items: List<AndroidQuickShareItem>, urls: List<String>,
    onShareText: (String, Long, Int?) -> Unit, onChooseFile: (Long, Int?) -> Unit, onStop: (String) -> Unit, onRefresh: () -> Unit,
) {
    var text by remember { mutableStateOf("") }; var minutes by remember { mutableStateOf("60") }; var limit by remember { mutableStateOf("1") }
    val clipboard = LocalClipboardManager.current
    var qrShareId by remember { mutableStateOf<String?>(null) }
    val qrShare = items.firstOrNull { it.id == qrShareId && it.url.isNotBlank() }
    LaunchedEffect(items) {
        if (qrShareId != null && qrShare == null) qrShareId = null
    }
    if (qrShare != null) {
        QuickShareQrDialog(qrShare, onDismiss = { qrShareId = null })
    }
    LaunchedEffect(Unit) { while (true) { onRefresh(); delay(5_000) } }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(stringResource(R.string.nav_quick_share), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.quick_share_intro), style = MaterialTheme.typography.bodyMedium)
            if (urls.isNotEmpty()) Text(stringResource(R.string.quick_share_address, urls.first()), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.quick_share_text_input)) }, minLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(stringResource(R.string.quick_share_expiration)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(limit, { limit = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(stringResource(R.string.quick_share_limit)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onShareText(text, minutes.toLongOrNull()?.coerceAtLeast(1) ?: 60, limit.toIntOrNull()?.takeIf { it > 0 }); text = "" }, enabled = text.isNotBlank()) { Text(stringResource(R.string.quick_share_create_text)) }
                OutlinedButton(onClick = { onChooseFile(minutes.toLongOrNull()?.coerceAtLeast(1) ?: 60, limit.toIntOrNull()?.takeIf { it > 0 }) }) { Text(stringResource(R.string.quick_share_create_file)) }
            }
        }
        if (items.isEmpty()) item { Text(stringResource(R.string.quick_share_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(items, key = { it.id }) { item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    val typeLabel = stringResource(if (item.isText) R.string.quick_share_text_type else R.string.quick_share_file_type)
                    val accesses = item.accessLimit?.let { "${item.accessCount}/$it" } ?: item.accessCount.toString()
                    Text(stringResource(R.string.quick_share_accesses, typeLabel, accesses), style = MaterialTheme.typography.bodySmall)
                    SelectionContainer { Text(item.url, style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(item.url)) }) { Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.quick_share_copy_link)) }
                        IconButton(onClick = { qrShareId = item.id }, enabled = item.url.isNotBlank()) {
                            Icon(Icons.Outlined.QrCode, contentDescription = stringResource(R.string.quick_share_show_qr))
                        }
                        TextButton(onClick = { onStop(item.id) }) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.quick_share_stop)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickShareQrDialog(item: AndroidQuickShareItem, onDismiss: () -> Unit) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, item.url) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            runCatching { com.shterneregen.securelan.androidclient.ui.qr.renderQrCode(item.url) }.getOrNull()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_share_qr_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (item.isText) stringResource(R.string.quick_share_text_type) else item.name)
                if (bitmap != null) Image(
                    bitmap = bitmap!!,
                    contentDescription = stringResource(R.string.quick_share_qr_description),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    filterQuality = FilterQuality.None,
                )
                Text(stringResource(R.string.quick_share_qr_hint), style = MaterialTheme.typography.bodySmall)
                SelectionContainer { Text(item.url, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.quick_share_qr_close)) } },
    )
}
