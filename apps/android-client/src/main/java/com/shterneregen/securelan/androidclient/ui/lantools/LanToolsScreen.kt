package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Android Material 3 rendering only; network/store actions belong to the injected ViewModel. */
@Composable
internal fun LanToolsScreen(model: LanToolsViewModel, onBack: () -> Unit, onRequestNearby: () -> Unit) {
    val state by model.state.collectAsState()
    val list = rememberLazyListState()
    val diagnosticsScroll = rememberScrollState()
    val editor = state.editorOpen && state.form != null
    val details = state.tab == LanTab.DEVICES && state.detailsOpen
    val title = when {
        editor -> if (state.devices.any { it.id == state.form?.id }) R.string.lan_edit else R.string.lan_add
        details -> R.string.lan_device_details
        state.tab == LanTab.DIAGNOSTICS -> when (state.diagnosticPage) {
            LanDiagnosticPage.TCP -> R.string.lan_tcp
            LanDiagnosticPage.DNS -> R.string.lan_dns
            LanDiagnosticPage.GATEWAY -> R.string.lan_gateway
            LanDiagnosticPage.ADVANCED -> R.string.lan_advanced
            else -> R.string.lan_title
        }
        else -> R.string.lan_title
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxSize().padding(horizontal = 12.dp).testTag("lan-tools"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (!model.back()) onBack() }, modifier = Modifier.testTag("lan-back")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, text(R.string.back))
            }
            Text(text(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        if (!editor && !details && (state.tab != LanTab.DIAGNOSTICS || state.diagnosticPage == LanDiagnosticPage.OVERVIEW)) {
            Surface(shape = RoundedCornerShape(50), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().selectableGroup().padding(2.dp)) {
                    LanTab.entries.forEach { tab ->
                        Surface(color = if (state.tab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (state.tab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(50), modifier = Modifier.weight(1f)
                                .selectable(state.tab == tab, role = Role.Tab, onClick = { model.tab(tab) }).testTag("lan-tab-${tab.name}")) {
                            Box(Modifier.heightIn(min = 48.dp).padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(text(if (tab == LanTab.DEVICES) R.string.lan_devices else R.string.lan_diagnostics), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        if (state.loading || state.saving) {
            Text(text(if (state.loading) R.string.lan_loading else R.string.lan_saving), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        state.storeError?.let {
            Notice(it, error = true)
            TextButton(onClick = model::reload, enabled = !state.loading && !state.saving) { Text(text(R.string.lan_reload)) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                editor -> LanDeviceEditor(state, model)
                details -> LanDeviceDetails(state, model)
                state.tab == LanTab.DEVICES -> LanDevicesPane(state, model, onRequestNearby, list)
                else -> LanDiagnosticsPane(state, model, diagnosticsScroll)
            }
        }
    }
    }
    state.deleting?.let {
        AlertDialog(onDismissRequest = { model.requestDelete(null) }, title = { Text(text(R.string.lan_delete)) },
            text = { Text(text(R.string.lan_delete_confirm)) },
            confirmButton = { TextButton(onClick = model::delete, enabled = !state.saving) { Text(text(R.string.lan_delete)) } },
            dismissButton = { TextButton(onClick = { model.requestDelete(null) }, enabled = !state.saving) { Text(text(R.string.lan_cancel)) } })
    }
}

@Composable
internal fun Notice(value: LanMessage, error: Boolean = false) {
    Text(text(value.resource, *value.args.toTypedArray()), style = MaterialTheme.typography.bodySmall, color = if (error) MaterialTheme.colorScheme.error else LocalContentColor.current,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
internal fun SourcePicker(state: LanToolsState, model: LanToolsViewModel, modifier: Modifier = Modifier, compact: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val label = state.selectedInterface?.source?.let { "${it.displayName}: ${it.binding.address.value}" }
        ?: text(if (state.binding == null) R.string.lan_os_routing else R.string.lan_stale_binding)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = modifier.then(if (compact) Modifier else Modifier.fillMaxWidth()).testTag("lan-source")) {
            if (!compact) {
                Icon(Icons.Outlined.Layers, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text(if (compact) R.string.lan_network_short else R.string.lan_source_picker,
                if (compact) state.selectedInterface?.source?.displayName ?: label else label), style = MaterialTheme.typography.bodySmall)
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text(text(R.string.lan_os_routing)) }, onClick = { model.binding(null); expanded = false })
            state.network?.interfaces.orEmpty().forEach { entry ->
                DropdownMenuItem(text = { Text("${entry.source.displayName}: ${entry.source.binding.address.value}") },
                    onClick = { model.binding(entry.source.binding.key); expanded = false }, enabled = entry.source.binding.up)
            }
        }
    }
}

@Composable
internal fun Field(value: String, update: (String) -> Unit, label: Int, tag: String,
    modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, inputModifier: Modifier = Modifier, error: LanMessage? = null) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val fieldLabel = text(label)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Surface(Modifier.fillMaxWidth(), color = colors.surface.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (error != null) colors.error else if (focused) colors.primary else colors.outline)) {
        BasicTextField(value, update, enabled = enabled, singleLine = true, interactionSource = interactions,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (enabled) colors.onSurface else colors.onSurfaceVariant),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = if (tag in setOf("lan-udp-port", "lan-tcp-port", "lan-gateway-port", "lan-timeout")) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).then(inputModifier).testTag(tag)
                .semantics { contentDescription = fieldLabel }.padding(horizontal = 12.dp, vertical = 6.dp),
            decorationBox = { input ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    icon?.let { Icon(it, null, Modifier.size(22.dp), tint = if (focused) colors.primary else colors.onSurfaceVariant) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        if (value.isNotEmpty()) Text(fieldLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Box {
                            if (value.isEmpty()) Text(fieldLabel, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            input()
                        }
                    }
                }
            })
    }
    error?.let { Notice(it, error = true) }
    }
}
