package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** One column on phones; a separate details pane is given space on tablets. Controls scroll too. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanDevicesPane(state: LanToolsState, model: LanToolsViewModel, onRequestNearby: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        val list = rememberLazyListState()
        LaunchedEffect(state.selectedSaved, state.selectedFound, wide) {
            if (!wide && (state.selectedSaved != null || state.selectedFound != null)) list.scrollToItem(1)
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LazyColumn(Modifier.weight(1f).fillMaxHeight().testTag("lan-device-list"), state = list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field(state.query, model::search, R.string.lan_search, "lan-search")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRequestNearby, enabled = state.outcome(LanOperation.DISCOVERY).phase != LanPhase.RUNNING) { Text(text(R.string.lan_refresh_devices)) }
                            Button(onClick = model::add, enabled = state.canModify) { Text(text(R.string.lan_add)) }
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expanded = true }) { Text(text(R.string.lan_show, filterLabel(state.filter))) }
                                DropdownMenu(expanded, { expanded = false }) {
                                    LanFilter.entries.forEach { filter -> DropdownMenuItem(text = { Text(filterLabel(filter)) }, onClick = { model.filter(filter); expanded = false }) }
                                }
                            }
                        }
                        SourcePicker(state, model)
                        Text(text(R.string.lan_discovery_intro), style = MaterialTheme.typography.bodySmall)
                        Outcome(state, LanOperation.DISCOVERY, model)
                        Outcome(state, LanOperation.WAKE, model)
                        state.notice?.let { Notice(it) }
                        if (state.found.partial) Text(text(R.string.lan_partial))
                    }
                }
                if (!wide && (state.selectedSaved != null || state.selectedFound != null)) item { DeviceDetails(state, model, scroll = false) }
                if (state.filter != LanFilter.DISCOVERED) {
                    item { Text(text(R.string.lan_saved_devices, state.savedRows.size.toString()), style = MaterialTheme.typography.titleMedium) }
                    if (state.savedRows.isEmpty()) item { Text(text(R.string.lan_empty_saved)) }
                }
                items(state.savedRows, key = { "saved:${it.id}" }) { device ->
                    Card(Modifier.fillMaxWidth().semantics { selected = state.selectedSaved == device.id }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(device.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(device.addressHint?.value ?: device.hostnameHint ?: text(R.string.lan_unknown))
                            CheckText(state.checks["saved:${device.id}"])
                            FlowRow {
                                TextButton(onClick = { model.selectSaved(device.id) }) { Text(text(R.string.lan_details)) }
                                TextButton(onClick = { model.diagnoseSaved(device.id) }, enabled = device.addressHint != null || device.hostnameHint != null) { Text(text(R.string.lan_diagnostics)) }
                                if (device.wake != null) TextButton(onClick = { model.wake(device.id) }, enabled = state.outcome(LanOperation.WAKE).phase != LanPhase.RUNNING) { Text(text(R.string.lan_wake)) }
                            }
                        }
                    }
                }
                if (state.filter != LanFilter.SAVED) {
                    item { Text(text(R.string.lan_found_devices, state.foundRows.size.toString()), style = MaterialTheme.typography.titleMedium) }
                    if (state.foundRows.isEmpty()) item { Text(text(R.string.lan_empty_found)) }
                }
                items(state.foundRows, key = { "found:${it.id}" }) { device ->
                    Card(Modifier.fillMaxWidth().semantics { selected = state.selectedFound == device.id }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(device.displayName ?: text(R.string.lan_unknown), style = MaterialTheme.typography.titleMedium)
                            Text(device.addresses.joinToString { it.value })
                            Text(text(if (device.stale) R.string.lan_historical else R.string.lan_observed), style = MaterialTheme.typography.bodySmall)
                            CheckText(state.checks["found:${device.id}"])
                            FlowRow {
                                TextButton(onClick = { model.selectFound(device.id) }) { Text(text(R.string.lan_details)) }
                                device.services.firstOrNull()?.let { service -> TextButton(onClick = {
                                    model.diagnoseFound(device.id, service.addresses.first().value, service.port)
                                }) { Text(text(R.string.lan_diagnostics)) } }
                            }
                        }
                    }
                }
            }
            if (wide) Box(Modifier.weight(1f).fillMaxHeight()) { DeviceDetails(state, model) }
        }
    }
}

@Composable
private fun filterLabel(filter: LanFilter) = text(when (filter) {
    LanFilter.ALL -> R.string.lan_all
    LanFilter.SAVED -> R.string.lan_saved_filter
    LanFilter.DISCOVERED -> R.string.lan_discovered_filter
})

@Composable
internal fun CheckText(check: LanCheck?) {
    if (check == null) Text(text(R.string.lan_no_check), style = MaterialTheme.typography.bodySmall)
    else {
        Text(text(R.string.lan_last_check, check.host, check.port.toString(), check.at.toString()), style = MaterialTheme.typography.bodySmall)
        check.outcome.message?.let { Notice(it, check.outcome.phase == LanPhase.FAILURE) }
    }
}

@Composable
private fun DeviceDetails(state: LanToolsState, model: LanToolsViewModel, scroll: Boolean = true) {
    Column(if (scroll) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val saved = state.devices.firstOrNull { it.id == state.selectedSaved }
        val found = state.found.devices.firstOrNull { it.id == state.selectedFound }
        when {
            saved != null -> {
                Text(saved.displayName, style = MaterialTheme.typography.titleLarge)
                Text(saved.addressHint?.value ?: text(R.string.lan_unknown))
                Text(text(R.string.lan_hostname_value, saved.hostnameHint ?: text(R.string.lan_unknown)))
                Text(text(R.string.lan_mac) + ": " + (saved.wake?.mac?.value ?: text(R.string.lan_unknown)))
                CheckText(state.checks["saved:${saved.id}"])
                TextButton(onClick = { model.edit(saved.id) }, enabled = state.canModify) { Text(text(R.string.lan_edit)) }
                TextButton(onClick = { model.requestDelete(saved.id) }, enabled = state.canModify) { Text(text(R.string.lan_delete)) }
                if (saved.wake != null) Button(onClick = { model.wake(saved.id) }, enabled = state.outcome(LanOperation.WAKE).phase != LanPhase.RUNNING) { Text(text(R.string.lan_wake)) }
            }
            found != null -> {
                Text(found.displayName ?: text(R.string.lan_unknown), style = MaterialTheme.typography.titleLarge)
                Text(text(R.string.lan_hostname_value, found.hostname ?: text(R.string.lan_unknown)))
                Text(text(R.string.lan_type_value, found.deviceType ?: text(R.string.lan_unknown)))
                Text(found.addresses.joinToString { it.value })
                Text(text(R.string.lan_source, found.sources.joinToString()))
                Text(found.scope)
                Text(text(R.string.lan_seen, found.lastSeen.toString()))
                Text(text(if (found.stale) R.string.lan_historical else R.string.lan_observed))
                CheckText(state.checks["found:${found.id}"])
                Text(text(R.string.lan_services), style = MaterialTheme.typography.titleMedium)
                found.services.forEach { service ->
                    Text("${service.instance}: ${service.type}")
                    service.addresses.forEach { address -> TextButton(onClick = { model.diagnoseFound(found.id, address.value, service.port) }) {
                        Text(text(R.string.lan_endpoint_diagnostics, address.value, service.port.toString()))
                    } }
                }
                Button(onClick = { model.saveFound(found.id) }, enabled = state.canModify) { Text(text(R.string.lan_save_new)) }
                Text(text(R.string.lan_review_copy), style = MaterialTheme.typography.bodySmall)
            }
            else -> Text(text(if (state.selectedFound != null) R.string.lan_record_gone else R.string.lan_choose_device))
        }
    }
}
