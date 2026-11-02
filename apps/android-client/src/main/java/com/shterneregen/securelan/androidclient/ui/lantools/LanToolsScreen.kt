package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Android Material 3 rendering only; network/store actions belong to the injected ViewModel. */
@Composable
internal fun LanToolsScreen(model: LanToolsViewModel, onBack: () -> Unit, onRequestNearby: () -> Unit) {
    val state by model.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).testTag("lan-tools"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text(R.string.lan_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack, modifier = Modifier.testTag("lan-back")) { Text(text(R.string.lan_back)) }
        }
        TabRow(state.tab.ordinal) {
            LanTab.entries.forEach { tab -> Tab(selected = state.tab == tab, onClick = { model.tab(tab) }, modifier = Modifier.testTag("lan-tab-${tab.name}"), text = {
                Text(text(if (tab == LanTab.DEVICES) R.string.lan_devices else R.string.lan_diagnostics))
            }) }
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
                state.form != null && state.tab == LanTab.DEVICES -> LanDeviceEditor(state, model)
                state.tab == LanTab.DEVICES -> LanDevicesPane(state, model, onRequestNearby)
                else -> LanDiagnosticsPane(state, model)
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
    Text(text(value.resource, *value.args.toTypedArray()), color = if (error) MaterialTheme.colorScheme.error else LocalContentColor.current,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
internal fun Outcome(state: LanToolsState, kind: LanOperation, model: LanToolsViewModel) {
    val outcome = state.outcome(kind)
    if (outcome.phase == LanPhase.IDLE) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        outcome.subject?.let { Notice(it) }
        val phase = when (outcome.phase) {
            LanPhase.RUNNING -> R.string.lan_running
            LanPhase.SUCCESS -> R.string.lan_phase_success
            LanPhase.FAILURE -> R.string.lan_phase_failure
            LanPhase.CANCELLED -> R.string.lan_phase_cancelled
            LanPhase.IDLE -> R.string.lan_unknown
        }
        Text(text(phase), style = MaterialTheme.typography.labelLarge)
        outcome.message?.let { Notice(it, outcome.phase == LanPhase.FAILURE) }
        if (outcome.phase == LanPhase.RUNNING) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = { model.cancel(kind) }) { Text(text(R.string.lan_cancel)) }
        }
    }
}

@Composable
internal fun SourcePicker(state: LanToolsState, model: LanToolsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val label = state.selectedInterface?.source?.let { "${it.displayName}: ${it.binding.address.value}" }
        ?: text(if (state.binding == null) R.string.lan_os_routing else R.string.lan_stale_binding)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("lan-source")) { Text(text(R.string.lan_source_picker, label)) }
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
internal fun Field(value: String, update: (String) -> Unit, label: Int, tag: String) {
    OutlinedTextField(value, update, Modifier.fillMaxWidth().testTag(tag), label = { Text(text(label)) }, singleLine = true)
}

/** This form is scrollable at phone/landscape sizes; Save is a real persistent operation. */
@Composable
internal fun LanDeviceEditor(state: LanToolsState, model: LanToolsViewModel) {
    val form = state.form ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field(form.name, { model.changeForm(form.copy(name = it)) }, R.string.lan_name, "lan-name")
        Field(form.address, { model.changeForm(form.copy(address = it)) }, R.string.lan_address, "lan-address")
        Field(form.hostname, { model.changeForm(form.copy(hostname = it)) }, R.string.lan_hostname, "lan-hostname")
        Field(form.mac, { model.changeForm(form.copy(mac = it)) }, R.string.lan_mac, "lan-mac")
        Text(text(R.string.lan_mac_help), style = MaterialTheme.typography.bodySmall)
        Field(form.broadcast, { model.changeForm(form.copy(broadcast = it)) }, R.string.lan_broadcast, "lan-broadcast")
        Field(form.port, { model.changeForm(form.copy(port = it)) }, R.string.lan_udp_port, "lan-udp-port")
        Field(form.preferred, { model.changeForm(form.copy(preferred = it)) }, R.string.lan_preferred, "lan-preferred")
        state.formError?.let { Notice(it, error = true) }
        Button(onClick = model::save, enabled = state.canModify, modifier = Modifier.testTag("lan-save")) { Text(text(R.string.lan_save)) }
        TextButton(onClick = model::cancelForm, enabled = !state.saving) { Text(text(R.string.lan_cancel)) }
    }
}
