package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerListTransitionPlan(
    val transitions: List<ComposePeerListTransitionIntent>,
) {
    val title: String = "Peer-list transition intents"
    val enabledTransitions: List<ComposePeerListTransitionIntent> = transitions.filter { it.enabled }
    val blockedTransitions: List<ComposePeerListTransitionIntent> = transitions.filterNot { it.enabled }
    val enabledSummary: String = if (enabledTransitions.isEmpty()) {
        "No peer-list transitions are ready for future live Compose wiring."
    } else {
        "Ready transitions: ${enabledTransitions.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedTransitions.isEmpty()) {
        "No peer-list transitions are blocked in this preview state."
    } else {
        "Blocked transitions: ${blockedTransitions.joinToString { it.label }}"
    }
    val sideEffectSummary: String = transitions.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    companion object {
        fun from(
            state: ComposePeerListState,
            lifecyclePlan: ComposePeerListLifecyclePlan = state.peerListLifecyclePlan,
            controlPlan: ComposePeerTargetControlPlan = state.targetControlPlan,
        ): ComposePeerListTransitionPlan {
            val source = lifecyclePlan.currentState
            val fallbackBlock = "JavaFX peer-list fallback is unavailable; transition intents must remain blocked."
            fun blocked(reason: String): String = if (!lifecyclePlan.fallbackAvailable) fallbackBlock else reason

            return ComposePeerListTransitionPlan(
                listOf(
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.SELECT_PEER,
                        label = "Select peer",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_SELECTED,
                        enabled = lifecyclePlan.fallbackAvailable && state.visiblePeers.isNotEmpty(),
                        guardSummary = "Peer selection requires at least one visible peer in the list.",
                        blockedReason = blocked("No visible peers; select-peer transition must remain blocked."),
                        sideEffectContract = "future implementation may update selected-peer index without opening connections",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.DESELECT_PEER,
                        label = "Deselect peer",
                        sourceState = source,
                        targetState = if (state.visiblePeers.isNotEmpty()) ComposePeerListLifecycleState.PEERS_VISIBLE else ComposePeerListLifecycleState.IDLE,
                        enabled = lifecyclePlan.fallbackAvailable && state.selectedPeer != null,
                        guardSummary = "Deselection clears the selected peer and returns to the visible-peers or idle state.",
                        blockedReason = blocked("No peer is currently selected; deselect transition must remain blocked."),
                        sideEffectContract = "future implementation may clear selected-peer index without affecting discovery",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_CHAT,
                        label = "Target peer for chat",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.chatReady,
                        guardSummary = "Chat targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set chat target while JavaFX owns shared-room messaging",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_FILE,
                        label = "Target peer for file transfer",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.fileReady,
                        guardSummary = "File transfer targeting requires an online discovered peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set encrypted file-transfer target without invoking transfer",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_VOICE,
                        label = "Target peer for voice",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.voiceReady,
                        guardSummary = "Voice targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.VOICE_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set voice target while WebRTC runtime stays JavaFX-owned",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_VIDEO,
                        label = "Target peer for video",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.videoReady,
                        guardSummary = "Experimental video targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.VIDEO_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set experimental video target while preserving fallback diagnostics",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.TARGET_PEER_FOR_DATA,
                        label = "Target peer for RTC data",
                        sourceState = source,
                        targetState = ComposePeerListLifecycleState.PEER_TARGETED,
                        enabled = lifecyclePlan.fallbackAvailable && state.targetActions.dataChannelReady,
                        guardSummary = "RTC data-channel targeting requires an online selected peer.",
                        blockedReason = blocked(controlPlan.command(ComposePeerTargetCommandKind.DATA_TARGET).blockedReason),
                        sideEffectContract = "future implementation may set data-channel target while signaling stays routed through chat-core",
                    ),
                    ComposePeerListTransitionIntent(
                        kind = ComposePeerListTransitionKind.REFRESH_PEER_LIST,
                        label = "Refresh peer list",
                        sourceState = source,
                        targetState = if (state.visiblePeers.isNotEmpty()) ComposePeerListLifecycleState.PEERS_VISIBLE else ComposePeerListLifecycleState.DISCOVERING,
                        enabled = lifecyclePlan.fallbackAvailable,
                        guardSummary = "Peer-list refresh re-evaluates visible peers without starting discovery.",
                        blockedReason = blocked("JavaFX fallback is unavailable; peer-list refresh must remain blocked."),
                        sideEffectContract = "future implementation may trigger a discovery scan through JavaFX-owned discovery service",
                    ),
                ),
            )
        }
    }
}
