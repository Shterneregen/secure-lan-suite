package com.shterneregen.securelan.desktop.compose.state.media

import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.desktop.ui.DesktopRealtimeFormatters
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot

data class ComposeMediaVoiceState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val microphones: List<MediaDeviceChoice> = listOf(MediaDeviceChoice.systemDefault("System default microphone")),
    val outputDevices: List<MediaDeviceChoice> = listOf(MediaDeviceChoice.systemDefault("System default speaker")),
    val selectedMicrophoneId: String = "",
    val selectedOutputDeviceId: String = "",
    val runtimeStatus: RtcRuntimeStatus? = null,
    val currentSession: RtcSessionSnapshot? = null,
    val localAudioLevel: Double = 0.0,
    val remoteAudioLevel: Double = 0.0,
    val microphoneTestStatus: String = "Not tested",
    val speakerTestStatus: String = "Not tested",
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Media devices and voice"
    val selectedPeer = peerListState.selectedPeer
    val selectedPeerName: String = selectedPeer?.nickname ?: "No peer selected"
    val selectedMicrophone: MediaDeviceChoice = microphones.firstOrNull { it.matches(selectedMicrophoneId) }
        ?: microphones.firstOrNull()
        ?: MediaDeviceChoice.systemDefault("System default microphone")
    val selectedOutputDevice: MediaDeviceChoice = outputDevices.firstOrNull { it.matches(selectedOutputDeviceId) }
        ?: outputDevices.firstOrNull()
        ?: MediaDeviceChoice.systemDefault("System default speaker")
    val physicalMicrophoneCount: Int = microphones.count { !it.systemDefault }
    val physicalOutputDeviceCount: Int = outputDevices.count { !it.systemDefault }
    val runtimeLabel: String = DesktopRealtimeFormatters.formatRuntimeStatus(runtimeStatus)
    val permissionStatusLabel: String = when {
        runtimeStatus?.available == true -> "Microphone permission: ready to test"
        runtimeStatus == null -> "Microphone permission: not checked yet"
        else -> "Microphone permission: unavailable"
    }
    val microphoneEmptyState: String = if (physicalMicrophoneCount == 0) {
        "No microphones found. Connect one and refresh devices."
    } else {
        "$physicalMicrophoneCount microphone option${if (physicalMicrophoneCount == 1) "" else "s"} available."
    }
    val outputEmptyState: String = if (physicalOutputDeviceCount == 0) {
        "No speaker list available. Calls use the system default."
    } else {
        "$physicalOutputDeviceCount speaker option${if (physicalOutputDeviceCount == 1) "" else "s"} available."
    }
    val voiceState: RtcSessionState = currentSession?.state ?: RtcSessionState.IDLE
    val voiceStatusText: String = DesktopRealtimeFormatters.voiceStatusText(voiceState, currentSession?.remotePeer ?: selectedPeer?.nickname)
    val callTransitionLabel: String = when (voiceState) {
        RtcSessionState.CONNECTED -> "Voice connected"
        RtcSessionState.NEGOTIATING, RtcSessionState.CONNECTING -> "Connecting voice…"
        RtcSessionState.CLOSING, RtcSessionState.CLOSED -> "Voice disconnected"
        RtcSessionState.FAILED -> "Voice failed"
        else -> "Voice idle"
    }
    val localAudioLabel: String = DesktopRealtimeFormatters.formatAudioLevel(localAudioLevel > 0.01, true, selectedPeer?.nickname, localAudioLevel)
    val remoteAudioLabel: String = DesktopRealtimeFormatters.formatAudioLevel(remoteAudioLevel > 0.01, false, selectedPeer?.nickname, remoteAudioLevel)
    val localAudioPercent: Int = (localAudioLevel.coerceIn(0.0, 1.0) * 100).toInt()
    val voiceTargetReady: Boolean = statusState.clientConnected && selectedPeer?.online == true && selectedPeer.voiceCapable
    val canRefreshDevices: Boolean = javaFxFallbackAvailable
    val canTestMicrophone: Boolean = microphones.isNotEmpty() && javaFxFallbackAvailable
    val canTestSpeaker: Boolean = outputDevices.isNotEmpty() && javaFxFallbackAvailable
    val canStartVoice: Boolean = voiceTargetReady && javaFxFallbackAvailable
    val canHangUp: Boolean = DesktopMainViewHelpers.hangUpAvailable(currentSession?.state) && javaFxFallbackAvailable
    val startVoiceLabel: String = if (canStartVoice) "Start voice call" else "Voice call blocked"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX voice workspace remains production fallback" else "JavaFX voice fallback unavailable"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) add("Connect to chat before starting a voice call.")
        if (selectedPeer == null) add("Select an online peer before starting voice.") else if (!selectedPeer.online) add("Selected peer is offline; voice must remain blocked.") else if (!selectedPeer.voiceCapable) add("Selected peer does not advertise voice support; voice must remain blocked.")
        if (!javaFxFallbackAvailable) add("JavaFX fallback is unavailable; keep live Compose voice actions disabled.")
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Voice controls are ready."
    } else {
        blockedReasons.joinToString(" · ")
    }
}
