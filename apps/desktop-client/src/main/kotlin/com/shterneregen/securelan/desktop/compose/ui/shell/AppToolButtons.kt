package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun AppToolButtons(
    onOpenQuickShare: () -> Unit,
    onOpenSteganography: () -> Unit,
    onOpenLanTools: (() -> Unit)? = null,
    toolsFocusRequester: FocusRequester? = null,
) {
    AppToolButton(
        icon = SecureLanIcons.QuickShare,
        onClick = onOpenQuickShare,
    )
    var toolsExpanded by remember { mutableStateOf(false) }
    Box {
        CompactButton(onClick = { toolsExpanded = true },
            modifier = toolsFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier) {
            Icon(SecureLanIcons.Commands, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tools")
            Text(" ▾")
        }
        DropdownMenu(expanded = toolsExpanded, onDismissRequest = { toolsExpanded = false }) {
            DropdownMenuItem(onClick = { toolsExpanded = false; onOpenLanTools?.invoke() }, enabled = onOpenLanTools != null) {
                Icon(SecureLanIcons.Network, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("LAN Tools")
            }
            DropdownMenuItem(onClick = { toolsExpanded = false; onOpenSteganography() }) {
                Icon(SecureLanIcons.Steganography, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("Steganography")
            }
        }
    }
}

@Composable
private fun AppToolButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    CompactButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Quick Share")
    }
}
