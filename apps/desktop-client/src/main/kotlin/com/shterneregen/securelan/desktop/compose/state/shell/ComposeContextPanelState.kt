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
    val collapsedCards: List<ComposeContextPanelCard> = visibleCards.filter { it.collapsed }
    val visibleCardTitles: List<String> = visibleCards.map { it.title }
    val visibleCardKinds: List<ComposeContextPanelCardKind> = visibleCards.map { it.kind }
    val hiddenFeatureNames: List<String> = when (mode) {
        RightPanelMode.ROOM_INFO -> listOf("Transfers", "Calls")
        RightPanelMode.PEER_INFO -> listOf("Port information")
        RightPanelMode.TRANSFERS -> listOf("Device settings")
        RightPanelMode.CALL -> listOf("Transfer setup")
        RightPanelMode.HIDDEN -> emptyList()
    }
    val hiddenFeatureSummary: String = if (hiddenFeatureNames.isEmpty()) {
        "Only the current conversation context is shown."
    } else {
        "More tools stay hidden until you need them."
    }
    val nextActionSummary: String = primaryCards.firstOrNull()?.body ?: visibleCards.firstOrNull()?.body.orEmpty()
    val answersCurrentContext: Boolean = visibleCards.isNotEmpty() && visibleCards.all { it.title.isNotBlank() && it.body.isNotBlank() }
    val hasOnePrimaryContext: Boolean = primaryCards.size <= 1
    val withinVisualComplexityLimit: Boolean = visibleCards.size <= visibleCardLimit && primaryButtons.size <= MAX_PRIMARY_BUTTONS
    val keepsRawDetailsCollapsed: Boolean = visibleCards.filter { it.technical }.all { it.collapsed }
    val hidesTechnicalControlsByDefault: Boolean = visibleCards.none { it.technical }
    val behavesAsContextAssistant: Boolean = answersCurrentContext &&
        hasOnePrimaryContext &&
        withinVisualComplexityLimit &&
        hidesTechnicalControlsByDefault &&
        keepsRawDetailsCollapsed

    fun responsiveStateFor(widthPx: Int): ComposeContextPanelResponsiveState = ComposeContextPanelResponsiveState.forWidth(widthPx)

    fun visibleCardsFor(responsiveState: ComposeContextPanelResponsiveState): List<ComposeContextPanelCard> = visibleCards.map { card ->
        when {
            responsiveState.collapseSecondaryCards && !card.primary -> card.copy(collapsed = true)
            responsiveState.collapseHistory && card.kind == ComposeContextPanelCardKind.RECENT_FILES -> card.copy(collapsed = true)
            else -> card
        }
    }

    fun visibleCardsForWidth(widthPx: Int): List<ComposeContextPanelCard> = visibleCardsFor(responsiveStateFor(widthPx))

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
                ComposeContextPanelCard(
                    kind = ComposeContextPanelCardKind.QUICK_SHARE,
                    title = "Quick Share",
                    body = "Share files through a temporary LAN browser link.",
                    collapsed = true,
                ),
            ),
        )

        fun forPeer(
            peerListState: ComposePeerListState,
            transferState: ComposeFileTransferState,
        ): ComposeContextPanelState {
            val peer = peerListState.selectedPeer
            return ComposeContextPanelState(
                mode = RightPanelMode.PEER_INFO,
                cards = buildList {
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.PEER_PROFILE,
                            title = peer?.nickname ?: peerListState.selectedPeerTitle,
                            body = peer?.selectedMeta ?: peerListState.selectedPeerMeta,
                            badge = peer?.availabilityLabel ?: "No peer",
                            metadata = peer?.capabilitySummary,
                            primary = true,
                        )
                    )
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.RECENT_FILES,
                            title = "Recent files",
                            body = if (transferState.recentEntryRows.isEmpty()) {
                                transferState.recentEmptyDetail
                            } else {
                                transferState.recentEntryRows.joinToString(" · ") { it.title }
                            },
                            badge = transferState.transferCountSummary,
                            collapsed = true,
                        )
                    )
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.SECURITY,
                            title = "Security",
                            body = "Messages and files stay inside the current secure LAN room.",
                            collapsed = true,
                        )
                    )
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.QUICK_SHARE,
                            title = "Quick Share",
                            body = "Share files through a temporary LAN browser link.",
                            collapsed = true,
                        )
                    )
                },
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
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.TRANSFER_DETAILS,
                            title = transferState.heroTitle,
                            body = transferState.nextStepSummary,
                            badge = transferState.transferCountSummary,
                            primaryAction = if (transferState.waitingPromptCount > 0) "Review files" else null,
                            primary = true,
                            collapsed = true,
                        )
                    )
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
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.QUICK_SHARE,
                            title = "Quick Share",
                            body = "Share files through a temporary LAN browser link.",
                            collapsed = true,
                        )
                    )
                },
            )
        }

        fun forCall(
            peerListState: ComposePeerListState,
            voiceState: ComposeMediaVoiceState,
            videoState: ComposeExperimentalVideoState,
        ): ComposeContextPanelState {
            val peer = peerListState.selectedPeer
            val isVideo = videoState.currentSession != null || videoState.previewRunning
            val callStatus = when {
                isVideo -> videoState.stageBadge
                voiceState.currentSession != null -> voiceState.voiceStatusText
                else -> "Ready to call"
            }
            val callKind = if (isVideo) "Video call" else "Voice call"
            return ComposeContextPanelState(
                mode = RightPanelMode.CALL,
                cards = buildList {
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.CALL_CONTROLS,
                            title = "$callKind status",
                            body = callStatus,
                            badge = peer?.availabilityLabel ?: "Unknown",
                            primary = true,
                            collapsed = true,
                        )
                    )
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
                    add(
                        ComposeContextPanelCard(
                            kind = ComposeContextPanelCardKind.QUICK_SHARE,
                            title = "Quick Share",
                            body = "Share files through a temporary LAN browser link.",
                            collapsed = true,
                        )
                    )
                },
            )
        }
    }
}
