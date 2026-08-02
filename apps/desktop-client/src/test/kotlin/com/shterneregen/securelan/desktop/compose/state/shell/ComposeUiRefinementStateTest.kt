package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.chat.ComposeCallWorkspaceFocusMode
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLineKind
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLinePresentation
import com.shterneregen.securelan.desktop.compose.state.media.ComposeVideoPreviewCorner
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.media.settleVideoPreviewCorner
import com.shterneregen.securelan.desktop.ui.TransferEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeUiRefinementStateTest {
    @Test
    fun shouldKeepTransfersAvailableInRoomContext() {
        val state = ComposeContextPanelState.forRoom(
            ComposeShellMetadata.DEFAULT_PEER_LIST_STATE,
            ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE,
        )

        assertTrue(state.keepsPersistentToolsVisible)
        assertTrue(ComposeContextPanelCardKind.TRANSFER_DETAILS in state.visibleCardKinds)
        assertFalse(state.visibleCardTitles.any { it == "Choose someone to start" })
        assertFalse(state.primaryButtons.any { it == "Select a peer" })
    }

    @Test
    fun shouldKeepDedicatedAndNonActionableToolsOutOfContextAssistant() {
        val peerState = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE
        val transferState = ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE
        val states = listOf(
            ComposeContextPanelState.forRoom(peerState, transferState),
            ComposeContextPanelState.forPeer(peerState, transferState),
            ComposeContextPanelState.forTransfer(transferState, peerState),
            ComposeContextPanelState.forCall(
                peerState,
                ComposeShellMetadata.DEFAULT_MEDIA_VOICE_STATE,
                ComposeShellMetadata.DEFAULT_VIDEO_STATE,
                transferState,
            ),
        )

        assertTrue(states.all { it.keepsPersistentToolsVisible })
        assertTrue(states.all { ComposeContextPanelCardKind.TRANSFER_DETAILS in it.visibleCardKinds })
        assertTrue(states.all { ComposeContextPanelCardKind.QUICK_SHARE !in it.visibleCardKinds })
        assertTrue(states.all { state ->
            state.visibleCardTitles.none { it.equals("Security", ignoreCase = true) }
        })
        assertFalse(
            states
                .first { it.mode == RightPanelMode.TRANSFERS }
                .visibleCards
                .first { it.kind == ComposeContextPanelCardKind.TRANSFER_DETAILS }
                .collapsed
        )
        assertFalse(
            states
                .first { it.mode == RightPanelMode.CALL }
                .visibleCards
                .first { it.kind == ComposeContextPanelCardKind.CALL_CONTROLS }
                .collapsed
        )
    }

    @Test
    fun shouldCollapseInactiveTransferPanelWithoutRemovingIt() {
        val state = ComposeContextPanelState.forPeer(
            ComposeShellMetadata.DEFAULT_PEER_LIST_STATE,
            ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE,
        )
        val responsiveStates = listOf(
            ComposeContextPanelResponsiveState.forWidth(1600),
            ComposeContextPanelResponsiveState.forWidth(1440),
            ComposeContextPanelResponsiveState.forWidth(1280),
            ComposeContextPanelResponsiveState.forWidth(1199),
        )

        assertTrue(responsiveStates.all { responsive ->
            state.visibleCardsFor(responsive)
                .first { it.kind == ComposeContextPanelCardKind.TRANSFER_DETAILS }
                .collapsed
        })
    }

    @Test
    fun shouldKeepTransferPanelWhenReceiveDecisionReturnsContextToPeer() {
        val peerState = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE
        val beforeDecision = ComposeContextPanelState.forTransfer(
            ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE,
            peerState,
        )
        val afterDecision = ComposeContextPanelState.forPeer(
            peerState,
            ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE,
        )

        assertTrue(ComposeContextPanelCardKind.TRANSFER_DETAILS in beforeDecision.visibleCardKinds)
        assertTrue(ComposeContextPanelCardKind.TRANSFER_DETAILS in afterDecision.visibleCardKinds)
        assertTrue(
            afterDecision.visibleCards
                .first { it.kind == ComposeContextPanelCardKind.TRANSFER_DETAILS }
                .collapsed
        )
        assertEquals(
            listOf(ComposeContextPanelCardKind.TRANSFER_DETAILS),
            beforeDecision.visibleCardKinds,
        )
        assertEquals(beforeDecision.visibleCardKinds, afterDecision.visibleCardKinds)
        assertTrue(beforeDecision.visibleCards.none { it.title == peerState.selectedPeerTitle })
        assertTrue(afterDecision.visibleCards.none { it.title == peerState.selectedPeerTitle })
    }

    @Test
    fun shouldExpandTransferDetailsOnlyWhenTransferNeedsAttention() {
        val activeTransferState = ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE.copy(
            entries = listOf(
                TransferEntry("send-1", "archive.zip", true, "Sending", 40, 1024),
            ),
        )
        val state = ComposeContextPanelState.forTransfer(
            activeTransferState,
            ComposeShellMetadata.DEFAULT_PEER_LIST_STATE,
        )
        val transferCard = state.visibleCards.first {
            it.kind == ComposeContextPanelCardKind.TRANSFER_DETAILS
        }

        assertFalse(transferCard.collapsed)
        assertEquals("1 active", transferCard.badge)
    }

    @Test
    fun shouldOmitRoomStatusAndHideZeroTransferCountersInIdleRoomContext() {
        val peerState = ComposePeerListState(peers = emptyList())
        val state = ComposeContextPanelState.forRoom(
            peerState,
            ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE.copy(peerListState = peerState),
        )
        val transferCard = state.visibleCards.first {
            it.kind == ComposeContextPanelCardKind.TRANSFER_DETAILS
        }

        assertFalse(state.visibleCardTitles.any { it == "Room status" })
        assertEquals(null, transferCard.badge)
        assertTrue(transferCard.collapsed)
    }

    @Test
    fun shouldKeepCompletedTransferHistoryDiscoverableOutsideTheTranscript() {
        val transferState = ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE.copy(
            entries = listOf(
                TransferEntry("receive-1", "report.pdf", false, "Completed", 100, 2048),
                TransferEntry("send-1", "archive.zip", true, "Completed", 100, 4096),
            ),
        )
        val state = ComposeContextPanelState.forRoom(
            ComposeShellMetadata.DEFAULT_PEER_LIST_STATE,
            transferState,
        )
        val transferCard = state.visibleCards.first {
            it.kind == ComposeContextPanelCardKind.TRANSFER_DETAILS
        }

        assertTrue(transferCard.collapsed)
        assertEquals("2 recent", transferCard.badge)
    }

    @Test
    fun shouldMoveContextAssistantToDrawerBeforeConversationGetsCramped() {
        assertEquals(
            ComposeContextPanelResponsiveMode.DRAWER,
            ComposeContextPanelResponsiveState.forWidth(1279).mode,
        )
        assertEquals(
            ComposeContextPanelResponsiveMode.COLLAPSED_HISTORY,
            ComposeContextPanelResponsiveState.forWidth(1280).mode,
        )
        assertEquals(
            ComposeContextPanelResponsiveMode.COLLAPSED_SECONDARY,
            ComposeContextPanelResponsiveState.forWidth(1440).mode,
        )
        assertEquals(
            ComposeContextPanelResponsiveMode.FULL_PANEL,
            ComposeContextPanelResponsiveState.forWidth(1600).mode,
        )
    }

    @Test
    fun shouldPresentQuickShareLinksAsActionableTranscriptEvents() {
        val url = "http://192.168.1.50:5053/file"
        val presentation = ComposeChatTranscriptLinePresentation.from(
            "[system] Quick Share file link is ready: $url"
        )

        assertEquals(ComposeChatTranscriptLineKind.QUICK_SHARE, presentation.kind)
        assertEquals("Quick Share", presentation.label)
        assertTrue(presentation.body.startsWith("File link created"))
        assertEquals(url, presentation.actionUrl)
    }

    @Test
    fun shouldKeepCallWorkspaceFocusModesExplicit() {
        assertTrue(ComposeCallWorkspaceFocusMode.SPLIT.splitResizable)
        assertTrue(ComposeCallWorkspaceFocusMode.VIDEO.showsVideo)
        assertFalse(ComposeCallWorkspaceFocusMode.VIDEO.showsChat)
        assertTrue(ComposeCallWorkspaceFocusMode.CHAT.showsChat)
        assertFalse(ComposeCallWorkspaceFocusMode.CHAT.showsVideo)
    }

    @Test
    fun shouldPresentPresenceAsCompactSystemEvent() {
        val presentation = ComposeChatTranscriptLinePresentation.from(
            "[system] Sybil joined the chat"
        )

        assertEquals(ComposeChatTranscriptLineKind.PRESENCE, presentation.kind)
        assertEquals("Sybil joined the chat", presentation.body)
    }

    @Test
    fun shouldCompactLocalNetworkAddressForRoomStatus() {
        assertEquals("192.168.1.50", compactRoomNetworkStatus("local network IP: 192.168.1.50"))
        assertEquals(
            "10.0.0.5 +1",
            compactRoomNetworkStatus("local network IPs: 10.0.0.5, 192.168.1.50"),
        )
        assertEquals(null, compactRoomNetworkStatus("local network IP is unavailable right now"))
    }

    @Test
    fun shouldSnapLocalVideoPreviewBetweenCorners() {
        assertEquals(
            ComposeVideoPreviewCorner.TOP_START,
            settleVideoPreviewCorner(ComposeVideoPreviewCorner.BOTTOM_END, dragX = -80f, dragY = -60f),
        )
        assertEquals(
            ComposeVideoPreviewCorner.BOTTOM_START,
            settleVideoPreviewCorner(ComposeVideoPreviewCorner.TOP_START, dragX = 0f, dragY = 60f),
        )
    }
}
