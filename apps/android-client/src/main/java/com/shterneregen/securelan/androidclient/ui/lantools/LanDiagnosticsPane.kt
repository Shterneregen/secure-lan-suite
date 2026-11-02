package com.shterneregen.securelan.androidclient.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.*
import com.shterneregen.securelan.lantools.diagnostics.AddressFamily
import com.shterneregen.securelan.lantools.diagnostics.MetadataAvailability
import com.shterneregen.securelan.androidclient.ui.appStringResource as text

/** Explicit endpoint checks only. TCP timing, resolver answers and gateway observations remain distinct. */
@Composable
internal fun LanDiagnosticsPane(state: LanToolsState, model: LanToolsViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = model::refreshNetwork, enabled = state.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING) { Text(text(R.string.lan_refresh_network)) }
        Outcome(state, LanOperation.NETWORK, model)
        state.network?.interfaces.orEmpty().forEach { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${entry.source.displayName}: ${entry.source.binding.address.value}/${entry.source.binding.prefixLength}")
                    if (entry.metadata == MetadataAvailability.AVAILABLE) {
                        Text(text(R.string.lan_gateway_value, entry.gateways.joinToString { it.value }.ifEmpty { text(R.string.lan_none_reported) }))
                        Text(text(R.string.lan_dns_servers, entry.dnsServers.joinToString().ifEmpty { text(R.string.lan_none_reported) }))
                    } else Text(text(R.string.lan_metadata_unknown))
                    if (!entry.source.broadcastCapable) Text(text(R.string.lan_no_broadcast))
                }
            }
        }
        if (state.network?.interfaces?.isEmpty() != false) Text(text(R.string.lan_no_network))
        SourcePicker(state, model)
        val input = state.input
        Field(input.timeout, { value -> model.editInput { it.copy(timeout = value) } }, R.string.lan_timeout, "lan-timeout")
        Text(text(R.string.lan_tcp), style = MaterialTheme.typography.titleLarge)
        Field(input.host, { value -> model.editInput { it.copy(host = value) } }, R.string.lan_tcp_host, "lan-tcp-host")
        Field(input.port, { value -> model.editInput { it.copy(port = value) } }, R.string.lan_tcp_port, "lan-tcp-port")
        Button(onClick = model::tcp, enabled = state.outcome(LanOperation.TCP).phase != LanPhase.RUNNING) { Text(text(R.string.lan_check_tcp)) }
        Outcome(state, LanOperation.TCP, model)
        Text(text(R.string.lan_tcp_help), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text(text(R.string.lan_dns), style = MaterialTheme.typography.titleLarge)
        Text(text(R.string.lan_dns_help), style = MaterialTheme.typography.bodySmall)
        Field(input.dns, { value -> model.editInput { it.copy(dns = value) } }, R.string.lan_dns_host, "lan-dns-host")
        var familyExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { familyExpanded = true }) { Text(text(R.string.lan_family, if (input.family == AddressFamily.ALL) text(R.string.lan_all) else input.family.name)) }
            DropdownMenu(familyExpanded, { familyExpanded = false }) {
                AddressFamily.entries.forEach { family -> DropdownMenuItem(text = { Text(if (family == AddressFamily.ALL) text(R.string.lan_all) else family.name) },
                    onClick = { model.editInput { it.copy(family = family) }; familyExpanded = false }) }
            }
        }
        Button(onClick = model::dns, enabled = state.outcome(LanOperation.DNS).phase != LanPhase.RUNNING) { Text(text(R.string.lan_lookup)) }
        Outcome(state, LanOperation.DNS, model)
        HorizontalDivider()
        Text(text(R.string.lan_gateway), style = MaterialTheme.typography.titleLarge)
        Text(text(R.string.lan_gateway_help), style = MaterialTheme.typography.bodySmall)
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
        } else Text(text(R.string.lan_no_gateway))
        Outcome(state, LanOperation.GATEWAY, model)
        Text(text(R.string.lan_icmp), style = MaterialTheme.typography.bodySmall)
    }
}
