package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import com.shterneregen.securelan.androidclient.ui.SectionCard
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Draft lives in the ViewModel; Back suspends it and Cancel explicitly discards it. */
@Composable
internal fun LanDeviceEditor(state: LanToolsState, model: LanToolsViewModel) {
    val form = state.form ?: return
    val focus = remember { FocusRequester() }
    var advanced by rememberSaveable(form.id.toString()) { mutableStateOf(false) }
    var manualBinding by rememberSaveable(form.id.toString()) { mutableStateOf(false) }
    val error = state.formError?.resource
    LaunchedEffect(form.id) { focus.requestFocus() }
    LaunchedEffect(error) {
        if (error in setOf(R.string.lan_invalid_broadcast, R.string.lan_invalid_port, R.string.lan_invalid_device)) advanced = true
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard {
                Text(text(R.string.lan_basics), style = MaterialTheme.typography.titleMedium)
                FormField(state, form.name, R.string.lan_name, "lan-name", setOf(R.string.lan_invalid_name), Modifier.focusRequester(focus)) {
                    model.changeForm(form.copy(name = it))
                }
                FormField(state, form.address, R.string.lan_address, "lan-address", setOf(R.string.lan_invalid_address)) { model.changeForm(form.copy(address = it)) }
                FormField(state, form.hostname, R.string.lan_hostname, "lan-hostname", setOf(R.string.lan_invalid_hostname)) { model.changeForm(form.copy(hostname = it)) }
            }
            SectionCard {
                Text(text(R.string.lan_wol), style = MaterialTheme.typography.titleMedium)
                FormField(state, form.mac, R.string.lan_mac, "lan-mac", setOf(R.string.lan_invalid_mac)) { model.changeForm(form.copy(mac = it)) }
                Text(text(R.string.lan_mac_requirement), style = MaterialTheme.typography.bodySmall)
                Disclosure(text(R.string.lan_wol_advanced), advanced, { advanced = !advanced }, "lan-wol-advanced")
                if (advanced) {
                    FormField(state, form.broadcast, R.string.lan_broadcast, "lan-broadcast", setOf(R.string.lan_invalid_broadcast)) { model.changeForm(form.copy(broadcast = it)) }
                    FormField(state, form.port, R.string.lan_udp_port, "lan-udp-port", setOf(R.string.lan_invalid_port)) { model.changeForm(form.copy(port = it)) }
                    var picker by remember { mutableStateOf(false) }
                    val selected = state.network?.interfaces?.firstOrNull { it.source.binding.interfaceKey == form.preferred }?.source
                    Box {
                        OutlinedButton(onClick = { picker = true }, enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("lan-wol-source")) {
                            Text(selected?.let { "${it.displayName}: ${it.binding.address.value}" }
                                ?: if (form.preferred.isBlank()) text(R.string.lan_default_binding) else text(R.string.lan_stale_binding), modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ExpandMore, null)
                        }
                        DropdownMenu(picker, { picker = false }) {
                            DropdownMenuItem(text = { Text(text(R.string.lan_default_binding)) }, onClick = { model.changeForm(form.copy(preferred = "")); picker = false })
                            state.network?.interfaces.orEmpty().distinctBy { it.source.binding.interfaceKey }.forEach { entry ->
                                DropdownMenuItem(text = { Text("${entry.source.displayName}: ${entry.source.binding.address.value}") }, enabled = entry.source.binding.up,
                                    onClick = { model.changeForm(form.copy(preferred = entry.source.binding.interfaceKey)); picker = false })
                            }
                            DropdownMenuItem(text = { Text(text(R.string.lan_manual_binding)) }, onClick = { manualBinding = true; picker = false })
                        }
                    }
                    if (manualBinding || form.preferred.isNotEmpty() && selected == null) {
                        FormField(state, form.preferred, R.string.lan_preferred, "lan-preferred", setOf(R.string.lan_invalid_device)) { model.changeForm(form.copy(preferred = it)) }
                    }
                    Text(text(R.string.lan_mac_help), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        // Keep a short validation summary visible even if its input is currently behind the IME.
        state.formError?.let { Notice(it, error = true) }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = model::cancelForm, enabled = !state.saving, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(text(R.string.lan_cancel)) }
            Button(onClick = model::save, enabled = state.canModify, modifier = Modifier.weight(1.25f).heightIn(min = 48.dp).testTag("lan-save")) {
                Text(text(R.string.lan_save_short))
            }
        }
    }
}

@Composable
private fun FormField(state: LanToolsState, value: String, label: Int, tag: String, invalid: Set<Int>,
    modifier: Modifier = Modifier, change: (String) -> Unit) {
    Field(value, change, label, tag, enabled = !state.saving, inputModifier = modifier,
        error = state.formError?.takeIf { it.resource in invalid })
}
