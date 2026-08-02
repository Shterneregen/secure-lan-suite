package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.StatusChip
import com.shterneregen.securelan.desktop.ui.LocalNetworkAddress

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NetworkStatusChip(
    text: String,
    addresses: List<LocalNetworkAddress>,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var expanded by remember { mutableStateOf(false) }
    val tooltipText = if (addresses.size == 1) {
        "Show local IP address"
    } else {
        "Show all ${addresses.size} local IP addresses"
    }

    Box {
        TooltipArea(
            tooltip = {
                Surface(
                    shape = RoundedCornerShape(tokens.radius.small),
                    border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                    color = MaterialTheme.colors.surface,
                ) {
                    Text(
                        text = tooltipText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.caption,
                    )
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .semantics { contentDescription = tooltipText }
                    .clickable { expanded = true },
            ) {
                StatusChip(text)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 280.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Local IP addresses", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(8.dp))
                addresses.forEachIndexed { index, address ->
                    NetworkAddressRow(address = address, primary = index == 0)
                    if (index != addresses.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun NetworkAddressRow(address: LocalNetworkAddress, primary: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(address.address, style = MaterialTheme.typography.body2)
            Text(
                text = address.interfaceLabel,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
            )
        }
        if (primary) {
            Text(
                text = "Primary",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary,
            )
        }
    }
}
