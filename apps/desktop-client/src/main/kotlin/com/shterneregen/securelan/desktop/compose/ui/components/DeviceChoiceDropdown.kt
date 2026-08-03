package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
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
    var fieldWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val tokens = LocalSecureLanDesignTokens.current
    val canOpen = enabled && choices.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        fieldWidth = with(density) { coordinates.size.width.toDp() }
                    }
                    .semantics { contentDescription = "$label, ${selected.label}" }
                    .clickable(enabled = canOpen) { expanded = true },
                shape = RoundedCornerShape(tokens.radius.small),
                color = MaterialTheme.colors.surface,
                border = BorderStroke(
                    1.dp,
                    if (expanded) MaterialTheme.colors.primary
                    else tokens.colors.borderSubtle.copy(alpha = if (canOpen) 0.90f else 0.45f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = if (canOpen) 0.92f else 0.48f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (expanded) "⌃" else "⌄",
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = if (canOpen) 0.68f else 0.38f),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(fieldWidth).heightIn(max = 320.dp),
            ) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        onClick = {
                            onSelected(choice.deviceId)
                            expanded = false
                        },
                    ) {
                        Text(
                            text = choice.toString(),
                            modifier = Modifier.weight(1f),
                            fontWeight = if (choice.deviceId == selected.deviceId) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (choice.deviceId == selected.deviceId) {
                            Text("✓", color = MaterialTheme.colors.primary)
                        }
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
