package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun ChatCallActions(
    hostAdapter: ComposeDesktopHostAdapter,
    selectedPeer: ComposePeerListItem?,
) {
    val voiceEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.voiceCapable
    val videoEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.videoCapable
    val sessionActive = hostAdapter.mediaVoiceState.currentSession != null ||
        hostAdapter.experimentalVideoState.currentSession != null
    val hangUpEnabled = selectedPeer?.realtimeCapable == true &&
            (hostAdapter.mediaVoiceState.canHangUp || hostAdapter.experimentalVideoState.canHangUp)

    if (!sessionActive) {
        CompactButton(
            onClick = {
                selectedPeer?.let { peer ->
                    hostAdapter.startRealtimeSession(
                        hostAdapter.statusState.nickname,
                        peer.nickname,
                        RtcSessionMode.AUDIO
                    )
                }
            },
            modifier = Modifier.defaultMinSize(minWidth = 0.dp),
            enabled = voiceEnabled,
        ) {
            CallActionLabel(icon = SecureLanIcons.Voice, label = "Voice call")
        }
        CompactButton(
            onClick = {
                selectedPeer?.let { peer ->
                    hostAdapter.startRealtimeSession(
                        hostAdapter.statusState.nickname,
                        peer.nickname,
                        RtcSessionMode.AUDIO_VIDEO
                    )
                }
            },
            modifier = Modifier.defaultMinSize(minWidth = 0.dp),
            enabled = videoEnabled,
        ) {
            CallActionLabel(icon = SecureLanIcons.Video, label = "Video call")
        }
    } else {
        CompactButton(
            onClick = hostAdapter::closeRealtimeSession,
            modifier = Modifier.defaultMinSize(minWidth = 0.dp),
            enabled = hangUpEnabled,
            tone = CompactButtonTone.DESTRUCTIVE,
        ) {
            CallActionLabel(icon = SecureLanIcons.CallEnd, label = "End call")
        }
    }
}

@Composable
private fun CallActionLabel(
    icon: ImageVector,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label)
    }
}
