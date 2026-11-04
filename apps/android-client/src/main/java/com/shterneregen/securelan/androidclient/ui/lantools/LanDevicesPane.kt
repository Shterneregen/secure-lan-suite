package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import com.shterneregen.securelan.androidclient.ui.ActionLabel
import com.shterneregen.securelan.androidclient.ui.IconTile
import com.shterneregen.securelan.androidclient.ui.SectionCard
import com.shterneregen.securelan.androidclient.ui.SectionHeading
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Selection opens a separate surface. The list state stays owned by the screen while it is hidden. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanDevicesPane(state: LanToolsState, model: LanToolsViewModel, onRequestNearby: () -> Unit, list: LazyListState) {
    LazyColumn(Modifier.fillMaxSize().testTag("lan-device-list"), state = list,
        contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(state.query, model::search, R.string.lan_search, "lan-search", icon = Icons.Outlined.Search)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onRequestNearby, enabled = state.outcome(LanOperation.DISCOVERY).phase != LanPhase.RUNNING,
                        modifier = Modifier.weight(1f).testTag("lan-refresh")) { ActionLabel(text(R.string.lan_refresh_short), Icons.Outlined.Refresh) }
                    Button(onClick = model::add, enabled = state.canModify, modifier = Modifier.weight(1f).testTag("lan-add")) { ActionLabel(text(R.string.lan_add), Icons.Outlined.Add) }
                }
                var expanded by remember { mutableStateOf(false) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(text(R.string.lan_show, filterLabel(state.filter)), style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Outlined.ExpandMore, null)
                        }
                        DropdownMenu(expanded, { expanded = false }) {
                            LanFilter.entries.forEach { filter -> DropdownMenuItem(text = { Text(filterLabel(filter)) }, onClick = { model.filter(filter); expanded = false }) }
                        }
                    }
                    SourcePicker(state, model, Modifier, compact = true)
                }
                var help by rememberSaveable { mutableStateOf(false) }
                if (help) Text(text(R.string.lan_discovery_intro), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text(R.string.lan_discovery_help), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { help = !help }, modifier = Modifier.testTag("lan-discovery-help")) { Icon(Icons.Outlined.Info, text(R.string.lan_details)) }
                }
                if (state.outcome(LanOperation.DISCOVERY).phase == LanPhase.RUNNING || state.foundRows.isNotEmpty()) Outcome(state, LanOperation.DISCOVERY, model)
                if (state.found.partial) Notice(LanMessage(R.string.lan_partial))
                if (state.form != null) TextButton(onClick = model::add, modifier = Modifier.testTag("lan-resume")) { Text(text(R.string.lan_resume_draft)) }
            }
        }
        if (state.filter != LanFilter.DISCOVERED) {
            item { Text(text(R.string.lan_saved_devices, state.savedRows.size.toString()), style = MaterialTheme.typography.titleMedium) }
            if (state.savedRows.isEmpty()) item {
                SectionCard {
                    SectionHeading(text(if (state.query.isBlank() && state.devices.isEmpty()) R.string.lan_empty_saved_title else R.string.lan_empty_filtered), Icons.Outlined.Computer,
                        text(if (state.devices.isEmpty() && state.query.isBlank()) R.string.lan_no_saved_brief else R.string.lan_no_matches_brief))
                }
            }
            items(state.savedRows, key = { "saved:${it.id}" }) { device ->
                Card(onClick = { model.selectSaved(device.id) }, modifier = Modifier.fillMaxWidth().testTag("lan-saved-${device.id}")
                    .semantics { selected = state.selectedSaved == device.id },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconTile(Icons.Outlined.Computer)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(device.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(device.addressHint?.value ?: device.hostnameHint ?: text(R.string.lan_unknown), style = MaterialTheme.typography.bodyMedium)
                            CheckText(state.checks["saved:${device.id}"])
                        }
                        Icon(Icons.Outlined.ChevronRight, text(R.string.lan_details))
                    }
                }
            }
        }
        if (state.filter != LanFilter.SAVED) {
            item { Text(text(R.string.lan_found_devices, state.foundRows.size.toString()), style = MaterialTheme.typography.titleMedium) }
            if (state.foundRows.isEmpty()) item {
                val phase = state.outcome(LanOperation.DISCOVERY).phase
                val title = when {
                    state.query.isNotBlank() -> R.string.lan_empty_filtered
                    phase == LanPhase.RUNNING -> R.string.lan_discovered_filter
                    phase == LanPhase.CANCELLED -> R.string.lan_empty_cancelled
                    phase == LanPhase.FAILURE -> R.string.lan_empty_failed
                    phase == LanPhase.IDLE -> R.string.lan_not_searched
                    else -> R.string.lan_empty_found_title
                }
                SectionCard {
                    SectionHeading(text(title), Icons.Outlined.Radar,
                        text(if (state.query.isNotBlank()) R.string.lan_no_matches_brief else R.string.lan_empty_found_hint))
                    if (phase == LanPhase.FAILURE || phase == LanPhase.CANCELLED) state.outcome(LanOperation.DISCOVERY).message?.let { Notice(it, phase == LanPhase.FAILURE) }
                }
            }
            items(state.foundRows, key = { "found:${it.id}" }) { device ->
                Card(onClick = { model.selectFound(device.id) }, modifier = Modifier.fillMaxWidth().testTag("lan-found-${device.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconTile(Icons.Outlined.Radar)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(device.displayName ?: text(R.string.lan_unknown), style = MaterialTheme.typography.titleMedium)
                            Text(device.addresses.joinToString { it.value }, style = MaterialTheme.typography.bodyMedium)
                            Text(text(if (device.stale) R.string.lan_historical else R.string.lan_observed), style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Outlined.ChevronRight, text(R.string.lan_details))
                    }
                }
            }
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
    Text(if (check == null) text(R.string.lan_not_checked) else text(R.string.lan_check_history, localCheckTime(check.at)), style = MaterialTheme.typography.bodySmall)
    if (check != null) Text(phaseLabel(check.outcome.phase), style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun LanDeviceDetails(state: LanToolsState, model: LanToolsViewModel) {
    val saved = state.devices.firstOrNull { it.id == state.selectedSaved }
    val found = state.found.devices.firstOrNull { it.id == state.selectedFound }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("lan-device-details"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            saved != null -> {
                val check = state.checks["saved:${saved.id}"]
                SectionCard {
                    SectionHeading(saved.displayName, Icons.Outlined.Computer, text(R.string.lan_saved_filter))
                    Text(saved.addressHint?.value ?: text(R.string.lan_unknown))
                    Text(text(R.string.lan_hostname_value, saved.hostnameHint ?: text(R.string.lan_unknown)), style = MaterialTheme.typography.bodyMedium)
                    Text(saved.wake?.mac?.value ?: text(R.string.lan_wol_not_configured), style = MaterialTheme.typography.bodyMedium)
                    if (check == null) Text(text(R.string.lan_not_checked), style = MaterialTheme.typography.bodySmall)
                }
                if (check != null) {
                    Text(text(R.string.lan_check_history, localCheckTime(check.at)), style = MaterialTheme.typography.bodySmall)
                    OutcomeSummary(check.outcome)
                    if (check.outcome.phase == LanPhase.FAILURE) RecoveryNetwork(state, model)
                }
                Button(onClick = { model.diagnoseSaved(saved.id) }, enabled = saved.addressHint != null || saved.hostnameHint != null,
                    modifier = Modifier.fillMaxWidth().testTag("lan-device-diagnostics")) { ActionLabel(text(R.string.lan_run_diagnostics), Icons.Outlined.PlayArrow) }
                if (saved.wake != null) {
                    OutlinedButton(onClick = { model.wake(saved.id) }, enabled = state.outcome(LanOperation.WAKE).phase != LanPhase.RUNNING, modifier = Modifier.fillMaxWidth()) {
                        ActionLabel(text(R.string.lan_wake), Icons.Outlined.PowerSettingsNew)
                    }
                    Outcome(state, LanOperation.WAKE, model)
                }
                OutlinedButton(onClick = { model.edit(saved.id) }, enabled = state.canModify, modifier = Modifier.fillMaxWidth().testTag("lan-edit")) { ActionLabel(text(R.string.lan_edit), Icons.Outlined.Edit) }
                TextButton(onClick = { model.requestDelete(saved.id) }, enabled = state.canModify, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { ActionLabel(text(R.string.lan_delete), Icons.Outlined.DeleteOutline) }
            }
            found != null -> {
                SectionCard {
                    SectionHeading(found.displayName ?: text(R.string.lan_unknown), Icons.Outlined.Radar, text(R.string.lan_discovered_filter))
                    Text(found.addresses.joinToString { it.value })
                    Text(text(R.string.lan_hostname_value, found.hostname ?: text(R.string.lan_unknown)))
                    Text(text(R.string.lan_type_value, found.deviceType ?: text(R.string.lan_unknown)))
                    Text(text(if (found.stale) R.string.lan_historical else R.string.lan_observed), style = MaterialTheme.typography.bodySmall)
                    Text(text(R.string.lan_seen, localCheckTime(found.lastSeen)), style = MaterialTheme.typography.bodySmall)
                }
                state.checks["found:${found.id}"]?.let { OutcomeSummary(it.outcome) }
                SectionCard {
                    Text(text(R.string.lan_services), style = MaterialTheme.typography.titleMedium)
                    found.services.forEach { service ->
                        Text("${service.instance}: ${service.type}", style = MaterialTheme.typography.bodySmall)
                        service.addresses.forEach { address -> TextButton(onClick = { model.diagnoseFound(found.id, address.value, service.port) }) {
                            Text(text(R.string.lan_endpoint_diagnostics, address.value, service.port.toString()))
                        } }
                    }
                    var details by rememberSaveable { mutableStateOf(false) }
                    Disclosure(text(R.string.lan_details), details, { details = !details })
                    if (details) {
                        Text(text(R.string.lan_source, found.sources.joinToString()))
                        Text(found.scope)
                    }
                }
                Button(onClick = { model.saveFound(found.id) }, enabled = state.canModify, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.lan_save_new)) }
                Text(text(R.string.lan_review_copy), style = MaterialTheme.typography.bodySmall)
            }
            else -> Text(text(R.string.lan_record_gone))
        }
        Spacer(Modifier.height(8.dp))
    }
}
