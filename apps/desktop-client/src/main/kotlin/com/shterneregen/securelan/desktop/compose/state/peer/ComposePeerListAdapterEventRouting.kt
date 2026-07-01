package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerListAdapterEventRouting(
    val contracts: List<ComposePeerListAdapterEventContract>,
    val fallbackAvailable: Boolean,
    val readyEvents: List<ComposePeerListAdapterEventContract>,
    val blockedEvents: List<ComposePeerListAdapterEventContract>,
) {
    val title: String = "Peer-list adapter event contract"
    val subtitle: String =
        "Side-effect-free event contract for future live peer-list integration; JavaFX still owns discovery and selection."
    val readyCount: Int = readyEvents.size
    val blockedCount: Int = blockedEvents.size
    val totalCount: Int = contracts.size
    val readinessSummary: String = when {
        readyCount == totalCount -> "All $totalCount peer-list adapter events are ready for future live wiring."
        blockedCount == totalCount -> "All $totalCount peer-list adapter events are blocked; JavaFX discovery must remain active."
        else -> "$readyCount of $totalCount peer-list events ready; $blockedCount blocked."
    }
    val fallbackStatus: String =
        if (fallbackAvailable) "JavaFX fallback available; adapter event routing is speculative." else "JavaFX fallback unavailable; live adapter event routing must remain blocked."
    val blockedSummary: String = if (blockedEvents.isEmpty()) {
        "No peer-list adapter events are blocked."
    } else {
        blockedEvents.joinToString(" · ") { "${it.readinessLabel}: ${it.blockedReason}" }
    }
    val eventOrderLabel: String = contracts.joinToString(" → ") { it.routeTag }

    companion object {
        fun from(state: ComposePeerListState): ComposePeerListAdapterEventRouting {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val fallbackBlock =
                "JavaFX fallback is unavailable; live peer-list adapter event routing must remain blocked."

            fun block(reason: String): String = if (!fallbackAvailable) fallbackBlock else reason

            val hasPeers = state.visiblePeers.isNotEmpty()
            val selectedPeer = state.selectedPeer
            val hasSelected = selectedPeer != null
            val selectedOnline = selectedPeer?.online == true

            val peerDiscoveredReady = fallbackAvailable && hasPeers
            val peerLostReady = fallbackAvailable && hasPeers
            val peerSelectedReady = fallbackAvailable && hasSelected && selectedOnline
            val peerDeselectedReady = fallbackAvailable && hasSelected
            val peerTargetChangedReady = fallbackAvailable && hasSelected && selectedOnline
            val peerListRefreshedReady = fallbackAvailable

            return ComposePeerListAdapterEventRouting(
                contracts = listOf(
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_DISCOVERED,
                        label = "Peer discovered",
                        ready = peerDiscoveredReady,
                        guarded = !peerDiscoveredReady && fallbackAvailable,
                        description = "Future runtime adapter fires when a new LAN peer is discovered and added to the visible list.",
                        prerequisites = listOf("visible peers exist"),
                        blockedReason = block("No peers are visible; peer-discovered event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_LOST,
                        label = "Peer lost",
                        ready = peerLostReady,
                        guarded = !peerLostReady && fallbackAvailable,
                        description = "Future runtime adapter fires when a peer goes offline or is removed from the visible list.",
                        prerequisites = listOf("visible peers exist"),
                        blockedReason = block("No peers are visible; peer-lost event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_SELECTED,
                        label = "Peer selected",
                        ready = peerSelectedReady,
                        guarded = !peerSelectedReady && fallbackAvailable,
                        description = "Future runtime adapter fires when the user selects an online peer from the list.",
                        prerequisites = listOf("online peer selected"),
                        blockedReason = block("No online peer is selected; peer-selected event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_DESELECTED,
                        label = "Peer deselected",
                        ready = peerDeselectedReady,
                        guarded = !peerDeselectedReady && fallbackAvailable,
                        description = "Future runtime adapter fires when the user deselects the current peer.",
                        prerequisites = listOf("a peer is currently selected"),
                        blockedReason = block("No peer is currently selected; peer-deselected event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_TARGET_CHANGED,
                        label = "Peer target changed",
                        ready = peerTargetChangedReady,
                        guarded = !peerTargetChangedReady && fallbackAvailable,
                        description = "Future runtime adapter fires when target actions (chat/file/voice/video/data) are set for the selected peer.",
                        prerequisites = listOf("online peer selected"),
                        blockedReason = block("No online peer is selected; peer-target-changed event cannot fire."),
                    ),
                    ComposePeerListAdapterEventContract(
                        kind = ComposePeerListAdapterEventKind.PEER_LIST_REFRESHED,
                        label = "Peer list refreshed",
                        ready = peerListRefreshedReady,
                        guarded = true,
                        description = "Future runtime adapter fires after discovery produces updated peer presence; it is guarded and fires only on actual refresh.",
                        prerequisites = listOf("discovery refresh triggered"),
                        blockedReason = block("JavaFX fallback is unavailable; peer-list refresh is suspended."),
                    ),
                ),
                fallbackAvailable = fallbackAvailable,
                readyEvents = emptyList(),
                blockedEvents = emptyList(),
            ).let { routing ->
                routing.copy(
                    readyEvents = routing.contracts.filter { it.ready },
                    blockedEvents = routing.contracts.filterNot { it.ready },
                )
            }
        }
    }
}
