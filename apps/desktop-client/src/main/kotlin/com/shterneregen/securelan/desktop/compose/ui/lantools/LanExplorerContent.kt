package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.lantools.DesktopExplorerController
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController
import com.shterneregen.securelan.lantools.discovery.DiscoveredLanDevice
import com.shterneregen.securelan.lantools.discovery.LanAdvertisedService
import com.shterneregen.securelan.lantools.model.LanNetworkSnapshot

/** Explicit local browse controls. No effect or automatic discovery is launched from composition. */
@Composable
internal fun LanExplorerControls(state: LanExplorerUiState, controller: DesktopExplorerController, onAdd: () -> Unit, canAdd: Boolean) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        val search: @Composable (Modifier) -> Unit = { fieldModifier ->
            CompactTextField(state.query, controller::query, outlined = true, label = "", placeholder = "Search devices or services",
                modifier = fieldModifier,
                inputModifier = Modifier.testTag("lan-search").semantics { contentDescription = "Search devices or services" },
                leadingContent = { Icon(SecureLanIcons.Search, contentDescription = null, modifier = Modifier.size(18.dp)) })
        }
        val actions: @Composable () -> Unit = {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Icon(SecureLanIcons.Filter, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(tokens.spacing.xxs))
                    Text("Show: ${state.filter.name.lowercase().replaceFirstChar(Char::uppercase)}")
                    Spacer(Modifier.width(tokens.spacing.xxs))
                    Icon(SecureLanIcons.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded, { expanded = false }) {
                    LanDeviceFilter.entries.forEach { filter -> DropdownMenuItem(onClick = {
                        controller.filter(filter); expanded = false
                    }) { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) } }
                }
            }
            if (state.running) OutlinedButton(onClick = controller::cancel) { Text("Cancel discovery") }
            else OutlinedButton(onClick = controller::refresh) {
                Icon(SecureLanIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(tokens.spacing.xxs))
                Text("Refresh devices")
            }
            Button(onClick = onAdd, enabled = canAdd) {
                Icon(SecureLanIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(tokens.spacing.xxs))
                Text("Add device")
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 680.dp) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                search(Modifier.weight(1f))
                actions()
            } else FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                search(Modifier.fillMaxWidth())
                actions()
            }
        }
        if (state.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.phase != DiagnosticPhase.IDLE) {
            Text(if (state.running) "Searching advertised services..." else state.message, style = MaterialTheme.typography.caption,
                color = if (state.phase == DiagnosticPhase.FAILURE) MaterialTheme.colors.error else tokens.colors.textSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("lan-explorer-status"))
        }
        if (state.snapshot.partial) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs), verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("lan-explorer-partial")) {
                Icon(SecureLanIcons.Information, contentDescription = null, tint = tokens.colors.warning, modifier = Modifier.size(16.dp))
                Text("Partial results; some sources did not finish.", style = MaterialTheme.typography.caption,
                    color = tokens.colors.warning)
            }
        }
    }
}

@Composable
internal fun DiscoveredDeviceRow(device: DiscoveredLanDevice, state: LanExplorerUiState, controller: DesktopLanToolsController, check: DeviceTcpCheck?) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(Modifier.fillMaxWidth().testTag("found-row-${device.id}")
        .clickable(enabled = controller.explorer != null, role = Role.Button) { controller.explorer?.select(device.id) }
        .semantics { selected = device.id == state.selectedId }, elevation = 0.dp,
        border = BorderStroke(1.dp, if (device.id == state.selectedId) MaterialTheme.colors.primary else tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            Text(device.displayName ?: "Unknown", style = MaterialTheme.typography.subtitle1)
            Text(device.addresses.joinToString(" · ") { it.value }.ifEmpty { "Address unknown" })
            Text(if (device.stale) "Historical · not online status" else "Observed · not a reachability check",
                style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
            if (device.services.isNotEmpty()) {
                Text(device.services.joinToString(" · ") { formatLanServiceType(it.type) },
                    style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
            }
            if (check != null) DeviceCheckText(check)
        }
    }
}

/** Details retain exact service endpoints and provenance; no guessed manufacturer, MAC or hardware type. */
@Composable
internal fun DiscoveredDeviceDetails(
    state: LanExplorerUiState,
    controller: DesktopLanToolsController,
    check: DeviceTcpCheck?,
    networks: LanNetworkSnapshot,
    modifier: Modifier,
    scroll: Boolean = true,
) {
    val device = state.selected
    val tokens = LocalSecureLanDesignTokens.current
    if (device == null) {
        Text("This discovery record is no longer in the current snapshot. Refresh or choose another device; saved settings are unchanged.",
            modifier = modifier)
        return
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(tokens.radius.medium), color = tokens.colors.surfaceLevel1,
        border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
        if (scroll) {
            Column(Modifier.fillMaxSize().padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                DiscoveredDetailHeader(device, check, controller)
                Divider(color = tokens.colors.borderSubtle)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    DiscoveredDetailBody(device, controller, networks)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                DiscoveredDetailHeader(device, check, controller)
                Divider(color = tokens.colors.borderSubtle)
                DiscoveredDetailBody(device, controller, networks)
            }
        }
    }
}

@Composable
private fun DiscoveredDetailHeader(device: DiscoveredLanDevice, check: DeviceTcpCheck?, controller: DesktopLanToolsController) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                Text(device.displayName ?: "Unknown", style = MaterialTheme.typography.h6)
                Text(device.addresses.joinToString(" · ") { it.value }.ifEmpty { "Address unknown" },
                    style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
            }
            DiscoveryStateBadge(device.stale)
        }
        Button(onClick = { controller.beginSaveDiscovered(device.id) }, enabled = controller.state.value.canModify,
            modifier = Modifier.testTag("save-discovered-device")) {
            Icon(SecureLanIcons.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(tokens.spacing.xxs))
            Text("Save as new device")
        }
        DeviceCheckText(check)
        Text(if (device.stale) "Historical record; presence has not been checked again."
            else "Observed during this browse; this is not proof of reachability or identity.",
            style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
    }
}

@Composable
private fun DiscoveryStateBadge(stale: Boolean) {
    val tokens = LocalSecureLanDesignTokens.current
    val color = if (stale) tokens.colors.warning else tokens.colors.success
    Surface(shape = RoundedCornerShape(tokens.radius.pill), color = color.copy(alpha = 0.14f),
        border = BorderStroke(tokens.border.subtle, color.copy(alpha = 0.45f))) {
        Text(if (stale) "Historical" else "Observed", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.caption, color = tokens.colors.textPrimary)
    }
}

@Composable
private fun DiscoveredDetailBody(device: DiscoveredLanDevice, controller: DesktopLanToolsController, networks: LanNetworkSnapshot) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Text("Overview", style = MaterialTheme.typography.subtitle1)
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            LanDetailField("Hostname", device.hostname ?: "Unknown")
            LanDetailField("Type", device.deviceType ?: "Unknown")
            LanDetailField("Addresses", device.addresses.joinToString(" · ") { it.value }.ifEmpty { "Address unknown" })
            LanDetailField("Network", formatLanNetwork(device.scope, networks))
            LanDetailField("Last observed", formatLanObservedTime(device.lastSeen))
            LanDetailField("Source", device.sources.joinToString(" · ").ifEmpty { "Unknown" })
        }
        if (device.services.isNotEmpty()) {
            Text("Advertised services", style = MaterialTheme.typography.subtitle1)
            device.services.forEachIndexed { index, service ->
                if (index > 0) Divider(color = tokens.colors.borderSubtle)
                AdvertisedServiceRow(device, service, controller)
            }
        }
        LanDisclosure("About discovery data") {
            Text("Discovery shows services advertised on this network; it is not an online-status check. Names and addresses are not authenticated identities. No MAC or Wake-on-LAN settings are inferred, and saved and discovered records remain separate.",
                style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
        }
        LanDisclosure("Technical details") {
            LanDetailField("Scope", device.scope)
            LanDetailField("Last observed (UTC)", device.lastSeen.toString())
            if (device.services.isNotEmpty()) {
                LanDetailField("Service types", device.services.joinToString(" · ") { it.type })
            }
        }
    }
}

@Composable
private fun AdvertisedServiceRow(device: DiscoveredLanDevice, service: LanAdvertisedService, controller: DesktopLanToolsController) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        Text(formatLanServiceType(service.type), style = MaterialTheme.typography.subtitle2)
        val detail = buildList {
            add(service.type)
            if (service.instance.isNotBlank() && service.instance != device.displayName) add(service.instance)
        }.joinToString(" · ")
        Text(detail, style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
        service.addresses.forEach { address ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Text("${address.value}:${service.port}", style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                if (controller.diagnostics != null) {
                    CompactButton(onClick = { controller.diagnoseDiscovered(device.id, address.value, service.port) },
                        modifier = Modifier.testTag("check-service-${device.id}-${address.value}-${service.port}")) {
                        Text("Check")
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeviceCheckText(check: DeviceTcpCheck?) {
    val tokens = LocalSecureLanDesignTokens.current
    val text = check?.let { "Last TCP check: ${it.outcome.message} · ${formatLanObservedTime(it.checkedAt)}" } ?: "Not checked"
    Text(text, style = MaterialTheme.typography.caption, color = tokens.colors.textSecondary)
}
