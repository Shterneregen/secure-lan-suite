package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState

data class ComposeContextPanelState(
    val mode: RightPanelMode,
    val cards: List<ComposeContextPanelCard>,
) {
    val title: String = "Context Assistant"
    private val visibleCardLimit: Int = MAX_VISIBLE_CARDS
    val visibleCards: List<ComposeContextPanelCard> = cards.take(visibleCardLimit)
    val primaryCards: List<ComposeContextPanelCard> = visibleCards.filter { it.primary }
    val primaryButtons: List<String> = visibleCards.mapNotNull { it.primaryAction }
    val visibleCardTitles: List<String> = visibleCards.map { it.title }
    val visibleCardKinds: List<ComposeContextPanelCardKind> = visibleCards.map { it.kind }
    val persistentToolKinds: Set<ComposeContextPanelCardKind> =
        setOf(ComposeContextPanelCardKind.TRANSFER_DETAILS)
    val keepsPersistentToolsVisible: Boolean = persistentToolKinds.all(visibleCardKinds::contains)
    val nextActionSummary: String =
        primaryCards.firstOrNull()?.body ?: visibleCards.firstOrNull()?.body.orEmpty()
    val answersCurrentContext: Boolean =
        visibleCards.isNotEmpty() && visibleCards.all { it.title.isNotBlank() && it.body.isNotBlank() }
    val hasOnePrimaryContext: Boolean = primaryCards.size <= 1
    val withinVisualComplexityLimit: Boolean =
        visibleCards.size <= visibleCardLimit && primaryButtons.size <= MAX_PRIMARY_BUTTONS
    val keepsRawDetailsCollapsed: Boolean = visibleCards.filter { it.technical }.all { it.collapsed }
    val hidesTechnicalControlsByDefault: Boolean = visibleCards.none { it.technical }
    val behavesAsContextAssistant: Boolean = answersCurrentContext &&
        hasOnePrimaryContext &&
        withinVisualComplexityLimit &&
        keepsPersistentToolsVisible &&
        hidesTechnicalControlsByDefault &&
        keepsRawDetailsCollapsed

    fun visibleCardsFor(
        responsiveState: ComposeContextPanelResponsiveState,
    ): List<ComposeContextPanelCard> = visibleCards.map { card ->
        when {
            card.kind in persistentToolKinds -> card.copy(collapsed = false)
            responsiveState.collapseSecondaryCards && !card.primary -> card.copy(collapsed = true)
            responsiveState.collapseHistory && card.kind == ComposeContextPanelCardKind.RECENT_FILES ->
                card.copy(collapsed = true)
            else -> card
        }
    }

    companion object {
        private const val MAX_VISIBLE_CARDS: Int = 6
        private const val MAX_PRIMARY_BUTTONS: Int = 1

        fun forRoom(
            peerListState: ComposePeerListState,
            transferState: ComposeFileTransferState,
        ): ComposeContextPanelState = ComposeContextPanelState(
            mode = RightPanelMode.ROOM_INFO,
            cards = listOf(
                ComposeContextPanelCard(
                    kind = ComposeContextPanelCardKind.GUIDANCE,
                    title = "Choose someone to start",
                    body = peerListState.noPeerActionDetail,
                    primaryAction = "Select a peer",
                    primary = true,
                ),
                ComposeContextPanelCard(
                    kind = ComposeContextPanelCardKind.ROOM_STATUS,
                    title = "Room status",
                    body = "${peerListState.onlinePeers.size} online · ${transferState.transferCountSummary}",
                    badge = if (peerListState.onlinePeers.isEmpty()) "Waiting" else "Ready",
                ),
                persistentTransferCard(transferState),
            ),
        )

        fun forPeer(
            peerListState: ComposePeerListState,
            transferState: ComposeFileTransferState,
        ): ComposeContextPanelState {
            val peer = peerListState.selectedPeer
            return ComposeContextPanelState(
                mode = RightPanelMode.PEER_INFO,
                cards = listOf(
                    ComposeContextPanelCard(
                        kind = ComposeContextPanelCardKind.PEER_PROFILE,
                        title = peer?.nickname ?: peerListState.selectedPeerTitle,
                        body = peer?.selectedMeta ?: peerListState.selectedPeerMeta,
                        badge = peer?.availabilityLabel ?: "No peer",
                        metadata = peer?.capabilitySummary,
                        primary = true,
                    ),
                    persistentTransferCard(transferState),
                ),
            )
        }

        fun forTransfer(
            transferState: ComposeFileTransferState,
            peerListState: ComposePeerListState,
        ): ComposeContextPanelState {
            val peer = peerListState.selectedPeer
            return ComposeContextPanelState(
                mode = RightPanelMode.TRANSFERS,
                cards = buildList {
                    add(persistentTransferCard(transferState, primary = true))
                    if (peer != null) {
                        add(
                            ComposeContextPanelCard(
                                kind = ComposeContextPanelCardKind.PEER_PROFILE,
                                title = peer.nickname,
                                body = peer.selectedMeta,
                                badge = peer.availabilityLabel,
                                metadata = peer.capabilitySummary,
                                collapsed = true,
                            )
                        )
                    }
                },
            )
        }

        fun forCall(
            peerListState: ComposePeerListState,
            voiceState: ComposeMediaVoiceState,
            videoState: ComposeExperimentalVideoState,
            transferState: ComposeFileTransferState,
        ): ComposeContextPanelState {
            val peer = peerListState.selectedPeer
            val isVideo = videoState.currentSession != null || videoState.previewRunning
            val callKind = if (isVideo) "Video call" else "Voice call"
            val callStatus = if (isVideo) videoState.stageBadge else voiceState.voiceStatusText
            return ComposeContextPanelState(
                mode = RightPanelMode.CALL,
                cards = buildList {
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.CALL_CONTROLS,
                            title = callKind,
                            body = if (peer == null) callStatus else "$callStatus with ${peer.nickname}.",
                            badge = callStatus,
                            primary = true,
                        )
                    )
                    add(persistentTransferCard(transferState))
                    if (peer != null) {
                        add(
                            ComposeContextPanelCard(
                                kind = ComposeContextPanelCardKind.PEER_PROFILE,
                                title = peer.nickname,
                                body = peer.selectedMeta,
                                badge = peer.availabilityLabel,
                                metadata = peer.capabilitySummary,
                                collapsed = true,
                            )
                        )
                    }
                },
            )
        }

        private fun persistentTransferCard(
            transferState: ComposeFileTransferState,
            primary: Boolean = false,
        ): ComposeContextPanelCard = ComposeContextPanelCard(
            kind = ComposeContextPanelCardKind.TRANSFER_DETAILS,
            title = "Transfers",
            body = transferState.heroTitle,
            badge = transferState.transferCountSummary,
            primaryAction = if (transferState.waitingPromptCount > 0) "Review files" else null,
            primary = primary,
        )
    }
}
