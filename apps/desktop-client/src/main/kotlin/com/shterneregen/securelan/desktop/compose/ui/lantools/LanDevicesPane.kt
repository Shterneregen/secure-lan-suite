package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.LanToolsUiState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController
import com.shterneregen.securelan.lantools.model.SavedLanDevice

/** Saved-only device list. Discovery and diagnostics controls are intentionally absent until available. */
@Composable
internal fun LanDevicesPane(state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier = Modifier) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Saved devices (${state.devices.size})", style = MaterialTheme.typography.subtitle1)
            CompactButton(onClick = controller::beginAdd, enabled = state.canModify && state.editor == null) { Text("Add device") }
        }
        if (state.saving) { Text("Saving devices…"); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.deletingId?.let { id ->
            val name = state.devices.firstOrNull { it.id == id }?.displayName.orEmpty()
            Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
                Column(Modifier.padding(tokens.spacing.sm)) {
                    Text("Delete saved device “$name”? This removes only its saved settings.")
                    Row {
                        TextButton(onClick = controller::confirmDelete, enabled = !state.saving) { Text("Confirm delete") }
                        TextButton(onClick = controller::cancelDelete, enabled = !state.saving) { Text("Keep device") }
                    }
                }
            }
        }
        val draft = state.editor
        if (draft != null) {
            LanDeviceEditor(state, controller, Modifier.weight(1f))
        } else if (state.devices.isEmpty()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Text(if (state.loading) "Loading saved devices…" else "No saved devices yet.")
                Text("Add a device by name and configure its MAC address to use Wake-on-LAN. A secure room connection is not required.")
                Text("Automatic device discovery is not available yet.")
            }
        } else {
            BoxWithConstraints(Modifier.weight(1f)) {
                if (maxWidth >= 760.dp) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                        DeviceList(state, controller, Modifier.weight(1f))
                        DeviceDetails(state, controller, Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        // One scrollable surface on narrow windows avoids squeezing two columns.
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                            if (state.selected != null) item { DeviceDetails(state, controller, Modifier.fillMaxWidth(), scroll = false) }
                            items(state.devices, key = { it.id }) { DeviceRow(it, state, controller) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceList(state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier) {
    val tokens = LocalSecureLanDesignTokens.current
    LazyColumn(modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        items(state.devices, key = { it.id }) { DeviceRow(it, state, controller) }
    }
}

@Composable
private fun DeviceRow(device: SavedLanDevice, state: LanToolsUiState, controller: DesktopLanToolsController) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(Modifier.fillMaxWidth().semantics { selected = device.id == state.selectedId }, elevation = 0.dp,
        border = BorderStroke(1.dp, if (device.id == state.selectedId) MaterialTheme.colors.primary else tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            Text(device.displayName, style = MaterialTheme.typography.subtitle1)
            Text(device.addressHint?.value ?: "IP address unknown")
            Text(state.lastResults[device.id] ?: "No connectivity check performed", style = MaterialTheme.typography.caption)
            Row {
                TextButton(onClick = { controller.selectDevice(device.id) }) { Text("Details") }
                TextButton(onClick = { controller.wakeDevice(device.id) },
                    enabled = device.wake != null && state.wakingId == null && !state.saving && !state.networkLoading,
                    modifier = Modifier.testTag("wake-row-${device.id}")) { Text("Wake up") }
            }
        }
    }
}

@Composable
private fun DeviceDetails(state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier, scroll: Boolean = true) {
    val tokens = LocalSecureLanDesignTokens.current
    val selected = state.selected
    Column(if (scroll) modifier.verticalScroll(rememberScrollState()) else modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        if (selected == null) {
            Text("Choose a device to view its details and Wake-on-LAN settings.")
        } else {
            Text(selected.displayName, style = MaterialTheme.typography.h6)
            Text("IP: ${selected.addressHint?.value ?: "Unknown"}")
            Text("MAC: ${selected.wake?.mac?.value ?: "Not configured"}")
            Text("Broadcast: ${selected.wake?.broadcast?.value ?: "Selected subnet"}")
            Text("Preferred interface: ${selected.wake?.preferredInterfaceKey ?: "None"}")
            Row {
                TextButton(onClick = { controller.beginEdit(selected.id) }, enabled = state.canModify && state.wakingId == null) { Text("Edit device") }
                TextButton(onClick = { controller.requestDelete(selected.id) }, enabled = state.canModify && state.wakingId == null) { Text("Delete device") }
            }
            if (selected.wake != null) {
                InterfacePicker(state, controller)
                CompactButton(onClick = { controller.wakeDevice(selected.id) }, enabled = state.wakingId == null && !state.networkLoading && !state.saving,
                    modifier = Modifier.testTag("wake-details")) { Text(if (state.wakingId == selected.id) "Sending…" else "Wake up") }
            } else Text("Edit this device and add a MAC address to enable Wake-on-LAN.")
            state.lastResults[selected.id]?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Text("Sending a packet does not confirm that the device turned on.", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun InterfacePicker(state: LanToolsUiState, controller: DesktopLanToolsController) {
    var expanded by remember { mutableStateOf(false) }
    Text("Network interface", style = MaterialTheme.typography.subtitle2)
    val selected = state.networks.interfaces.firstOrNull { it.binding.key == state.bindingKey }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = !state.networkLoading && state.wakingId == null) {
            Text(selected?.let { "${it.displayName} · ${it.binding.address.value}" } ?: "Automatic / saved preference")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = { controller.selectBinding(null); expanded = false }) { Text("Automatic / saved preference") }
            state.networks.interfaces.forEach { entry ->
                val usable = entry.binding.up && entry.binding.kind == NetworkKind.LAN && entry.binding.address.isIpv4 &&
                    entry.binding.prefixLength in 1..30 && entry.broadcastCapable
                val hint = if (entry.binding.interfaceKey == state.networks.defaultRouteInterfaceKey) " · Recommended" else ""
                DropdownMenuItem(onClick = { controller.selectBinding(entry.binding.key); expanded = false }, enabled = usable) {
                    Text("${entry.displayName} · ${entry.binding.address.value}$hint${if (usable) "" else " · Unavailable"}")
                }
            }
        }
    }
    TextButton(onClick = controller::refreshInterfaces, enabled = !state.networkLoading && state.wakingId == null) { Text("Refresh interfaces") }
    if (state.networkLoading) Text("Reading interfaces…")
    state.networkError?.let { Text(it, color = MaterialTheme.colors.error) }
    if (!state.networkLoading && state.networks.interfaces.isEmpty()) Text("No local IPv4 interfaces available.")
    Text("If several interfaces are available, choose one explicitly. A missing saved preference will not switch networks automatically.", style = MaterialTheme.typography.caption)
}
