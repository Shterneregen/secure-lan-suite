package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.chat.ComposeCallWorkspaceFocusMode
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLineKind
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLinePresentation
import com.shterneregen.securelan.desktop.compose.state.media.ComposeVideoPreviewCorner
import com.shterneregen.securelan.desktop.compose.state.media.settleVideoPreviewCorner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeUiRefinementStateTest {
    @Test
    fun shouldShowHiddenToolsHintOnlyOnceInRoomContext() {
        val state = ComposeContextPanelState.forRoom(
            ComposeShellMetadata.DEFAULT_PEER_LIST_STATE,
            ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE,
        )

        assertEquals("More tools stay hidden until you need them.", state.hiddenFeatureSummary)
        assertFalse(state.nextActionSummary.contains("tools stay hidden", ignoreCase = true))
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
            ),
        )

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
