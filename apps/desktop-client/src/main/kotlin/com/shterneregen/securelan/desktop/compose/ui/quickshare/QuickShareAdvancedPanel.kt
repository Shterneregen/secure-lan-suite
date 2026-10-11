package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.components.ComposeAdvancedPane

@Composable
internal fun QuickShareAdvancedPanel(
    state: ComposeQuickShareState,
    port: String,
    onPortChange: (String) -> Unit,
    expirationMinutes: String,
    onExpirationMinutesChange: (String) -> Unit,
    accessLimit: String,
    onAccessLimitChange: (String) -> Unit,
    useCustomPort: Boolean,
    onUseCustomPortChange: (Boolean) -> Unit,
    noExpiration: Boolean,
    onNoExpirationChange: (Boolean) -> Unit,
    unlimitedAccess: Boolean,
    onUnlimitedAccessChange: (Boolean) -> Unit,
) {
    var customExpiration by remember { mutableStateOf(!noExpiration && expirationMinutes !in listOf("10", "60")) }
    var customLimit by remember { mutableStateOf(!unlimitedAccess && accessLimit !in listOf("1", "3", "10")) }
    ComposeAdvancedPane(title = "Advanced") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PolicySelector(
                label = "Link lifetime",
                value = when {
                    noExpiration -> "Until stopped"
                    customExpiration -> "Custom duration"
                    expirationMinutes == "60" -> "1 hour"
                    else -> "10 minutes"
                },
                options = listOf("10 minutes", "1 hour", "Until stopped", "Custom duration"),
                onSelected = { option ->
                    customExpiration = option == "Custom duration"
                    onNoExpirationChange(option == "Until stopped")
                    when (option) {
                        "10 minutes" -> onExpirationMinutesChange("10")
                        "1 hour" -> onExpirationMinutesChange("60")
                    }
                },
            )
            if (customExpiration && !noExpiration) {
                PolicyField("Minutes", expirationMinutes, true, state.expirationValidationMessage, onExpirationMinutesChange)
            }
            PolicySelector(
                label = "Download limit",
                value = when {
                    unlimitedAccess -> "Unlimited"
                    customLimit -> "Custom limit"
                    accessLimit == "1" -> "1 download"
                    else -> "$accessLimit downloads"
                },
                options = listOf("1 download", "3 downloads", "10 downloads", "Unlimited", "Custom limit"),
                onSelected = { option ->
                    customLimit = option == "Custom limit"
                    onUnlimitedAccessChange(option == "Unlimited")
                    when (option) {
                        "1 download" -> onAccessLimitChange("1")
                        "3 downloads" -> onAccessLimitChange("3")
                        "10 downloads" -> onAccessLimitChange("10")
                    }
                },
            )
            if (customLimit && !unlimitedAccess) {
                PolicyField("Number of downloads", accessLimit, true, state.accessValidationMessage, onAccessLimitChange)
            }
            Text(
                "Applies to new links.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = useCustomPort,
                    enabled = !state.running,
                    role = Role.Checkbox,
                    onValueChange = onUseCustomPortChange,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = useCustomPort, onCheckedChange = null, enabled = !state.running)
                Text("Use custom port", style = MaterialTheme.typography.body2)
            }
            if (useCustomPort) {
                PolicyField("Port", port, !state.running, state.portValidationMessage, onPortChange)
            }
            if (state.running && useCustomPort) {
                Text(
                    "Stop sharing to change the port.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                )
            }
            state.serverError?.takeIf { it.isNotBlank() && (!useCustomPort || it != state.portValidationMessage) }?.let {
                Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
            }
        }
    }
}

@Composable
private fun PolicySelector(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f))
        Box(Modifier.fillMaxWidth()) {
            CompactButton(
                onClick = { expanded = true },
                tone = CompactButtonTone.TERTIARY,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(value, modifier = Modifier.weight(1f))
                Text("⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        expanded = false
                        onSelected(option)
                    }) {
                        Text(option, color = if (option == value) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyField(
    label: String,
    value: String,
    enabled: Boolean,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CompactTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit)) },
            label = label,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!error.isNullOrBlank()) {
            Text(error, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
        }
    }
}
