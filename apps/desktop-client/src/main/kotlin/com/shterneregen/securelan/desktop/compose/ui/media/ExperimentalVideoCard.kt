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
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.components.DeviceChoiceDropdown

@Composable
internal fun LiveExperimentalVideoCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val state = hostAdapter.experimentalVideoState.copy(peerListState = peerState)
    ExperimentalVideoCardContent(
        state = state,
        onRefresh = hostAdapter::refreshMediaDevices,
        onCameraSelected = hostAdapter::selectCamera,
        onTestCamera = { hostAdapter.testCamera() },
        onStartPreview = { hostAdapter.startCameraPreview() },
        onStopPreview = hostAdapter::closeCameraPreview,
        onStartVideo = {
            state.selectedPeer?.let { peer ->
                hostAdapter.startRealtimeSession(
                    localPeer = hostAdapter.statusState.nickname,
                    remotePeer = peer.nickname,
                    mode = RtcSessionMode.AUDIO_VIDEO,
                )
            }
        },
        onHangUp = hostAdapter::closeRealtimeSession,
        diagnostics = emptyList(),
    )
}

@Composable
internal fun ExperimentalVideoPreviewCard(initialState: ComposeExperimentalVideoState) {
    ExperimentalVideoCardContent(initialState, {}, {}, {}, {}, {}, {}, {}, emptyList(), previewOnly = true)
}

@Composable
internal fun ExperimentalVideoCardContent(
    state: ComposeExperimentalVideoState,
    onRefresh: () -> Unit,
    onCameraSelected: (String?) -> Unit,
    onTestCamera: () -> Unit,
    onStartPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onStartVideo: () -> Unit,
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
                label = "Camera",
                choices = state.cameras,
                selected = state.selectedCamera,
                enabled = !previewOnly,
                onSelected = onCameraSelected,
            )
            Text(state.previewConfigurationLabel, style = MaterialTheme.typography.caption)
            Text(
                "${state.stageTitle} · ${state.stageBadge} · ${state.mediaLabel}",
                style = MaterialTheme.typography.body2
            )
            Text("${state.previewStatus} · ${state.frameCaption}", style = MaterialTheme.typography.caption)
            Text(
                state.cameraTestStatus,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactButton(
                    onClick = onRefresh,
                    enabled = !previewOnly && state.canRefreshCameras
                ) { Text("Refresh cameras") }
                CompactButton(
                    onClick = onTestCamera,
                    enabled = !previewOnly && state.canTestCamera
                ) { Text("Test camera") }
                CompactButton(
                    onClick = onStartPreview,
                    enabled = !previewOnly && state.canStartPreview
                ) { Text("Start preview") }
                CompactButton(
                    onClick = onStopPreview,
                    enabled = !previewOnly && state.canStopPreview
                ) { Text("Stop preview") }
                CompactButton(
                    onClick = onStartVideo,
                    enabled = !previewOnly && state.canStartVideo
                ) { Text(state.startVideoLabel) }
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
