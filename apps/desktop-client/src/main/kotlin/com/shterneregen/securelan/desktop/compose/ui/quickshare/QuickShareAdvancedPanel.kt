package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeQuickShareState
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
    var editing by remember(state.running) { mutableStateOf(!state.running) }

    ComposeAdvancedPane(title = "Advanced") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.running && !editing) {
                Text(
                    text = state.policySentence,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
                )
                Text(
                    text = "Current links keep their original settings.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                )
                CompactButton(onClick = { editing = true }) { Text("Change settings") }
            } else {
                PolicyField(
                    label = "Link expires after",
                    value = expirationMinutes,
                    suffix = "minutes",
                    enabled = !noExpiration,
                    error = state.expirationValidationMessage,
                    onValueChange = {
                        onNoExpirationChange(false)
                        onExpirationMinutesChange(it)
                    },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PresetButton("10 min", !noExpiration && expirationMinutes == "10") {
                        onNoExpirationChange(false)
                        onExpirationMinutesChange("10")
                    }
                    PresetButton("1 hour", !noExpiration && expirationMinutes == "60") {
                        onNoExpirationChange(false)
                        onExpirationMinutesChange("60")
                    }
                    PresetButton("Until stopped", noExpiration) {
                        onNoExpirationChange(true)
                    }
                }

                PolicyField(
                    label = "Download limit",
                    value = accessLimit,
                    suffix = "downloads",
                    enabled = !unlimitedAccess,
                    error = state.accessValidationMessage,
                    onValueChange = {
                        onUnlimitedAccessChange(false)
                        onAccessLimitChange(it)
                    },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("1", "3", "10").forEach { preset ->
                        PresetButton(preset, !unlimitedAccess && accessLimit == preset) {
                            onUnlimitedAccessChange(false)
                            onAccessLimitChange(preset)
                        }
                    }
                    PresetButton("Unlimited", unlimitedAccess) {
                        onUnlimitedAccessChange(true)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = useCustomPort,
                        onCheckedChange = onUseCustomPortChange,
                        enabled = !state.running,
                    )
                    Text(
                        text = "Use custom port",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2,
                    )
                }
                if (useCustomPort) {
                    PolicyField(
                        label = "Port",
                        value = port,
                        suffix = "",
                        enabled = !state.running,
                        error = state.portValidationMessage,
                        onValueChange = onPortChange,
                    )
                }
                if (state.running) {
                    Text(
                        text = "Stop Quick Share before changing its port. Link-policy changes apply only to new links.",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                    )
                }
                if (!state.serverError.isNullOrBlank()) {
                    Text(
                        text = state.serverError,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.error,
                    )
                }

                Text(
                    text = state.policySentence,
                    style = MaterialTheme.typography.caption,
                    color = if (
                        state.expirationValidationMessage == null &&
                        state.accessValidationMessage == null &&
                        state.portValidationMessage == null
                    ) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.58f)
                    },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PolicyField(
    label: String,
    value: String,
    suffix: String,
    enabled: Boolean,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactTextField(
                value = value,
                onValueChange = onValueChange,
                label = "",
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            if (suffix.isNotBlank()) {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f),
                )
            }
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
            )
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    CompactButton(
        onClick = onClick,
        tone = if (selected) CompactButtonTone.SECONDARY else CompactButtonTone.TERTIARY,
    ) {
        Text(label)
    }
}
