package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice

@Composable
internal fun DeviceChoiceDropdown(
    label: String,
    choices: List<MediaDeviceChoice>,
    selected: MediaDeviceChoice,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
    helperText: String? = null,
) {
    var expanded by remember(label, selected.deviceId) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
        )
        Box {
            CalmFocusButton(
                onClick = { expanded = true },
                enabled = enabled && choices.isNotEmpty(),
                fillMaxWidth = true,
            ) {
                Text(selected.toString())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        onClick = {
                            onSelected(choice.deviceId)
                            expanded = false
                        },
                    ) {
                        Text(choice.toString())
                    }
                }
            }
        }
        helperText?.let {
            Text(
                it,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
            )
        }
    }
}
