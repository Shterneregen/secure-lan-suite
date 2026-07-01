package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerListLifecyclePlan(
    val currentState: ComposePeerListLifecycleState,
    val steps: List<ComposePeerListLifecycleStep>,
    val blockedReasons: List<String>,
    val fallbackAvailable: Boolean,
    val selectedPeerLabel: String,
) {
    val title: String = "Live peer-list binding contract"
    val stateLabel: String = currentState.name.lowercase().replace('_', '/')
    val readySteps: List<ComposePeerListLifecycleStep> = steps.filter { it.ready }
    val readinessSummary: String = if (readySteps.isEmpty()) {
        "No live peer-list lifecycle steps are ready for Compose wiring."
    } else {
        "Ready lifecycle steps: ${readySteps.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedReasons.isEmpty()) {
        "No peer-list lifecycle blockers; service calls remain intentionally deferred."
    } else {
        blockedReasons.joinToString(" · ")
    }
    val fallbackStatus: String = if (fallbackAvailable) {
        "JavaFX peer-list fallback available"
    } else {
        "JavaFX peer-list fallback unavailable; live Compose peer binding must remain blocked"
    }
    val sideEffectContractSummary: String = steps.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    companion object {
        fun from(state: ComposePeerListState): ComposePeerListLifecyclePlan {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val selectedPeer = state.selectedPeer
            val hasPeers = state.visiblePeers.isNotEmpty()
            val hasOnlinePeer = selectedPeer?.online == true
            val selectedPeerLabel = selectedPeer?.nickname ?: "no peer selected"

            val blockers = buildList {
                if (!fallbackAvailable) {
                    add("JavaFX peer-list fallback is unavailable; live Compose peer binding must remain blocked.")
                }
            }

            val currentState = when {
                blockers.isNotEmpty() -> ComposePeerListLifecycleState.BLOCKED_ERROR
                hasOnlinePeer && state.targetActions.chatReady -> ComposePeerListLifecycleState.PEER_TARGETED
                hasOnlinePeer -> ComposePeerListLifecycleState.PEER_SELECTED
                hasPeers -> ComposePeerListLifecycleState.PEERS_VISIBLE
                fallbackAvailable -> ComposePeerListLifecycleState.DISCOVERING
                else -> ComposePeerListLifecycleState.BLOCKED_ERROR
            }

            val steps = listOf(
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.IDLE,
                    ready = currentState == ComposePeerListLifecycleState.IDLE && blockers.isEmpty(),
                    label = "Idle",
                    sideEffectContract = "observe peer-list preview state only; do not subscribe to discovery or chat events",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.DISCOVERING,
                    ready = fallbackAvailable && blockers.isEmpty() && !hasOnlinePeer,
                    label = "Discovering",
                    sideEffectContract = "reflect discovery/listening state; an empty peer list is normal while JavaFX remains discovery owner",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.PEERS_VISIBLE,
                    ready = fallbackAvailable && hasPeers,
                    label = "Peers visible",
                    sideEffectContract = "display visible peer list without subscribing to discovery refresh",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.PEER_SELECTED,
                    ready = fallbackAvailable && hasOnlinePeer,
                    label = "Peer selected",
                    sideEffectContract = "reflect selected-peer metadata and target-action readiness; do not connect or start RTC",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.PEER_TARGETED,
                    ready = fallbackAvailable && hasOnlinePeer && state.targetActions.chatReady,
                    label = "Peer targeted",
                    sideEffectContract = "peer is selected and target actions are ready; JavaFX still owns live targeting and runtime actions",
                ),
                ComposePeerListLifecycleStep(
                    state = ComposePeerListLifecycleState.BLOCKED_ERROR,
                    ready = blockers.isNotEmpty(),
                    label = "Blocked/error",
                    sideEffectContract = "surface validation or fallback blockers before any live peer-list binding can run",
                ),
            )

            return ComposePeerListLifecyclePlan(
                currentState = currentState,
                steps = steps,
                blockedReasons = blockers,
                fallbackAvailable = fallbackAvailable,
                selectedPeerLabel = selectedPeerLabel,
            )
        }
    }
}
