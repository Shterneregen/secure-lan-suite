package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun QuickShareServerPanel(
    state: ComposeQuickShareState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCopyIndex: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.running) {
            ServerActiveRow(state = state)
            ServerRunningActions(
                onStop = onStop,
                onCopyIndex = onCopyIndex,
                canStopServer = state.canStopServer,
                canCopyIndex = state.canCopyIndex,
            )
        } else {
            ServerIdleExplanation(state = state)
            CompactButton(
                onClick = onStart,
                enabled = state.canStartServer,
                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        SecureLanIcons.QuickShare,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.onPrimary,
                    )
                    Text(
                        text = "Start sharing",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Text(
            text = "LAN only — no login required",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.48f),
        )
    }
}

@Composable
private fun ServerActiveRow(state: ComposeQuickShareState) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tokens.colors.success, RoundedCornerShape(4.dp)),
            )
            Text(
                text = state.serverStatusSummary,
                style = MaterialTheme.typography.body2,
                color = tokens.colors.success,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.landingText.isNotBlank()) {
            Text(
                text = state.landingText,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ServerRunningActions(
    onStop: () -> Unit,
    onCopyIndex: () -> Unit,
    canStopServer: Boolean,
    canCopyIndex: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        CompactButton(
            onClick = onStop,
            enabled = canStopServer,
            tone = CompactButtonTone.SECONDARY,
            modifier = Modifier.widthIn(min = 108.dp).heightIn(min = 34.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    SecureLanIcons.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Stop sharing",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CompactButton(
            onClick = onCopyIndex,
            enabled = canCopyIndex,
            tone = CompactButtonTone.TERTIARY,
            modifier = Modifier.heightIn(min = 34.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Copy",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    SecureLanIcons.Copy,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                )
            }
        }
    }
}

@Composable
private fun ServerIdleExplanation(state: ComposeQuickShareState) {
    Text(
        text = state.statusDetail,
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
    )
}
