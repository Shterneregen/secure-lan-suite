package com.shterneregen.securelan.desktop.compose.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.ui.components.*
import com.shterneregen.securelan.desktop.compose.util.rememberPreviewImage

@Composable
internal fun LiveAudioVideoDevicesCard(hostAdapter: ComposeDesktopHostAdapter, peerState: ComposePeerListState) {
    val voiceState = hostAdapter.mediaVoiceState.copy(peerListState = peerState)
    val videoState = hostAdapter.experimentalVideoState.copy(peerListState = peerState)
    AudioVideoDevicesCardContent(
        voiceState = voiceState,
        videoState = videoState,
        onRefresh = hostAdapter::refreshMediaDevices,
        onMicrophoneSelected = hostAdapter::selectMicrophone,
        onSpeakerSelected = hostAdapter::selectSpeaker,
        onCameraSelected = hostAdapter::selectCamera,
        onTestMicrophone = { hostAdapter.testMicrophone() },
        onTestSpeaker = { hostAdapter.testSpeaker() },
        onTestCamera = { hostAdapter.testCamera() },
        onStartPreview = { hostAdapter.startCameraPreview() },
        onStopPreview = hostAdapter::closeCameraPreview,
        diagnostics = emptyList(),
    )
}

@Composable
internal fun AudioVideoDevicesPreviewCard() {
    AudioVideoDevicesCardContent(
        voiceState = ComposeShellMetadata.DEFAULT_MEDIA_VOICE_STATE,
        videoState = ComposeShellMetadata.DEFAULT_VIDEO_STATE,
        onRefresh = {},
        onMicrophoneSelected = {},
        onSpeakerSelected = {},
        onCameraSelected = {},
        onTestMicrophone = {},
        onTestSpeaker = {},
        onTestCamera = {},
        onStartPreview = {},
        onStopPreview = {},
        diagnostics = emptyList(),
        previewOnly = true,
    )
}

@Composable
internal fun AudioVideoDevicesCardContent(
    voiceState: ComposeMediaVoiceState,
    videoState: ComposeExperimentalVideoState,
    onRefresh: () -> Unit,
    onMicrophoneSelected: (String?) -> Unit,
    onSpeakerSelected: (String?) -> Unit,
    onCameraSelected: (String?) -> Unit,
    onTestMicrophone: () -> Unit,
    onTestSpeaker: () -> Unit,
    onTestCamera: () -> Unit,
    onStartPreview: () -> Unit,
    onStopPreview: () -> Unit,
    diagnostics: List<String>,
    previewOnly: Boolean = false,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.background) {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Device selections apply to the next call; active calls are preserved.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                }
                CompactButton(
                    onClick = onRefresh,
                    enabled = !previewOnly && voiceState.canRefreshDevices,
                ) {
                    Text("Refresh devices")
                }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 620.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MicrophoneSettingsSection(
                            voiceState = voiceState,
                            previewOnly = previewOnly,
                            onMicrophoneSelected = onMicrophoneSelected,
                            onTestMicrophone = onTestMicrophone,
                            modifier = Modifier.weight(1f),
                        )
                        SpeakerSettingsSection(
                            voiceState = voiceState,
                            previewOnly = previewOnly,
                            onSpeakerSelected = onSpeakerSelected,
                            onTestSpeaker = onTestSpeaker,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MicrophoneSettingsSection(
                            voiceState = voiceState,
                            previewOnly = previewOnly,
                            onMicrophoneSelected = onMicrophoneSelected,
                            onTestMicrophone = onTestMicrophone,
                        )
                        SpeakerSettingsSection(
                            voiceState = voiceState,
                            previewOnly = previewOnly,
                            onSpeakerSelected = onSpeakerSelected,
                            onTestSpeaker = onTestSpeaker,
                        )
                    }
                }
            }
            DeviceSettingsSection(
                title = "Camera · Experimental",
            ) {
                DeviceChoiceDropdown(
                    inline = true,
                    label = "Choose camera",
                    choices = videoState.cameras,
                    selected = videoState.selectedCamera,
                    enabled = !previewOnly,
                    onSelected = onCameraSelected,
                    helperText = videoState.cameraEmptyState,
                )
                CameraPreviewStatus(state = videoState)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(videoState.previewStateLabel)
                    CompactButton(
                        onClick = onTestCamera,
                        enabled = !previewOnly && videoState.canTestCamera
                    ) { Text("Test camera") }
                    CompactButton(onClick = onStartPreview, enabled = !previewOnly && videoState.canStartPreview) {
                        Text(
                            videoState.startPreviewLabel
                        )
                    }
                    CompactButton(onClick = onStopPreview, enabled = !previewOnly && videoState.canStopPreview) {
                        Text(
                            videoState.stopPreviewLabel
                        )
                    }
                }
                Text(
                    videoState.cameraTestStatus,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            ComposeAdvancedPane("Device access and troubleshooting") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(voiceState.permissionStatusLabel, style = MaterialTheme.typography.caption)
                    Text(videoState.permissionStatusLabel, style = MaterialTheme.typography.caption)
                    HelpNotice("If access is blocked", "Allow camera and microphone access in operating-system privacy settings, close other apps using the device, then refresh devices.")
                }
            }
            diagnostics.takeLast(3).forEach {
                Text(
                    "• $it",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
                )
            }
        }
    }
}

@Composable
private fun MicrophoneSettingsSection(
    voiceState: ComposeMediaVoiceState,
    previewOnly: Boolean,
    onMicrophoneSelected: (String?) -> Unit,
    onTestMicrophone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DeviceSettingsSection(
        title = "Microphone",
        modifier = modifier,
    ) {
        DeviceChoiceDropdown(
                    inline = true,
            label = "Input device",
            choices = voiceState.microphones,
            selected = voiceState.selectedMicrophone,
            enabled = !previewOnly,
            onSelected = onMicrophoneSelected,
            helperText = voiceState.microphoneEmptyState,
        )
        AudioInputLevelMeter(percent = voiceState.localAudioPercent, label = voiceState.localAudioLabel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            CompactButton(
                onClick = onTestMicrophone,
                enabled = !previewOnly && voiceState.canTestMicrophone,
            ) {
                Text("Test microphone")
            }
            Text(
                voiceState.microphoneTestStatus,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun SpeakerSettingsSection(
    voiceState: ComposeMediaVoiceState,
    previewOnly: Boolean,
    onSpeakerSelected: (String?) -> Unit,
    onTestSpeaker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DeviceSettingsSection(
        title = "Speakers",
        modifier = modifier,
    ) {
        DeviceChoiceDropdown(
                    inline = true,
            label = "Output device",
            choices = voiceState.outputDevices,
            selected = voiceState.selectedOutputDevice,
            enabled = !previewOnly,
            onSelected = onSpeakerSelected,
            helperText = voiceState.outputEmptyState,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            CompactButton(
                onClick = onTestSpeaker,
                enabled = !previewOnly && voiceState.canTestSpeaker,
            ) {
                Text("Test speakers")
            }
            Text(
                voiceState.speakerTestStatus,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun DeviceSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = LocalSecureLanDesignTokens.current.colors.surfaceLevel2,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            content()
        }
    }
}

@Composable
internal fun AudioInputLevelMeter(percent: Int, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Input level", style = MaterialTheme.typography.caption)
            Text("$percent%", style = MaterialTheme.typography.caption)
        }
        LinearProgressIndicator(progress = percent.coerceIn(0, 100) / 100f, modifier = Modifier.fillMaxWidth())
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
        )
    }
}

@Composable
internal fun CameraPreviewStatus(state: ComposeExperimentalVideoState) {
    Surface(
        color = MaterialTheme.colors.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val previewImage = rememberPreviewImage(state.latestPreviewFrame)
            if (previewImage != null) {
                Image(
                    bitmap = previewImage,
                    contentDescription = "Live camera preview from ${state.selectedCamera}",
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                VideoSurfacePlaceholder(
                    title = state.previewStateLabel,
                    body = state.previewActionHint,
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                )
            }
            ComposeAdvancedPane("Preview details") {
                Column {
                    Text(state.previewStatus, style = MaterialTheme.typography.caption)
                    Text(state.frameCaption, style = MaterialTheme.typography.caption)
                    Text(state.previewConfigurationLabel, style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}

@Composable
internal fun HelpNotice(title: String, body: String) {
    Surface(
        color = MaterialTheme.colors.secondary.copy(alpha = 0.10f),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Text(
                body,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}
