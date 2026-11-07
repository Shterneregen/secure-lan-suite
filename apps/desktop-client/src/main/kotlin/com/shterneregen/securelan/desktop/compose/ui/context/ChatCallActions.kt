package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.runtime.Composable
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.ui.shell.RoomHeaderAction

@Composable
internal fun ChatCallActions(
    hostAdapter: ComposeDesktopHostAdapter,
    selectedPeer: ComposePeerListItem?,
) {
    val voiceEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.voiceCapable
    val videoEnabled = hostAdapter.chatConnected && selectedPeer?.online == true && selectedPeer.videoCapable
    val sessionActive = hostAdapter.mediaVoiceState.currentSession != null ||
        hostAdapter.experimentalVideoState.currentSession != null
    val hangUpEnabled = hostAdapter.mediaVoiceState.canHangUp || hostAdapter.experimentalVideoState.canHangUp

    if (!sessionActive) {
        RoomHeaderAction(
            label = "Voice call", icon = SecureLanIcons.Voice,
            onClick = {
                selectedPeer?.let { peer ->
                    hostAdapter.startRealtimeSession(
                        hostAdapter.statusState.nickname,
                        peer.nickname,
                        RtcSessionMode.AUDIO
                    )
                }
            },
            enabled = voiceEnabled,
        )
        RoomHeaderAction(
            label = "Video call", icon = SecureLanIcons.Video,
            onClick = {
                selectedPeer?.let { peer ->
                    hostAdapter.startRealtimeSession(
                        hostAdapter.statusState.nickname,
                        peer.nickname,
                        RtcSessionMode.AUDIO_VIDEO
                    )
                }
            },
            enabled = videoEnabled,
        )
    } else {
        RoomHeaderAction(
            label = "End call", icon = SecureLanIcons.CallEnd,
            onClick = hostAdapter::closeRealtimeSession,
            enabled = hangUpEnabled,
            destructive = true,
        )
    }
}
