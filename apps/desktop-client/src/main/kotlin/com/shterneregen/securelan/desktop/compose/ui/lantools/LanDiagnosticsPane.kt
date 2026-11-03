package com.shterneregen.securelan.desktop.compose.ui.lantools

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
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.lantools.DesktopDiagnosticsController
import com.shterneregen.securelan.lantools.diagnostics.AddressFamily
import com.shterneregen.securelan.lantools.diagnostics.DiagnosticInterface
import com.shterneregen.securelan.lantools.diagnostics.MetadataAvailability

/** One scroll surface; only presentation order changes, never interface selection or routing. */
@Composable
internal fun LanDiagnosticsPane(controller: DesktopDiagnosticsController, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()
    val tokens = LocalSecureLanDesignTokens.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wide = maxWidth >= 760.dp
        Column(Modifier.widthIn(max = 1120.dp).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("diagnostics-pane"),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            LanToolCard("Network overview") {
                OperationButton("Refresh network information", DiagnosticKind.NETWORK, state, controller, controller::refresh)
                Outcome(state.outcome(DiagnosticKind.NETWORK))
                val entries = state.network?.interfaces.orEmpty()
                val primary = state.selectedInterface ?: entries.firstOrNull { it.source.binding.up && it.source.binding.kind == NetworkKind.LAN }
                    ?: entries.firstOrNull()
                primary?.let { NetworkSummary(it) }
                val others = entries.filter { it != primary }
                if (others.isNotEmpty()) LanDisclosure("Other interfaces (${others.size})") {
                    others.forEach { NetworkSummary(it) }
                }
                if (state.network != null && entries.isEmpty()) Text("No local IPv4 interfaces reported.")
            }
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                LanToolCard("TCP connection", Modifier.weight(1f)) { TcpForm(state, controller) }
                LanToolCard("DNS lookup", Modifier.weight(1f)) { DnsForm(state, controller) }
            } else {
                LanToolCard("TCP connection") { TcpForm(state, controller) }
                LanToolCard("DNS lookup") { DnsForm(state, controller) }
            }
            LanToolCard("Gateway TCP check") {
                SourcePicker(state, controller)
                GatewayForm(state, controller)
            }
            LanDisclosure("Advanced settings") {
                OutlinedTextField(state.inputs.timeoutMillis, { value -> controller.edit { it.copy(timeoutMillis = value) } },
                    label = { Text("Timeout (ms, 1-30000)") }, singleLine = true,
                    modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().testTag("diag-timeout"))
                Text("The source selector above applies to TCP and gateway checks. DNS uses system routing.", style = MaterialTheme.typography.caption)
                Text("ICMP ping: unavailable on this platform. TCP connection time is not ICMP RTT.", style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun NetworkSummary(entry: DiagnosticInterface) {
    val binding = entry.source.binding
    Text("${entry.source.displayName} (${binding.interfaceKey})", style = MaterialTheme.typography.subtitle2)
    Text("IPv4: ${binding.address.value}/${binding.prefixLength} - ${if (binding.up) "Up" else "Down"} - ${binding.kind}", style = MaterialTheme.typography.body2)
    if (entry.metadata == MetadataAvailability.AVAILABLE) {
        Text("Gateway: ${entry.gateways.joinToString { it.value }.ifEmpty { "None reported" }}", style = MaterialTheme.typography.body2)
        Text("DNS servers: ${entry.dnsServers.joinToString().ifEmpty { "None reported" }}", style = MaterialTheme.typography.body2)
    } else Text("Gateway / DNS: ${metadataLabel(entry.metadata)}", style = MaterialTheme.typography.body2)
}

@Composable
private fun TcpForm(state: LanDiagnosticsUiState, controller: DesktopDiagnosticsController) {
    val input = state.inputs
    val tokens = LocalSecureLanDesignTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        OutlinedTextField(input.tcpHost, { value -> controller.edit { it.copy(tcpHost = value) } }, label = { Text("TCP hostname or IP") },
            singleLine = true, modifier = Modifier.weight(1f).testTag("diag-tcp-host"))
        OutlinedTextField(input.tcpPort, { value -> controller.edit { it.copy(tcpPort = value) } }, label = { Text("TCP port") },
            singleLine = true, modifier = Modifier.width(100.dp).testTag("diag-tcp-port"))
    }
    OperationButton("Check TCP", DiagnosticKind.TCP, state, controller, controller::checkTcp)
    Outcome(state.outcome(DiagnosticKind.TCP))
    LanDisclosure("About TCP results") { Text("A refused or timed-out port does not prove a device is offline. A hostname check uses the first suitable resolved address, not every address.", style = MaterialTheme.typography.caption) }
}

@Composable
private fun DnsForm(state: LanDiagnosticsUiState, controller: DesktopDiagnosticsController) {
    val input = state.inputs
    Text("System resolver and default routing", style = MaterialTheme.typography.caption)
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
}

@Composable
private fun GatewayForm(state: LanDiagnosticsUiState, controller: DesktopDiagnosticsController) {
    val input = state.inputs
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
            singleLine = true, modifier = Modifier.widthIn(max = 220.dp).fillMaxWidth().testTag("diag-gateway-port"))
        OperationButton("Check gateway TCP", DiagnosticKind.GATEWAY, state, controller, controller::checkGateway)
    } else Text(if (selected == null) "Select an available source interface to view gateway options."
        else "No gateway check is available: ${if (selected.metadata == MetadataAvailability.AVAILABLE) "no IPv4 gateway reported" else metadataLabel(selected.metadata)}.")
    Outcome(state.outcome(DiagnosticKind.GATEWAY))
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
    FlowRow {
        CompactButton(onClick = action, enabled = !running) { Text(label) }
        if (running) TextButton(onClick = { controller.cancel(kind) }) { Text("Cancel ${if (kind == DiagnosticKind.NETWORK) "refresh" else kind.name.lowercase()}") }
    }
}

@Composable
private fun Outcome(value: DiagnosticOutcome) {
    if (value.phase == DiagnosticPhase.IDLE) return
    if (value.phase == DiagnosticPhase.RUNNING) LinearProgressIndicator(Modifier.fillMaxWidth())
    Text(value.message,
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
