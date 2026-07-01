package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone

@Composable
internal fun ChatCallActions(
    hostAdapter: ComposeDesktopHostAdapter,
    selectedPeer: ComposePeerListItem?,
) {
    val voiceEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.voiceCapable
    val videoEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.videoCapable
    val hangUpEnabled = selectedPeer?.realtimeCapable == true &&
            (hostAdapter.mediaVoiceState.canHangUp || hostAdapter.experimentalVideoState.canHangUp)

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
    ) { Text("Voice") }
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
    ) { Text("Video") }
    CompactButton(
        onClick = hostAdapter::closeRealtimeSession,
        modifier = Modifier.defaultMinSize(minWidth = 0.dp),
        enabled = hangUpEnabled,
        tone = CompactButtonTone.DESTRUCTIVE,
    ) { Text("End") }
}
