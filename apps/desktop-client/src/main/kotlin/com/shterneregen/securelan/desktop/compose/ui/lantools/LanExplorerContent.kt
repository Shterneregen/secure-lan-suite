package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.lantools.DesktopExplorerController
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController
import com.shterneregen.securelan.lantools.discovery.DiscoveredLanDevice

/** Explicit local browse controls. No effect or automatic discovery is launched from composition. */
@Composable
internal fun LanExplorerControls(state: LanExplorerUiState, controller: DesktopExplorerController, onAdd: () -> Unit, canAdd: Boolean) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            OutlinedTextField(state.query, controller::query, placeholder = { Text("Search devices or services") }, singleLine = true,
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().testTag("lan-search").semantics {
                    contentDescription = "Search devices or services"
                })
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text("Show: ${state.filter.name.lowercase().replaceFirstChar(Char::uppercase)}") }
                DropdownMenu(expanded, { expanded = false }) {
                    LanDeviceFilter.entries.forEach { filter -> DropdownMenuItem(onClick = {
                        controller.filter(filter); expanded = false
                    }) { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) } }
                }
            }
            if (state.running) OutlinedButton(onClick = controller::cancel) { Text("Cancel discovery") }
            else OutlinedButton(onClick = controller::refresh) { Text("Refresh devices") }
            Button(onClick = onAdd, enabled = canAdd) { Text("Add device") }
        }
        if (state.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.phase != DiagnosticPhase.IDLE) Text(if (state.running) "Searching advertised services..." else state.message, style = MaterialTheme.typography.caption,
            color = if (state.phase == DiagnosticPhase.FAILURE) MaterialTheme.colors.error else LocalContentColor.current,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (state.snapshot.partial) Text("Partial results: interface, metadata or event limits were reached, or a network source failed.", style = MaterialTheme.typography.caption)
    }
}

@Composable
internal fun DiscoveredDeviceRow(device: DiscoveredLanDevice, state: LanExplorerUiState, controller: DesktopLanToolsController, check: DeviceTcpCheck?) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(Modifier.fillMaxWidth().testTag("found-row-${device.id}").semantics { selected = device.id == state.selectedId }, elevation = 0.dp,
        border = BorderStroke(1.dp, if (device.id == state.selectedId) MaterialTheme.colors.primary else tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            Text(device.displayName ?: "Unknown", style = MaterialTheme.typography.subtitle1)
            Text(device.addresses.joinToString { it.value }.ifEmpty { "Unknown" })
            Text("Discovered · ${if (device.stale) "Historical snapshot" else "Observed during this browse"}", style = MaterialTheme.typography.caption)
            Text(device.services.joinToString { it.type }, style = MaterialTheme.typography.caption)
            DeviceCheckText(check)
            FlowRow {
                TextButton(onClick = { controller.explorer?.select(device.id) }) { Text("Details") }
                val service = device.services.firstOrNull()
                if (service != null && controller.diagnostics != null) TextButton(onClick = {
                    controller.diagnoseDiscovered(device.id, service.addresses.first().value, service.port)
                }) { Text("Diagnostics") }
            }
        }
    }
}

/** Details retain exact service endpoints and provenance; no guessed manufacturer, MAC or hardware type. */
@Composable
internal fun DiscoveredDeviceDetails(state: LanExplorerUiState, controller: DesktopLanToolsController, check: DeviceTcpCheck?, modifier: Modifier, scroll: Boolean = true) {
    val device = state.selected
    val tokens = LocalSecureLanDesignTokens.current
    Column(if (scroll) modifier.verticalScroll(rememberScrollState()) else modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        if (device == null) {
            Text("This discovery record is no longer in the current snapshot. Refresh or choose another device; saved settings are unchanged.")
        } else {
            Text(device.displayName ?: "Unknown", style = MaterialTheme.typography.h6)
            Text("Hostname: ${device.hostname ?: "Unknown"}")
            Text("Type: ${device.deviceType ?: "Unknown"}")
            Text("Addresses: ${device.addresses.joinToString { it.value }}")
            Text("Source: ${device.sources.joinToString()}")
            Text("Network binding: ${device.scope}")
            Text("Last observed: ${device.lastSeen}")
            Text(if (device.stale) "Historical snapshot; presence has not been checked again." else "Service observed; this is not proof of device reachability or identity.")
            DeviceCheckText(check)
            Text("Advertised services", style = MaterialTheme.typography.subtitle1)
            device.services.forEach { service ->
                Text("${service.instance} · ${service.type} · port ${service.port}")
                service.addresses.forEach { address ->
                    if (controller.diagnostics != null) TextButton(onClick = {
                        controller.diagnoseDiscovered(device.id, address.value, service.port)
                    }) { Text("Diagnostics ${address.value}:${service.port}") }
                }
            }
            CompactButton(onClick = { controller.beginSaveDiscovered(device.id) }, enabled = controller.state.value.canModify) { Text("Save as new device") }
            Text("Review the copied name/address before saving. No MAC or Wake-on-LAN configuration is inferred. Saved and discovered records are kept separate; addresses are not trusted identities.", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
internal fun DeviceCheckText(check: DeviceTcpCheck?) {
    Text(check?.let { "Last TCP check (${it.checkedAt}): ${it.outcome.message}" } ?: "No connectivity check performed", style = MaterialTheme.typography.caption)
}
