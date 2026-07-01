package com.shterneregen.securelan.desktop.compose.state.peer

import com.shterneregen.securelan.desktop.compose.state.shell.ComposeEmptyStateVisualWeight

public data class ComposePeerListState(
    val peers: List<ComposePeerListItem> = ComposePeerListItem.defaultPreviewItems(clientConnected = false),
    val selectedPeerIndex: Int = 0,
    val selectedPeerNickname: String? = null,
    val selectedTargetKind: ComposePeerTargetCommandKind? = null,
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Contacts / Peers"
    val hint: String = "Discovered peers appear here. Select one to chat, send files, or call."
    val visiblePeers: List<ComposePeerListItem> = peers.sortedWith(
        compareByDescending<ComposePeerListItem> { it.online }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nickname },
    )
    val onlinePeers: List<ComposePeerListItem> = visiblePeers.filter { it.online }
    val offlinePeers: List<ComposePeerListItem> = visiblePeers.filterNot { it.online }
    val visiblePeerRows: List<ComposePeerListItemPresentation> = visiblePeers.map(ComposePeerListItemPresentation::from)
    val peerSections: List<ComposePeerListSectionPresentation> = listOf(
        ComposePeerListSectionPresentation(
            key = "online",
            title = "Online",
            availability = ComposePeerAvailabilityKind.ONLINE,
            rows = visiblePeerRows.filter { it.availability == ComposePeerAvailabilityKind.ONLINE },
        ),
        ComposePeerListSectionPresentation(
            key = "offline",
            title = "Offline",
            availability = ComposePeerAvailabilityKind.OFFLINE,
            rows = visiblePeerRows.filter { it.availability == ComposePeerAvailabilityKind.OFFLINE },
        ),
    ).filter { it.rows.isNotEmpty() }
    val peerGroupingSummary: String = when {
        visiblePeers.isEmpty() -> "No people visible"
        offlinePeers.isEmpty() -> "${onlinePeers.size} online"
        onlinePeers.isEmpty() -> "${offlinePeers.size} offline"
        else -> "${onlinePeers.size} online · ${offlinePeers.size} offline"
    }
    val hasAnyPeers: Boolean = visiblePeers.isNotEmpty()
    val emptyStateTitle: String = "No peers visible yet"
    val emptyStateSituation: String = emptyStateTitle
    val emptyStateExplanation: String = "People appear here once you join a room."
    val emptyStateNextAction: String = "Open or join a room"
    val emptyStateDetail: String = "$emptyStateExplanation Use Advanced connection if a peer is hidden."
    val emptyStateActionLabel: String = emptyStateNextAction
    val emptyStateStructuredCopy: List<String> = listOf(emptyStateSituation, emptyStateExplanation, emptyStateNextAction)
    val emptyStateVisualWeight: ComposeEmptyStateVisualWeight = ComposeEmptyStateVisualWeight.SUPPORTING
    val emptyStateKeepsConversationDominant: Boolean = true
    val resolvedSelectedPeerIndex: Int = resolveSelectedPeerIndex()
    val selectedPeer: ComposePeerListItem? = visiblePeers.getOrNull(resolvedSelectedPeerIndex)
    val selectedPeerTitle: String = selectedPeer?.nickname ?: "No peer selected"
    val selectedPeerMeta: String =
        selectedPeer?.selectedMeta ?: "Select an online peer to send files or call."
    val peerStatus: String =
        selectedPeer?.let { if (it.online) "Peer ${it.nickname}" else "Peer offline" } ?: "Peer not selected"
    val noPeerActionTitle: String = "Choose a peer to start"
    val noPeerActionDetail: String =
        "Select an online peer to start messaging, files, or calls. Other tools stay hidden until you need them."
    val actionSummary: String = selectedTargetKind?.let { kind ->
        selectedPeer?.let { "${kind.displayName} target selected for ${it.nickname}. Available once the connection is ready." }
    } ?: selectedPeer?.actionSummary ?: "Select an online peer to send messages, files, or start calls."
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX peer list remains production fallback" else "JavaFX peer list fallback unavailable"
    val targetActions: ComposePeerTargetActions = ComposePeerTargetActions.from(selectedPeer)
    val targetControlPlan: ComposePeerTargetControlPlan =
        ComposePeerTargetControlPlan.from(selectedPeer, targetActions, javaFxFallbackAvailable, selectedTargetKind)
    val peerListLifecyclePlan: ComposePeerListLifecyclePlan = ComposePeerListLifecyclePlan.from(this)
    val peerListTransitionPlan: ComposePeerListTransitionPlan =
        ComposePeerListTransitionPlan.from(this, peerListLifecyclePlan, targetControlPlan)
    val peerListAdapterEventRouting: ComposePeerListAdapterEventRouting = ComposePeerListAdapterEventRouting.from(this)

    fun selectionKeyFor(index: Int): String? = visiblePeers.getOrNull(index)?.nickname

    private fun resolveSelectedPeerIndex(): Int {
        val indexByNickname = selectedPeerNickname?.let { selectedNickname ->
            visiblePeers.indexOfFirst { it.nickname.equals(selectedNickname, ignoreCase = true) }
        } ?: -1
        return when {
            indexByNickname >= 0 -> indexByNickname
            selectedPeerIndex in visiblePeers.indices -> selectedPeerIndex
            else -> -1
        }
    }
}
