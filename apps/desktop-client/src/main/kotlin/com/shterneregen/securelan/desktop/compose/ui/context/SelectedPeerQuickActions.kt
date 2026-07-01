package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun SelectedPeerQuickActions(
    attachEnabled: Boolean,
    voiceEnabled: Boolean,
    videoEnabled: Boolean,
    hangUpEnabled: Boolean,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    onVideo: () -> Unit,
    onHangUp: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickActionButton(
            onClick = onAttach,
            enabled = attachEnabled,
            icon = SecureLanIcons.Attach,
            label = "Attach",
        )
        QuickActionButton(
            onClick = onVoice,
            enabled = voiceEnabled,
            icon = SecureLanIcons.Voice,
            label = "Voice call",
        )
        QuickActionButton(
            onClick = onVideo,
            enabled = videoEnabled,
            icon = SecureLanIcons.Video,
            label = "Video call",
        )
        QuickActionButton(
            onClick = onHangUp,
            enabled = hangUpEnabled,
            icon = SecureLanIcons.CallEnd,
            label = "End call",
            tone = CompactButtonTone.DESTRUCTIVE,
        )
    }
}

@Composable
private fun QuickActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    tone: CompactButtonTone = CompactButtonTone.SECONDARY,
) {
    CompactButton(onClick = onClick, enabled = enabled, tone = tone) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(label)
        }
    }
}
