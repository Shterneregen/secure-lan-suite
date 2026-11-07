package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
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
        val wide = maxWidth >= 680.dp
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("diagnostics-pane"),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            LanToolCard("Network overview", icon = SecureLanIcons.Network,
                headerAction = if (wide) {{ OperationButton("Refresh network information", DiagnosticKind.NETWORK,
                    state, controller, controller::refresh, prominent = false) }} else null,
                subtitle = "Local interface data, gateway and DNS metadata.") {
                if (!wide) OperationButton("Refresh network information", DiagnosticKind.NETWORK,
                    state, controller, controller::refresh, prominent = false)
                val networkOutcome = state.outcome(DiagnosticKind.NETWORK)
                val complete = state.network?.interfaces?.takeIf { it.isNotEmpty() }
                    ?.all { it.metadata == MetadataAvailability.AVAILABLE } == true
                if (networkOutcome.phase != DiagnosticPhase.SUCCESS || !complete) Outcome(networkOutcome)
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
            if (wide) Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                LanToolCard("TCP connection", Modifier.weight(1f).fillMaxHeight(), SecureLanIcons.Tcp, subtitle = "Test a TCP connection to a host and port.") { TcpForm(state, controller) }
                LanToolCard("DNS lookup", Modifier.weight(1f).fillMaxHeight(), SecureLanIcons.Dns, subtitle = "Resolve a hostname using system resolver and default routing.") { DnsForm(state, controller) }
            } else {
                LanToolCard("TCP connection", icon = SecureLanIcons.Tcp, subtitle = "Test a TCP connection to a host and port.") { TcpForm(state, controller) }
                LanToolCard("DNS lookup", icon = SecureLanIcons.Dns, subtitle = "Resolve a hostname using system resolver and default routing.") { DnsForm(state, controller) }
            }
            LanToolCard("Gateway TCP check", icon = SecureLanIcons.Gateway, subtitle = "Check TCP connectivity to the network gateway.") {
                if (wide && state.selectedInterface == null) Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg)) {
                    SourcePicker(state, controller)
                    Box(Modifier.weight(1f)) { GatewayForm(state, controller) }
                } else {
                    SourcePicker(state, controller)
                    GatewayForm(state, controller)
                }
            }
            LanToolCard("") { LanDisclosure("Advanced settings", SecureLanIcons.Settings) {
                val timeout: @Composable () -> Unit = {
                    Column(Modifier.widthIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                        Text("Timeout (ms, 1-30000)", style = MaterialTheme.typography.caption)
                        DiagnosticField(state.inputs.timeoutMillis, { value -> controller.edit { it.copy(timeoutMillis = value) } },
                            "Timeout (ms, 1-30000)", "diag-timeout", Modifier.fillMaxWidth())
                    }
                }
                val explanation: @Composable () -> Unit = {
                    Column(Modifier.widthIn(max = 580.dp), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                        Text("The source selector above applies to TCP and gateway checks. DNS uses system routing.", style = MaterialTheme.typography.caption)
                        Text("ICMP ping: unavailable on this platform. TCP connection time is not ICMP RTT.", style = MaterialTheme.typography.caption)
                    }
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xl), verticalAlignment = Alignment.CenterVertically) {
                    timeout()
                    Box(Modifier.weight(1f)) { explanation() }
                } else { timeout(); explanation() }
            } }
        }
    }
}

@Composable
private fun NetworkSummary(entry: DiagnosticInterface) {
    val binding = entry.source.binding
    val tokens = LocalSecureLanDesignTokens.current
    Card(Modifier.fillMaxWidth(), elevation = 0.dp, backgroundColor = tokens.colors.surfaceLevel1,
        border = androidx.compose.foundation.BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (binding.up) SecureLanIcons.PresenceOnline else SecureLanIcons.PresenceOffline, contentDescription = null,
                    tint = if (binding.up) tokens.colors.success else tokens.colors.textSecondary,
                    modifier = Modifier.size(18.dp))
                Text("${entry.source.displayName} (${binding.interfaceKey})", style = MaterialTheme.typography.subtitle2)
                Text(if (binding.up) "Up" else "Down", style = MaterialTheme.typography.caption,
                    color = tokens.colors.textSecondary)
            }
            BoxWithConstraints {
                val gateway = entry.gateways.joinToString { it.value }.ifEmpty { "None reported" }
                val dns = entry.dnsServers.joinToString().ifEmpty { "None reported" }
                val spacious = maxWidth >= 620.dp
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                if (spacious && entry.metadata == MetadataAvailability.AVAILABLE) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        NetworkFact("IPv4", "${binding.address.value}/${binding.prefixLength}", Modifier.weight(1f))
                        NetworkFact("Gateway", gateway, Modifier.weight(1f))
                        NetworkFact("DNS servers", dns, Modifier.weight(1f))
                    }
                } else {
                    Text("IPv4: ${binding.address.value}/${binding.prefixLength} - ${if (binding.up) "Up" else "Down"} - ${binding.kind}", style = MaterialTheme.typography.body2)
                    if (entry.metadata == MetadataAvailability.AVAILABLE) {
                        Text("Gateway: $gateway", style = MaterialTheme.typography.body2)
                        Text("DNS servers: $dns", style = MaterialTheme.typography.body2)
                    } else Text("Gateway / DNS: ${metadataLabel(entry.metadata)}", style = MaterialTheme.typography.body2)
                }
                }
            }
        }
    }
}

@Composable
private fun TcpForm(state: LanDiagnosticsUiState, controller: DesktopDiagnosticsController) {
    val input = state.inputs
    val tokens = LocalSecureLanDesignTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        DiagnosticField(input.tcpHost, { value -> controller.edit { it.copy(tcpHost = value) } },
            "TCP hostname or IP", "diag-tcp-host", Modifier.weight(1f))
        DiagnosticField(input.tcpPort, { value -> controller.edit { it.copy(tcpPort = value) } },
            "TCP port", "diag-tcp-port", Modifier.width(90.dp))
    }
    OperationButton("Check TCP", DiagnosticKind.TCP, state, controller, controller::checkTcp)
    Outcome(state.outcome(DiagnosticKind.TCP))
    LanDisclosure("About TCP results", SecureLanIcons.Information) { Text("A refused or timed-out port does not prove a device is offline. A hostname check uses the first suitable resolved address, not every address.", style = MaterialTheme.typography.caption) }
}

@Composable
private fun DnsForm(state: LanDiagnosticsUiState, controller: DesktopDiagnosticsController) {
    val input = state.inputs
    DiagnosticField(input.dnsHost, { value -> controller.edit { it.copy(dnsHost = value) } },
        "DNS hostname or IP", "diag-dns-host", Modifier.fillMaxWidth())
    var familyExpanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { familyExpanded = true }) {
            Text("Address filter: ${input.family}")
            Spacer(Modifier.width(8.dp))
            Icon(SecureLanIcons.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
        }
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
        DiagnosticField(input.gatewayPort, { value -> controller.edit { it.copy(gatewayPort = value) } },
            "Gateway TCP port", "diag-gateway-port", Modifier.widthIn(max = 220.dp).fillMaxWidth())
        OperationButton("Check gateway TCP", DiagnosticKind.GATEWAY, state, controller, controller::checkGateway)
    } else Text(if (selected == null) "Select an available source interface to view gateway options."
        else "No gateway check is available: ${if (selected.metadata == MetadataAvailability.AVAILABLE) "no IPv4 gateway reported" else metadataLabel(selected.metadata)}.", style = MaterialTheme.typography.caption)
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
private fun OperationButton(label: String, kind: DiagnosticKind, state: LanDiagnosticsUiState,
    controller: DesktopDiagnosticsController, action: () -> Unit, prominent: Boolean = true) {
    val running = state.outcome(kind).phase == DiagnosticPhase.RUNNING
    FlowRow {
        if (prominent) Button(onClick = action, enabled = !running) {
            Icon(SecureLanIcons.RunCheck, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LocalSecureLanDesignTokens.current.spacing.xs))
            Text(label)
        }
        else OutlinedButton(onClick = action, enabled = !running) {
            Icon(SecureLanIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LocalSecureLanDesignTokens.current.spacing.xs))
            Text(label)
        }
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

/** Compact input keeps its accessible name after the placeholder disappears. */
@Composable
private fun DiagnosticField(value: String, change: (String) -> Unit, label: String, tag: String, modifier: Modifier) {
    CompactTextField(value, change, outlined = true, label = "", placeholder = label, modifier = modifier.testTag("$tag-field"),
        inputModifier = Modifier.testTag(tag).semantics { contentDescription = label })
}

@Composable
private fun NetworkFact(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label:", style = MaterialTheme.typography.caption)
        Text(value, style = MaterialTheme.typography.body2)
    }
}
