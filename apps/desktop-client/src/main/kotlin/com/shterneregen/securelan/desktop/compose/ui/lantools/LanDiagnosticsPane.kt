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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.lantools.DesktopDiagnosticsController
import com.shterneregen.securelan.lantools.diagnostics.AddressFamily
import com.shterneregen.securelan.lantools.diagnostics.MetadataAvailability

/** A focused scrollable tool, not a replacement for the main messenger workspace. */
@Composable
internal fun LanDiagnosticsPane(controller: DesktopDiagnosticsController, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()
    val input = state.inputs
    val tokens = LocalSecureLanDesignTokens.current
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("diagnostics-pane"),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Text("Network information", style = MaterialTheme.typography.h6)
        OperationButton("Refresh network information", DiagnosticKind.NETWORK, state, controller, controller::refresh)
        Outcome(state.outcome(DiagnosticKind.NETWORK))
        state.network?.interfaces?.forEach { entry ->
            val binding = entry.source.binding
            Card(Modifier.fillMaxWidth(), elevation = 0.dp, border = BorderStroke(1.dp, tokens.colors.borderSubtle)) {
                Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    Text("${entry.source.displayName} (${binding.interfaceKey})", style = MaterialTheme.typography.subtitle2)
                    Text("IPv4: ${binding.address.value}/${binding.prefixLength} · ${if (binding.up) "Up" else "Down"} · ${binding.kind}")
                    if (entry.metadata == MetadataAvailability.AVAILABLE) {
                        Text("Gateway: ${entry.gateways.joinToString { it.value }.ifEmpty { "None reported" }}")
                        Text("DNS servers: ${entry.dnsServers.joinToString().ifEmpty { "None reported" }}")
                    } else Text("Gateway / DNS: ${metadataLabel(entry.metadata)}")
                }
            }
        }
        if (state.network?.interfaces?.isEmpty() == true) Text("No local IPv4 interfaces reported.")
        Divider()
        Text("Check settings", style = MaterialTheme.typography.subtitle1)
        OutlinedTextField(input.timeoutMillis, { value -> controller.edit { it.copy(timeoutMillis = value) } },
            label = { Text("Timeout (ms, 1–30000)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("diag-timeout"))
        SourcePicker(state, controller)
        Text("TCP connectivity", style = MaterialTheme.typography.h6)
        OutlinedTextField(input.tcpHost, { value -> controller.edit { it.copy(tcpHost = value) } }, label = { Text("TCP hostname or IP") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("diag-tcp-host"))
        OutlinedTextField(input.tcpPort, { value -> controller.edit { it.copy(tcpPort = value) } }, label = { Text("TCP port") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("diag-tcp-port"))
        OperationButton("Check TCP", DiagnosticKind.TCP, state, controller, controller::checkTcp)
        Outcome(state.outcome(DiagnosticKind.TCP))
        Text("A refused or timed-out port does not prove a device is offline. A hostname check uses the first suitable resolved address, not every address.", style = MaterialTheme.typography.caption)
        Divider()
        Text("DNS lookup", style = MaterialTheme.typography.h6)
        Text("Uses the system resolver, its cache and default routing. The TCP source selection does not bind DNS lookup to that interface.")
        OutlinedTextField(input.dnsHost, { value -> controller.edit { it.copy(dnsHost = value) } }, label = { Text("DNS hostname or IP") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("diag-dns-host"))
        var familyExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { familyExpanded = true }) { Text("Address filter: ${input.family}") }
            DropdownMenu(familyExpanded, { familyExpanded = false }) {
                AddressFamily.entries.forEach { family -> DropdownMenuItem(onClick = {
                    controller.edit { it.copy(family = family) }; familyExpanded = false
                }) { Text(family.name) } }
            }
        }
        OperationButton("Look up DNS", DiagnosticKind.DNS, state, controller, controller::lookupDns)
        Outcome(state.outcome(DiagnosticKind.DNS))
        Divider()
        Text("Gateway TCP check", style = MaterialTheme.typography.h6)
        Text("Choose a source interface and its reported gateway. Enter one TCP port to check; no common ports are scanned automatically.")
        val selected = state.selectedInterface
        val gateways = selected?.gateways.orEmpty()
        if (selected?.metadata == MetadataAvailability.AVAILABLE && gateways.isNotEmpty()) {
            var gatewaysExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { gatewaysExpanded = true }) { Text(input.gateway?.let { "Gateway: $it" } ?: "Choose gateway") }
                DropdownMenu(gatewaysExpanded, { gatewaysExpanded = false }) {
                    gateways.forEach { gateway -> DropdownMenuItem(onClick = {
                        controller.edit { it.copy(gateway = gateway.value) }; gatewaysExpanded = false
                    }) { Text(gateway.value) } }
                }
            }
            OutlinedTextField(input.gatewayPort, { value -> controller.edit { it.copy(gatewayPort = value) } }, label = { Text("Gateway TCP port") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("diag-gateway-port"))
            OperationButton("Check gateway TCP", DiagnosticKind.GATEWAY, state, controller, controller::checkGateway)
        } else Text(if (selected == null) "Select an available source interface to view gateway options."
            else "No gateway check is available: ${if (selected.metadata == MetadataAvailability.AVAILABLE) "no IPv4 gateway reported" else metadataLabel(selected.metadata)}.")
        Outcome(state.outcome(DiagnosticKind.GATEWAY))
        Divider()
        Text("ICMP ping: unavailable on this platform. TCP connection time is not ICMP RTT.", style = MaterialTheme.typography.caption)
    }
}

@Composable
private fun SourcePicker(state: LanDiagnosticsUiState, controller: DesktopDiagnosticsController) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.selectedInterface
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("diag-source")) {
            Text(selected?.source?.let { "TCP source: ${it.binding.interfaceKey} · ${it.binding.address.value}" }
                ?: if (state.inputs.bindingKey == null) "TCP source: OS routing" else "TCP source: selected interface unavailable")
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(onClick = { controller.selectInterface(null); expanded = false }) { Text("Use OS routing") }
            state.network?.interfaces?.forEach { entry ->
                DropdownMenuItem(onClick = { controller.selectInterface(entry.source.binding.key); expanded = false }, enabled = entry.source.binding.up) {
                    Text("${entry.source.binding.interfaceKey} · ${entry.source.binding.address.value}")
                }
            }
        }
    }
}

@Composable
private fun OperationButton(label: String, kind: DiagnosticKind, state: LanDiagnosticsUiState, controller: DesktopDiagnosticsController, action: () -> Unit) {
    val running = state.outcome(kind).phase == DiagnosticPhase.RUNNING
    Row {
        CompactButton(onClick = action, enabled = !running) { Text(label) }
        if (running) TextButton(onClick = { controller.cancel(kind) }) { Text("Cancel ${if (kind == DiagnosticKind.NETWORK) "refresh" else kind.name.lowercase()}") }
    }
}

@Composable
private fun Outcome(value: DiagnosticOutcome) {
    if (value.phase == DiagnosticPhase.IDLE) return
    if (value.phase == DiagnosticPhase.RUNNING) LinearProgressIndicator(Modifier.fillMaxWidth())
    Text("${value.phase.name.lowercase().replaceFirstChar(Char::uppercase)}: ${value.message}",
        color = if (value.phase == DiagnosticPhase.FAILURE) MaterialTheme.colors.error else LocalContentColor.current,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
}

private fun metadataLabel(value: MetadataAvailability): String = when (value) {
    MetadataAvailability.AVAILABLE -> "Available"
    MetadataAvailability.UNSUPPORTED -> "Unsupported by this platform backend"
    MetadataAvailability.PERMISSION_DENIED -> "Permission denied"
    MetadataAvailability.TIMEOUT -> "OS query timed out; refresh to retry"
    MetadataAvailability.RESOURCE_LIMIT -> "OS metadata limit reached"
    MetadataAvailability.UNAVAILABLE -> "Unknown / unavailable; refresh to retry"
}
