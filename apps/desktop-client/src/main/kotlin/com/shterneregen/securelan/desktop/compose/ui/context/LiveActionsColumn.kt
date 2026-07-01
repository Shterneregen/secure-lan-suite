package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.*
import com.shterneregen.securelan.desktop.compose.ui.quickshare.LiveQuickShareCard
import com.shterneregen.securelan.desktop.compose.ui.transfer.LiveFileTransferCard

@Composable
internal fun LiveActionsColumn(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    responsiveState: ComposeContextPanelResponsiveState,
    expandedCardKind: ComposeContextPanelCardKind?,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val reducedMotion = LocalReducedMotion.current
    val panelScrollState = rememberScrollState()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(panelScrollState),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            val transferState = ComposeFileTransferState(
                statusState = hostAdapter.statusState,
                peerListState = peerState,
                senderId = hostAdapter.statusState.nickname,
                sessionPassword = hostAdapter.currentRoomPassword,
                entries = hostAdapter.transferEntries,
                incomingPrompts = hostAdapter.incomingTransferPrompts,
                autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles,
            )
            val voiceState = hostAdapter.mediaVoiceState.copy(peerListState = peerState)
            val videoState = hostAdapter.experimentalVideoState.copy(peerListState = peerState)
            val contextPanelState = when {
                voiceState.currentSession != null || videoState.currentSession != null -> ComposeContextPanelState.forCall(
                    peerState,
                    voiceState,
                    videoState,
                )

                transferState.activeCount > 0 || transferState.waitingPromptCount > 0 -> ComposeContextPanelState.forTransfer(
                    transferState,
                    peerState,
                )

                peerState.selectedPeer != null -> ComposeContextPanelState.forPeer(peerState, transferState)
                else -> ComposeContextPanelState.forRoom(peerState, transferState)
            }
            LaunchedEffect(contextPanelState.mode, contextPanelState.visibleCardKinds.firstOrNull()) {
                panelScrollState.scrollTo(0)
            }
            ContextPanelSummary(state = contextPanelState, responsiveState = responsiveState)
            val contextCardsReduced = LocalReducedMotion.current
            AnimatedContent(
                targetState = contextPanelState.mode,
                transitionSpec = {
                    fadeIn(motionTween(contextCardsReduced)) + slideInHorizontally(
                        motionTween(
                            contextCardsReduced
                        )
                    ) { it / 8 } togetherWith
                            fadeOut(motionTween(contextCardsReduced)) + slideOutHorizontally(motionTween(contextCardsReduced)) { it / 8 }
                },
                label = "ContextAssistantCards",
            ) { _ ->
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    contextPanelState.visibleCardsFor(responsiveState).forEach { card ->
                        val expandedContent: (@Composable () -> Unit)? = when (card.kind) {
                            ComposeContextPanelCardKind.TRANSFER_DETAILS -> {
                                { LiveFileTransferCard(hostAdapter, peerState) }
                            }

                            ComposeContextPanelCardKind.CALL_CONTROLS -> {
                                { CallStatusPanel(voiceState, videoState) }
                            }

                            ComposeContextPanelCardKind.QUICK_SHARE -> {
                                { LiveQuickShareCard(hostAdapter) }
                            }

                            else -> null
                        }
                        val initialExpanded = expandedCardKind?.let { it == card.kind }
                        ContextPanelCard(card, expandedContent, initialExpanded = initialExpanded)
                    }
                }
            }
        }
    }
}
