package com.shterneregen.securelan.desktop.compose.state.peer

/**
 * JavaFX → Compose mapping for the Actions-column selected-peer header:
 * selectedPeerTitleValue / selectedPeerMetaValue become deterministic selected-peer summary copy;
 * Attach / Voice call / Video call / End call become quick-action readiness flags;
 * JavaFX disabled states become explicit enabled flags plus blocked reasons.
 */
public data class ComposeSelectedPeerQuickActionsState(
    val peerListState: ComposePeerListState,
    val clientConnected: Boolean,
    val voiceRuntimeReady: Boolean = true,
    val videoRuntimeReady: Boolean = true,
    val hangUpReady: Boolean = false,
    val javaFxFallbackAvailable: Boolean = peerListState.javaFxFallbackAvailable,
) {
    val title: String = peerListState.selectedPeerTitle
    val meta: String = peerListState.selectedPeerMeta
    val selectedPeer: ComposePeerListItem? = peerListState.selectedPeer
    val selectedPeerStatus: String = peerListState.peerStatus
    val fileTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.fileCapable
    val voiceTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.voiceCapable
    val videoTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.videoCapable
    val realtimeTargetReady: Boolean = selectedPeer?.online == true && selectedPeer.realtimeCapable
    val attachEnabled: Boolean = clientConnected && fileTargetReady && javaFxFallbackAvailable
    val voiceEnabled: Boolean = clientConnected && voiceTargetReady && voiceRuntimeReady && javaFxFallbackAvailable
    val videoEnabled: Boolean = clientConnected && videoTargetReady && videoRuntimeReady && javaFxFallbackAvailable
    val hangUpEnabled: Boolean = hangUpReady && realtimeTargetReady && javaFxFallbackAvailable
    val attachLabel: String = if (attachEnabled) "Attach ready" else "Attach blocked"
    val voiceLabel: String = if (voiceEnabled) "Voice call ready" else "Voice call blocked"
    val videoLabel: String = if (videoEnabled) "Video call ready" else "Video call blocked"
    val hangUpLabel: String = if (hangUpEnabled) "End call ready" else "End call blocked"
    val enabledActionLabels: List<String> = buildList {
        if (attachEnabled) add("Attach")
        if (voiceEnabled) add("Voice call")
        if (videoEnabled) add("Video call")
        if (hangUpEnabled) add("End call")
    }
    val readinessLabel: String = if (enabledActionLabels.isEmpty()) {
        "Quick actions blocked"
    } else {
        "Ready: ${enabledActionLabels.joinToString()}"
    }
    val actionSummary: String = selectedPeer?.let { peer ->
        if (clientConnected) {
            "Quick actions ready for ${peer.nickname}."
        } else {
            "Selected ${peer.nickname}. Connect to chat before sending files or starting calls."
        }
    } ?: "No peer selected. Choose an online peer to send files or start a call."
    val blockedReasons: List<String> = buildList {
        if (selectedPeer == null) {
            add("Select an online peer before using quick actions.")
        } else {
            if (!selectedPeer.online) {
                add("Selected peer is offline; wait for discovery or presence refresh.")
            }
            if (selectedPeer.online && !selectedPeer.fileCapable) {
                add("Attach is blocked until a file receiver endpoint is available.")
            }
            if (selectedPeer.online && !selectedPeer.voiceCapable) {
                add("Voice call is blocked because the peer does not advertise voice support.")
            }
            if (selectedPeer.online && !selectedPeer.videoCapable) {
                add("Video call is blocked because the peer does not advertise video support.")
            }
        }
        if (!clientConnected) {
            add("Connect to chat before sending files or starting calls.")
        }
        if (clientConnected && voiceTargetReady && !voiceRuntimeReady) {
            add("Voice is not ready yet; wait before calling.")
        }
        if (clientConnected && videoTargetReady && !videoRuntimeReady) {
            add("Video is not ready yet; wait before calling.")
        }
        if (!javaFxFallbackAvailable) {
            add("JavaFX fallback is unavailable; keep live Compose quick actions disabled.")
        }
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Quick actions ready for ${selectedPeer?.nickname ?: "the selected peer"}."
    } else {
        blockedReasons.joinToString(" · ")
    }
    val buttonReadinessSummary: String = listOf(attachLabel, voiceLabel, videoLabel, hangUpLabel).joinToString(" · ")
}
