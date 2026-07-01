package com.shterneregen.securelan.desktop.compose.ui.peerlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerAvailabilityKind
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerCapabilityPresentation
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItemPresentation
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.util.MicrointeractionTone
import com.shterneregen.securelan.desktop.compose.util.calmFocusRing
import com.shterneregen.securelan.desktop.compose.util.interactiveSurfaceBorder
import com.shterneregen.securelan.desktop.compose.util.rememberInteractiveSurfaceState

@Composable
internal fun PeerPreviewRow(
    row: ComposePeerListItemPresentation,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val peer = row.peer
    val statusColor = when (row.availability) {
        ComposePeerAvailabilityKind.ONLINE -> tokens.colors.success
        ComposePeerAvailabilityKind.OFFLINE -> tokens.colors.textTertiary
    }
    val (interactionSource, interactive) = rememberInteractiveSurfaceState(
        selected = selected,
        enabled = true,
        tone = if (peer.online) MicrointeractionTone.SUCCESS else MicrointeractionTone.NEUTRAL,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.density.sidebarRowMinHeight)
            .calmFocusRing(interactive.focused, tokens.radius.medium)
            .semantics(mergeDescendants = true) {
                contentDescription = row.accessibilityLabel
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onSelect,
            ),
        color = interactive.backgroundColor,
        shape = RoundedCornerShape(tokens.radius.medium),
        border = interactiveSurfaceBorder(interactive),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (peer.online) SecureLanIcons.PresenceOnline else SecureLanIcons.PresenceOffline,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = statusColor,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.body1,
                        color = interactive.contentColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }
                PeerCapabilityStrip(row.capabilityChips, row.capabilitySummary, peer.online)
            }
        }
    }
}

@Composable
private fun PeerCapabilityStrip(
    capabilities: List<ComposePeerCapabilityPresentation>,
    fallbackSummary: String,
    online: Boolean,
) {
    val tokens = LocalSecureLanDesignTokens.current
    if (capabilities.isEmpty()) {
        Text(
            text = fallbackSummary,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = if (online) 0.58f else 0.46f),
            maxLines = 1,
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        capabilities.take(3).forEach { capability ->
            PeerCapabilityChip(capability.label, online)
        }
    }
}

@Composable
private fun PeerCapabilityChip(
    label: String,
    online: Boolean,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val content = if (online) tokens.colors.textSecondary else tokens.colors.textTertiary
    Surface(
        shape = RoundedCornerShape(tokens.radius.pill),
        color = tokens.colors.surfaceLevel3.copy(alpha = if (online) 0.54f else 0.34f),
        border = BorderStroke(
            tokens.border.subtle,
            tokens.colors.borderSubtle.copy(alpha = if (online) 0.74f else 0.42f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val capabilityIcon = SecureLanIcons.forCapability(label)
            if (capabilityIcon != null) {
                Icon(
                    imageVector = capabilityIcon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = content,
                )
            }
            Text(label, style = MaterialTheme.typography.caption, color = content, maxLines = 1)
        }
    }
}
