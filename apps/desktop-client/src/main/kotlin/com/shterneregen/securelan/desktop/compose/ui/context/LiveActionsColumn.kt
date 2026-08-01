package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelCardKind
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelResponsiveState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.ui.transfer.LiveFileTransferCard

@Composable
internal fun LiveActionsColumn(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    responsiveState: ComposeContextPanelResponsiveState,
    onOpenQuickShare: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
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
                    transferState,
                )

                transferState.requiresAttention -> ComposeContextPanelState.forTransfer(
                    transferState,
                    peerState,
                )

                peerState.selectedPeer != null -> ComposeContextPanelState.forPeer(peerState, transferState)
                else -> ComposeContextPanelState.forRoom(peerState, transferState)
            }
            LaunchedEffect(contextPanelState.mode, contextPanelState.visibleCardKinds.firstOrNull()) {
                panelScrollState.scrollTo(0)
            }
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                contextPanelState.visibleCardsFor(responsiveState).forEach { card ->
                    key(card.kind) {
                        val expandedContent: (@Composable () -> Unit)? = when (card.kind) {
                            ComposeContextPanelCardKind.TRANSFER_DETAILS -> {
                                { LiveFileTransferCard(hostAdapter, peerState) }
                            }

                            else -> null
                        }
                        ContextPanelCard(card, expandedContent)
                    }
                }
                QuickShareActivitySummary(
                    running = hostAdapter.quickShareRunning,
                    activeLinkCount = hostAdapter.quickShareEntries.count { it.active() },
                    onManage = onOpenQuickShare,
                )
            }
        }
    }
}
