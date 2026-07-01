package com.shterneregen.securelan.desktop.compose.ui.media

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.components.DeviceChoiceDropdown

@Composable
internal fun LiveMediaVoiceCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val state = hostAdapter.mediaVoiceState.copy(peerListState = peerState)
    MediaVoiceCardContent(
        state = state,
        onRefresh = hostAdapter::refreshMediaDevices,
        onMicrophoneSelected = hostAdapter::selectMicrophone,
        onTestMicrophone = { hostAdapter.testMicrophone() },
        onStartVoice = {
            state.selectedPeer?.let { peer ->
                hostAdapter.startRealtimeSession(
                    localPeer = hostAdapter.statusState.nickname,
                    remotePeer = peer.nickname,
                    mode = RtcSessionMode.AUDIO,
                )
            }
        },
        onHangUp = hostAdapter::closeRealtimeSession,
        diagnostics = emptyList(),
    )
}

@Composable
internal fun MediaVoicePreviewCard(initialState: ComposeMediaVoiceState) {
    MediaVoiceCardContent(initialState, {}, {}, {}, {}, {}, emptyList(), previewOnly = true)
}

@Composable
internal fun MediaVoiceCardContent(
    state: ComposeMediaVoiceState,
    onRefresh: () -> Unit,
    onMicrophoneSelected: (String?) -> Unit,
    onTestMicrophone: () -> Unit,
    onStartVoice: () -> Unit,
    onHangUp: () -> Unit,
    diagnostics: List<String>,
    previewOnly: Boolean = false,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.title, style = MaterialTheme.typography.h6)
            Text(
                "Status: ${state.runtimeLabel} · Target: ${state.selectedPeerName}",
                style = MaterialTheme.typography.body2
            )
            DeviceChoiceDropdown(
                label = "Microphone",
                choices = state.microphones,
                selected = state.selectedMicrophone,
                enabled = !previewOnly,
                onSelected = onMicrophoneSelected,
            )
            Text(state.voiceStatusText, style = MaterialTheme.typography.body2)
            Text("${state.localAudioLabel} · ${state.remoteAudioLabel}", style = MaterialTheme.typography.caption)
            Text(
                state.microphoneTestStatus,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactButton(
                    onClick = onRefresh,
                    enabled = !previewOnly && state.canRefreshDevices
                ) { Text("Refresh devices") }
                CompactButton(
                    onClick = onTestMicrophone,
                    enabled = !previewOnly && state.canTestMicrophone
                ) { Text("Test mic") }
                CompactButton(
                    onClick = onStartVoice,
                    enabled = !previewOnly && state.canStartVoice
                ) { Text(state.startVoiceLabel) }
                CompactButton(onClick = onHangUp, enabled = !previewOnly && state.canHangUp, tone = CompactButtonTone.DESTRUCTIVE) { Text("Hang up") }
            }
            diagnostics.takeLast(4).forEach {
                Text(
                    "• $it",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
                )
            }
            Text(
                state.readinessSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
            )
        }
    }
}
