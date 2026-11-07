package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import com.shterneregen.securelan.androidclient.ui.ActionLabel
import com.shterneregen.securelan.androidclient.ui.SectionCard
import com.shterneregen.securelan.androidclient.ui.SectionHeading
import com.shterneregen.securelan.lantools.diagnostics.AddressFamily
import com.shterneregen.securelan.lantools.diagnostics.MetadataAvailability
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Overview and explicit checks share one ViewModel; opening a page does not execute a check. */
@Composable
internal fun LanDiagnosticsPane(state: LanToolsState, model: LanToolsViewModel, overviewScroll: ScrollState) {
    val input = state.input
    val scroll = if (state.diagnosticPage == LanDiagnosticPage.OVERVIEW) overviewScroll else rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scroll).testTag("lan-diagnostics-scroll"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.diagnosticPage) {
            LanDiagnosticPage.OVERVIEW -> {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionHeading(text(R.string.lan_overview), Icons.Outlined.Wifi, text(R.string.lan_overview_hint)) }
                        IconButton(onClick = model::refreshNetwork, enabled = state.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING) {
                            Icon(Icons.Outlined.Refresh, text(R.string.lan_refresh_network))
                        }
                    }
                    val outcome = state.outcome(LanOperation.NETWORK)
                    if (outcome.phase == LanPhase.RUNNING) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        TextButton(onClick = { model.cancel(LanOperation.NETWORK) }) { Text(text(R.string.lan_cancel)) }
                    }
                    outcome.message?.let { Notice(it, outcome.phase == LanPhase.FAILURE) }
                    val interfaces = state.network?.interfaces.orEmpty()
                    interfaces.forEach { entry ->
                        HorizontalDivider()
                        Text("${entry.source.displayName}: ${entry.source.binding.address.value}/${entry.source.binding.prefixLength}", style = MaterialTheme.typography.bodyMedium)
                        if (entry.metadata == MetadataAvailability.AVAILABLE) {
                            Text(text(R.string.lan_gateway_value, entry.gateways.joinToString { it.value }.ifEmpty { text(R.string.lan_none_reported) }), style = MaterialTheme.typography.bodySmall)
                            Text(text(R.string.lan_dns_servers, entry.dnsServers.joinToString().ifEmpty { text(R.string.lan_none_reported) }), style = MaterialTheme.typography.bodySmall)
                        } else Text(text(metadataLabel(entry.metadata)), style = MaterialTheme.typography.bodySmall)
                    }
                    if (interfaces.isEmpty()) Text(text(R.string.lan_no_network), style = MaterialTheme.typography.bodySmall)
                }
                DiagnosticEntry(R.string.lan_tcp, R.string.lan_tcp_hint, Icons.Outlined.Share, "lan-open-tcp") { model.diagnosticPage(LanDiagnosticPage.TCP) }
                DiagnosticEntry(R.string.lan_dns, R.string.lan_dns_hint, Icons.Outlined.Layers, "lan-open-dns") { model.diagnosticPage(LanDiagnosticPage.DNS) }
                DiagnosticEntry(R.string.lan_gateway, R.string.lan_gateway_hint, Icons.Outlined.Router, "lan-open-gateway") { model.diagnosticPage(LanDiagnosticPage.GATEWAY) }
                DiagnosticEntry(R.string.lan_advanced, R.string.lan_advanced_hint, Icons.Outlined.Settings, "lan-open-advanced") { model.diagnosticPage(LanDiagnosticPage.ADVANCED) }
            }
            LanDiagnosticPage.TCP -> {
                SectionCard {
                    Text(text(R.string.lan_known_port), style = MaterialTheme.typography.bodySmall)
                    val scale = LocalDensity.current.fontScale
                    BoxWithConstraints {
                        val useInlineRow = this@BoxWithConstraints.maxWidth >= 360.dp && scale < 1.4f
                        if (useInlineRow) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Field(input.host, { value -> model.editInput { it.copy(host = value) } }, R.string.lan_tcp_host, "lan-tcp-host", Modifier.weight(1f))
                            Field(input.port, { value -> model.editInput { it.copy(port = value) } }, R.string.lan_tcp_port, "lan-tcp-port", Modifier.width(112.dp))
                        } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Field(input.host, { value -> model.editInput { it.copy(host = value) } }, R.string.lan_tcp_host, "lan-tcp-host")
                            Field(input.port, { value -> model.editInput { it.copy(port = value) } }, R.string.lan_tcp_port, "lan-tcp-port")
                        }
                    }
                    SourcePicker(state, model)
                    Button(onClick = model::tcp, enabled = state.outcome(LanOperation.TCP).phase != LanPhase.RUNNING, modifier = Modifier.fillMaxWidth().testTag("lan-check-tcp")) {
                        ActionLabel(text(R.string.lan_check_tcp), Icons.Outlined.PlayArrow)
                    }
                }
                Outcome(state, LanOperation.TCP, model)
                if (state.outcome(LanOperation.TCP).phase == LanPhase.IDLE) Text(text(R.string.lan_idle), style = MaterialTheme.typography.bodySmall)
                Help(R.string.lan_tcp_help)
            }
            LanDiagnosticPage.DNS -> {
                SectionCard {
                    Field(input.dns, { value -> model.editInput { it.copy(dns = value) } }, R.string.lan_dns_host, "lan-dns-host")
                    DnsFamilyPicker(state, model)
                    Button(onClick = model::dns, enabled = state.outcome(LanOperation.DNS).phase != LanPhase.RUNNING, modifier = Modifier.fillMaxWidth().testTag("lan-check-dns")) {
                        ActionLabel(text(R.string.lan_lookup), Icons.Outlined.Search)
                    }
                }
                Outcome(state, LanOperation.DNS, model)
                if (state.outcome(LanOperation.DNS).phase == LanPhase.IDLE) Text(text(R.string.lan_idle), style = MaterialTheme.typography.bodySmall)
                Help(R.string.lan_dns_help)
            }
            LanDiagnosticPage.GATEWAY -> {
                SectionCard {
                    SourcePicker(state, model)
                    val gateways = state.selectedInterface?.gateways.orEmpty()
                    if (state.selectedInterface?.metadata == MetadataAvailability.AVAILABLE && gateways.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }) { Text(text(R.string.lan_gateway_picker, input.gateway ?: text(R.string.lan_unknown))) }
                            DropdownMenu(expanded, { expanded = false }) {
                                gateways.forEach { gateway -> DropdownMenuItem(text = { Text(gateway.value) }, onClick = { model.editInput { it.copy(gateway = gateway.value) }; expanded = false }) }
                            }
                        }
                        Field(input.gatewayPort, { value -> model.editInput { it.copy(gatewayPort = value) } }, R.string.lan_gateway_port, "lan-gateway-port")
                        Button(onClick = model::gateway, enabled = state.outcome(LanOperation.GATEWAY).phase != LanPhase.RUNNING) { Text(text(R.string.lan_gateway_check)) }
                    } else {
                        Text(text(if (state.selectedInterface == null) R.string.lan_gateway_select_hint else R.string.lan_gateway_unreported), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = model::refreshNetwork, enabled = state.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING) { Text(text(R.string.lan_refresh_network)) }
                        state.outcome(LanOperation.NETWORK).message?.let { Notice(it) }
                    }
                }
                Outcome(state, LanOperation.GATEWAY, model)
                Help(R.string.lan_gateway_help)
            }
            LanDiagnosticPage.ADVANCED -> {
                SectionCard {
                    Field(input.timeout, { value -> model.editInput { it.copy(timeout = value) } }, R.string.lan_timeout, "lan-timeout")
                    SourcePicker(state, model)
                    Text(text(R.string.lan_icmp), style = MaterialTheme.typography.bodySmall)
                    Text(text(R.string.lan_dns_help), style = MaterialTheme.typography.bodySmall)
                    state.network?.interfaces.orEmpty().forEach { entry ->
                        Text("${entry.source.displayName}: ${entry.source.binding.address.value}", style = MaterialTheme.typography.bodySmall)
                        Text(text(metadataLabel(entry.metadata)), style = MaterialTheme.typography.bodySmall)
                        if (!entry.source.broadcastCapable) Text(text(R.string.lan_no_broadcast), style = MaterialTheme.typography.bodySmall)
                    }
                    state.network?.provenance?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun metadataLabel(metadata: MetadataAvailability) = when (metadata) {
    MetadataAvailability.AVAILABLE -> R.string.lan_metadata_available
    MetadataAvailability.TIMEOUT -> R.string.lan_metadata_timeout
    MetadataAvailability.PERMISSION_DENIED -> R.string.lan_metadata_denied
    MetadataAvailability.RESOURCE_LIMIT -> R.string.lan_metadata_limit
    MetadataAvailability.UNSUPPORTED -> R.string.lan_metadata_unsupported
    MetadataAvailability.UNAVAILABLE -> R.string.lan_metadata_unknown
}

@Composable
private fun Help(resource: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Disclosure(text(R.string.lan_details), expanded, { expanded = !expanded }, "lan-operation-help")
    if (expanded) Text(text(resource), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun DiagnosticEntry(title: Int, hint: Int, icon: ImageVector, tag: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { SectionHeading(text(title), icon, text(hint)) }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

@Composable
private fun DnsFamilyPicker(state: LanToolsState, model: LanToolsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(text(R.string.lan_family, if (state.input.family == AddressFamily.ALL) text(R.string.lan_all) else state.input.family.name), style = MaterialTheme.typography.bodySmall)
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            AddressFamily.entries.forEach { family -> DropdownMenuItem(text = { Text(if (family == AddressFamily.ALL) text(R.string.lan_all) else family.name) },
                onClick = { model.editInput { it.copy(family = family) }; expanded = false }) }
        }
    }
}
