package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.TransferInfoChip

@Composable
internal fun CallStatusPanel(
    voiceState: ComposeMediaVoiceState,
    videoState: ComposeExperimentalVideoState,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
        ) {
            TransferInfoChip(voiceState.voiceStatusText)
            if (videoState.currentSession != null || videoState.previewRunning) {
                TransferInfoChip(videoState.previewStateLabel)
            }
        }
        if (videoState.currentSession != null && videoState.stageBadge.isNotBlank()) {
            Text(
                text = videoState.stageBadge,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
        }
    }
}
