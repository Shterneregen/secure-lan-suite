package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeSelectedPeerQuickActionsState

@Composable
internal fun SelectedPeerQuickActionsCard(
    state: ComposeSelectedPeerQuickActionsState,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    onVideo: () -> Unit,
    onHangUp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(state.title, style = MaterialTheme.typography.subtitle1)
        Text(
            text = state.meta,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
        )
        Text(
            text = state.readinessLabel,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
        )
        SelectedPeerQuickActions(
            attachEnabled = state.attachEnabled,
            voiceEnabled = state.voiceEnabled,
            videoEnabled = state.videoEnabled,
            hangUpEnabled = state.hangUpEnabled,
            onAttach = onAttach,
            onVoice = onVoice,
            onVideo = onVideo,
            onHangUp = onHangUp,
        )
    }
}
