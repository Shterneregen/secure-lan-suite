package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice
import com.shterneregen.securelan.desktop.ui.PeerPresence
import com.shterneregen.securelan.desktop.ui.QuickShareEntry
import com.shterneregen.securelan.desktop.ui.TransferEntry
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import com.shterneregen.securelan.stego.model.BmpCapacity
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class ComposeShellMetadataTest {
    @Test
    fun shouldKeepComposeWindowTitleAlignedWithJavaFxBaseline() {
        assertEquals("SecureLanSuite Chat", ComposeShellMetadata.WINDOW_TITLE)
    }

    @Test
    fun shouldDocumentJavaFxFallbackInShellCopy() {
        assertTrue(ComposeShellMetadata.STATUS_TEXT.contains("JavaFX"))
        assertTrue(ComposeShellMetadata.FALLBACK_TEXT.contains("standard desktop launcher"))
    }

    @Test
    fun shouldKeepComposeWindowSizeAlignedWithJavaFxBaselineScene() {
        assertEquals(1360f, ComposeShellMetadata.DEFAULT_WINDOW_WIDTH.value)
        assertEquals(860f, ComposeShellMetadata.DEFAULT_WINDOW_HEIGHT.value)
    }

    @Test
    fun shouldExposeJavaFxWorkspaceParityLayoutContract() {
        val state = ComposeShellMetadata.DEFAULT_WORKSPACE_PARITY_STATE

        assertEquals("Messenger workspace layout", state.title)
        assertEquals(listOf(0.20, 0.80), state.dividerPositions)
        assertEquals(listOf("Peers", "Shared room chat", "Actions"), state.workspaceColumns.map { it.title })
        assertEquals(listOf(0.20f, 0.60f, 0.20f), state.workspaceColumns.map { it.weight })
        assertEquals(true, state.chatPrimary)
        assertTrue(state.statusSummary.contains("Transfers"))
        assertTrue(state.headerSummary.contains("Room connection"))
        assertTrue(state.actionSectionSummary.contains("Runtime / Diagnostics"))
        assertTrue(state.quickActionSummary.contains("Voice call"))
        assertTrue(state.javaFxMappingSummary.contains("buildWorkspace") || state.javaFxMappingSummary.contains("buildPeersColumn"))
        assertEquals(true, state.parityReady)
    }

    @Test
    fun shouldBlockJavaFxWorkspaceParityWhenFallbackUnavailable() {
        val state = ComposeJavaFxWorkspaceParityState(javaFxFallbackAvailable = false)

        assertEquals(false, state.parityReady)
        assertTrue(state.fallbackLabel.contains("unavailable"))
    }

    @Test
    fun shouldExposeActionsColumnPresentationContractWithJavaFxTitledPaneDensity() {
        val state = ComposeShellMetadata.DEFAULT_ACTIONS_PRESENTATION_STATE

        assertEquals("Actions column parity presentation", state.title)
        assertEquals(
            listOf(
                ComposeActionsSectionKind.SELECTED_PEER,
                ComposeActionsSectionKind.TRANSFERS,
                ComposeActionsSectionKind.QUICK_SHARE,
                ComposeActionsSectionKind.STEGANOGRAPHY,
                ComposeActionsSectionKind.MEDIA_DEVICES,
                ComposeActionsSectionKind.RUNTIME_DIAGNOSTICS,
            ),
            state.sections.map { it.kind },
        )
        assertEquals(listOf("Selected peer", "Transfers"), state.expandedSectionTitles)
        assertEquals(
            listOf("LAN browser quick share", "Steganography", "Audio / Video devices", "Runtime / Diagnostics"),
            state.collapsedSectionTitles,
        )
        assertTrue(state.javaFxMappingSummary.contains("TitledPane"))
        assertEquals(
            "Selected peer → Transfers → LAN browser quick share → Steganography → Audio / Video devices → Runtime / Diagnostics",
            state.sectionOrderSummary,
        )
        assertEquals("Transfers", state.section(ComposeActionsSectionKind.TRANSFERS).title)
        assertTrue(state.visualNoiseSummary.contains("Collapsed by default"))
        assertTrue(state.fallbackLabel.contains("JavaFX actions column"))
        assertEquals(true, state.parityReady)
    }

    @Test
    fun shouldResolveContextPanelAsAssistantInsteadOfPermanentToolbox() {
        val peerState = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE
        val transferState = ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE
        val roomPanel = ComposeContextPanelState.forRoom(peerState, transferState)
        val peerPanel = ComposeContextPanelState.forPeer(
            quickActions = ComposeShellMetadata.DEFAULT_SELECTED_PEER_QUICK_ACTIONS_STATE,
            transferState = transferState,
        )

        assertEquals("Context Assistant", roomPanel.title)
        assertEquals(RightPanelMode.ROOM_INFO, roomPanel.mode)
        assertEquals(listOf(ComposeContextPanelCardKind.GUIDANCE, ComposeContextPanelCardKind.ROOM_STATUS), roomPanel.visibleCardKinds)
        assertTrue(roomPanel.hiddenFeatureNames.contains("Quick Share"))
        assertTrue(roomPanel.hiddenFeatureNames.contains("Runtime"))
        assertTrue(roomPanel.hiddenFeatureNames.contains("Detailed diagnostics"))
        assertEquals("More tools stay tucked away until they help this conversation.", roomPanel.hiddenFeatureSummary)
        assertEquals(true, roomPanel.hidesQuickShareUntilRequested)
        assertEquals(true, roomPanel.behavesAsContextAssistant)
        assertEquals(true, peerPanel.behavesAsContextAssistant)
        assertTrue(peerPanel.visibleCardKinds.contains(ComposeContextPanelCardKind.PEER_PROFILE))
        assertFalse(peerPanel.visibleCardKinds.contains(ComposeContextPanelCardKind.QUICK_SHARE))
        assertTrue(peerPanel.hiddenFeatureNames.contains("Quick Share"))
        assertEquals(true, peerPanel.hidesQuickShareUntilRequested)
        assertFalse(peerPanel.visibleCardKinds.contains(ComposeContextPanelCardKind.DIAGNOSTICS))
        assertFalse(peerPanel.visibleCardKinds.contains(ComposeContextPanelCardKind.MEDIA))
        assertTrue(peerPanel.visibleCards.size <= 5)
        assertTrue(peerPanel.primaryButtons.size <= 1)
        assertEquals(1, peerPanel.primaryCards.size)
    }

    @Test
    fun shouldStartDiagnosticsContextPanelWithHealthAndRecoveryBeforeTechnicalDetails() {
        val diagnostics = ComposeDiagnosticsState(
            statusState = ComposeStatusConnectionState(nickname = " ", serverChatPortText = "0"),
            peerListState = ComposePeerListState(peers = emptyList()),
            chatDiagnostics = listOf("[connected] very long diagnostic message ".repeat(10)),
            fileTransferDiagnostics = listOf("Transfer completed: demo.txt."),
            quickShareDiagnostics = listOf("Quick-share created."),
            realtimeDiagnostics = listOf("RTC runtime ready"),
            javaFxFallbackAvailable = false,
        )
        val panel = ComposeContextPanelState.forDiagnostics(diagnostics)

        assertEquals(RightPanelMode.DIAGNOSTICS, panel.mode)
        assertEquals(
            listOf(
                ComposeContextPanelCardKind.DIAGNOSTICS,
                ComposeContextPanelCardKind.DIAGNOSTIC_RECOVERY,
                ComposeContextPanelCardKind.ADVANCED_DETAILS,
            ),
            panel.visibleCardKinds,
        )
        assertEquals(1, panel.primaryCards.size)
        assertEquals("Health summary", panel.visibleCards.first().title)
        assertTrue(panel.startsDiagnosticsWithHealthAndRecovery)
        assertTrue(panel.keepsRawDetailsCollapsed)
        assertTrue(panel.behavesAsContextAssistant)
        assertTrue(panel.visibleCards.last().collapsed)
        assertTrue(panel.visibleCards.last().technical)
        assertFalse(panel.visibleCards.last().body.contains("very long diagnostic message"))
        assertTrue(panel.visibleCards.size <= 6)
        assertTrue(panel.primaryButtons.size <= 1)
    }

    @Test
    fun shouldPrioritizeTransferAndCallContextsInRightPanel() {
        val peerState = ComposePeerListState(selectedPeerIndex = 0)
        val quickActions = ComposeSelectedPeerQuickActionsState(
            peerListState = peerState,
            clientConnected = true,
            hangUpReady = true,
        )
        val transferState = ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = peerState,
        )
        val transferPanel = ComposeContextPanelState.forTransfer(transferState, quickActions)
        val callPanel = ComposeContextPanelState.forCall(
            quickActions = quickActions,
            voiceState = ComposeMediaVoiceState(
                statusState = ComposeStatusConnectionState(clientConnected = true),
                peerListState = peerState,
            ),
            videoState = ComposeExperimentalVideoState(
                statusState = ComposeStatusConnectionState(clientConnected = true),
                peerListState = peerState,
            ),
        )

        assertEquals(RightPanelMode.TRANSFERS, transferPanel.mode)
        assertEquals(ComposeContextPanelCardKind.TRANSFER_DETAILS, transferPanel.visibleCardKinds.first())
        assertEquals(RightPanelMode.CALL, callPanel.mode)
        assertEquals(ComposeContextPanelCardKind.CALL_CONTROLS, callPanel.visibleCardKinds.first())
        assertTrue(transferPanel.hiddenFeatureNames.contains("Quick Share"))
        assertTrue(callPanel.hiddenFeatureNames.contains("Steganography"))
        assertEquals(true, transferPanel.behavesAsContextAssistant)
        assertEquals(true, callPanel.behavesAsContextAssistant)
    }

    @Test
    fun shouldExposeResponsiveContextAssistantBehaviorForPhaseElevenWidths() {
        val peerState = ComposePeerListState(selectedPeerIndex = 0)
        val quickActions = ComposeSelectedPeerQuickActionsState(
            peerListState = peerState,
            clientConnected = true,
        )
        val panel = ComposeContextPanelState.forPeer(
            quickActions = quickActions,
            transferState = ComposeShellMetadata.DEFAULT_FILE_TRANSFER_STATE,
        )

        val full = panel.responsiveStateFor(1600)
        val collapsedSecondary = panel.responsiveStateFor(1500)
        val collapsedHistory = panel.responsiveStateFor(1300)
        val drawer = panel.responsiveStateFor(1199)

        assertEquals(ComposeContextPanelResponsiveMode.FULL_PANEL, full.mode)
        assertEquals(true, full.inlinePanelVisible)
        assertEquals(false, full.drawerMode)
        assertEquals(ComposeContextPanelResponsiveMode.COLLAPSED_SECONDARY, collapsedSecondary.mode)
        assertEquals(true, collapsedSecondary.collapseSecondaryCards)
        assertEquals(ComposeContextPanelResponsiveMode.COLLAPSED_HISTORY, collapsedHistory.mode)
        assertEquals(true, collapsedHistory.collapseHistory)
        assertEquals(ComposeContextPanelResponsiveMode.DRAWER, drawer.mode)
        assertEquals(false, drawer.inlinePanelVisible)
        assertEquals(true, drawer.drawerMode)
        assertEquals(true, drawer.drawerEntryVisible)
        assertEquals("Open Context Assistant", drawer.drawerOpenLabel)
        assertEquals("Close Context Assistant", drawer.drawerCloseLabel)
        assertEquals("Context Assistant drawer", drawer.drawerContentDescription)
        assertEquals("Open Context Assistant drawer", drawer.drawerOpenContentDescription)
        assertEquals("Close Context Assistant drawer", drawer.drawerCloseContentDescription)
        assertEquals(true, drawer.escapeClosesDrawer)
        assertEquals(true, listOf(full, collapsedSecondary, collapsedHistory, drawer).all { it.preservesConversationFirst })
        assertTrue(collapsedSecondary.summary.contains("Secondary context cards collapse"), collapsedSecondary.summary)
    }

    @Test
    fun shouldCollapseContextAssistantCardsWithoutChangingPrimaryContext() {
        val peerState = ComposePeerListState(selectedPeerIndex = 0)
        val panel = ComposeContextPanelState.forPeer(
            quickActions = ComposeSelectedPeerQuickActionsState(
                peerListState = peerState,
                clientConnected = true,
            ),
            transferState = ComposeFileTransferState(
                statusState = ComposeStatusConnectionState(clientConnected = true),
                peerListState = peerState,
                entries = listOf(TransferEntry("tx-recent", "recent.bin", true, "Completed", 100, 4096)),
            ),
        )

        val fullCards = panel.visibleCardsForWidth(1600)
        val secondaryCollapsedCards = panel.visibleCardsForWidth(1500)
        val historyCollapsedCards = panel.visibleCardsForWidth(1300)

        assertEquals(panel.visibleCardKinds, fullCards.map { it.kind })
        assertEquals(false, fullCards.first { it.primary }.collapsed)
        assertEquals(false, secondaryCollapsedCards.first { it.primary }.collapsed)
        assertTrue(
            secondaryCollapsedCards.filterNot { it.primary }.all { it.collapsed },
            "secondaryCollapsedCards=$secondaryCollapsedCards",
        )
        assertEquals(false, historyCollapsedCards.first { it.primary }.collapsed)
        assertEquals(
            true,
            historyCollapsedCards.first { it.kind == ComposeContextPanelCardKind.RECENT_FILES }.collapsed,
        )
        assertEquals(1, historyCollapsedCards.count { it.primary })
    }

    @Test
    fun shouldBlockActionsColumnPresentationWhenFallbackUnavailable() {
        val state = ComposeActionsColumnPresentationState(javaFxFallbackAvailable = false)

        assertEquals(false, state.parityReady)
        assertTrue(state.fallbackLabel.contains("unavailable"))
    }

    @Test
    fun shouldExposeFirstLaunchOnboardingWithoutTechnicalDetails() {
        val state = ComposeShellMetadata.DEFAULT_ONBOARDING_STATE

        assertEquals("SecureLanSuite", state.title)
        assertEquals("Secure chat for people nearby", state.headline)
        assertEquals("Private LAN messages, files, and calls without cloud accounts.", state.body)
        assertEquals(listOf("Host secure room", "Join nearby room"), state.primaryActions)
        assertEquals(listOf("Advanced connection", "Settings"), state.secondaryLinks)
        assertEquals(listOf("LAN only", "Encrypted", "Files", "Calls"), state.benefitChips)
        assertEquals("Looking for nearby rooms…", state.discoveryStatus)
        assertEquals("No nearby rooms yet", state.emptyNearbyTitle)
        assertEquals(true, state.showsOnlyPrimaryConnectionChoices)
        assertEquals(true, state.hidesTechnicalDetails)
        assertEquals(true, state.hasFullDesktopComposition)
        assertEquals(180..240, state.primaryButtonWidthRange)
        assertEquals(180..240, state.secondaryButtonWidthRange)
        assertEquals(true, state.avoidsFirstRunTechnicalFields)
        assertFalse(state.guidanceSummary.contains("port", ignoreCase = true))
        assertFalse(state.guidanceSummary.contains("adapter", ignoreCase = true))
    }

    @Test
    fun shouldExposePhaseElevenDesignTokens() {
        val dark = SecureLanThemeTokens.Dark
        val light = SecureLanThemeTokens.Light

        assertEquals(4f, dark.spacing.xxs.value)
        assertEquals(8f, dark.spacing.xs.value)
        assertEquals(12f, dark.spacing.sm.value)
        assertEquals(16f, dark.spacing.md.value)
        assertEquals(20f, dark.spacing.lg.value)
        assertEquals(24f, dark.spacing.xl.value)
        assertEquals(32f, dark.spacing.xxl.value)
        assertEquals(40f, dark.spacing.xxxl.value)
        assertEquals(8f, dark.radius.small.value)
        assertEquals(12f, dark.radius.medium.value)
        assertEquals(20f, dark.radius.large.value)
        assertEquals(36f, dark.density.buttonMinHeight.value)
        assertEquals(38f, dark.density.inputMinHeight.value)
        assertEquals(48f, dark.density.sidebarRowMinHeight.value)
        assertEquals(60f, dark.density.sidebarRowMaxHeight.value)
        assertEquals(52f, dark.density.composerMinHeight.value)
        assertEquals(24f, dark.typography.titleMin.value)
        assertEquals(32f, dark.typography.titleMax.value)
        assertEquals(13f, dark.typography.bodySmall.value)
        assertEquals(15f, dark.typography.bodyLarge.value)
        assertEquals(150, dark.motion.durationFast)
        assertEquals(200, dark.motion.durationDefault)
        assertEquals(250, dark.motion.durationSlow)
        assertEquals(0, dark.motion.durationInstant)
        assertEquals(SecureLanThemeTokens.DarkColors, dark.colors)
        assertEquals(SecureLanThemeTokens.LightColors, light.colors)
        assertNotEquals(dark.colors.background, light.colors.background)
        assertNotEquals(androidx.compose.ui.graphics.Color.Black, dark.colors.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF60A5FA), dark.colors.borderFocus)
    }

    @Test
    fun shouldExposeComposerSafeLayoutMetadata() {
        assertEquals(52f, ComposeShellMetadata.COMPOSER_MIN_HEIGHT.value)
        assertTrue(ComposeShellMetadata.COMPOSER_SAFE_VERTICAL_SPACE >= ComposeShellMetadata.COMPOSER_MIN_HEIGHT)
        assertEquals(0.55f, ComposeShellMetadata.CONNECTION_HUB_EXPANDED_MAX_FRACTION)
        assertTrue(ComposeShellMetadata.MIN_CHAT_SURFACE_HEIGHT >= ComposeShellMetadata.COMPOSER_SAFE_VERTICAL_SPACE)
        assertEquals(8f, ComposeShellMetadata.CENTER_COLUMN_SPACING.value)

        val contract = ComposeWorkspaceLayoutContract()
        assertEquals(ComposeShellMetadata.COMPOSER_MIN_HEIGHT, contract.composerMinHeight)
        assertEquals(ComposeShellMetadata.COMPOSER_SAFE_VERTICAL_SPACE, contract.composerSafeVerticalSpace)
        assertEquals(ComposeShellMetadata.CONNECTION_HUB_EXPANDED_MAX_FRACTION, contract.connectionHubExpandedMaxFraction)
        assertEquals(ComposeShellMetadata.MIN_CHAT_SURFACE_HEIGHT, contract.minChatSurfaceHeight)
        assertEquals(ComposeShellMetadata.CENTER_COLUMN_SPACING, contract.centerColumnSpacing)
        assertTrue(contract.layoutSummary.contains("Composer"))
        assertTrue(contract.layoutSummary.contains("hub"))

        val offline = ComposeShellMetadata.DEFAULT_WORKSPACE_STATE
        assertEquals(ComposeShellMetadata.COMPOSER_MIN_HEIGHT, offline.layoutContract.composerMinHeight)
        assertTrue(offline.layoutContract.layoutSummary.contains("dp"))
    }

    @Test
    fun shouldExposePhaseElevenProductScreenModel() {
        val defaultState = ComposeShellMetadata.DEFAULT_PRODUCT_SCREEN_STATE
        val hostSetup = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(),
            requestedAppMode = AppMode.HOST_SETUP,
            connectionHubMode = ComposeConnectionHubMode.HOST,
        )
        val joinSetup = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(),
            requestedAppMode = AppMode.JOIN_SETUP,
            connectionHubMode = ComposeConnectionHubMode.JOIN,
        )
        val connectedPeer = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            requestedAppMode = AppMode.HOST_SETUP,
            selectedPeer = ComposePeerListItem.defaultPreviewItems(clientConnected = true).first(),
        )
        val transfer = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            activeTransfer = true,
        )
        val call = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            activeCall = true,
        )
        val diagnostics = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            diagnosticsRequested = true,
        )
        val settings = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            settingsRequested = true,
        )
        val advanced = ComposeProductScreenState.from(
            statusState = ComposeStatusConnectionState(),
            advancedConnectionRequested = true,
        )

        assertEquals(AppMode.WELCOME, defaultState.appMode)
        assertEquals(RoomState.OFFLINE, defaultState.roomState)
        assertEquals(SelectionState.NONE, defaultState.selectionState)
        assertEquals(RightPanelMode.HIDDEN, defaultState.rightPanelMode)
        assertEquals(AppMode.HOST_SETUP, hostSetup.appMode)
        assertEquals(AppMode.JOIN_SETUP, joinSetup.appMode)
        assertEquals(true, hostSetup.connectionFlowActive)
        assertEquals(false, defaultState.connectionAndCommunicationSeparated)
        assertEquals(false, hostSetup.connectionAndCommunicationSeparated)
        assertEquals(false, joinSetup.connectionAndCommunicationSeparated)
        assertEquals(AppMode.MESSENGER, connectedPeer.appMode)
        assertEquals(RoomState.CONNECTED, connectedPeer.roomState)
        assertEquals(SelectionState.PEER, connectedPeer.selectionState)
        assertEquals(RightPanelMode.PEER_INFO, connectedPeer.rightPanelMode)
        assertEquals(SelectionState.TRANSFER, transfer.selectionState)
        assertEquals(RightPanelMode.TRANSFERS, transfer.rightPanelMode)
        assertEquals(SelectionState.CALL, call.selectionState)
        assertEquals(RightPanelMode.CALL, call.rightPanelMode)
        assertEquals(AppMode.DIAGNOSTICS, diagnostics.appMode)
        assertEquals(RightPanelMode.DIAGNOSTICS, diagnostics.rightPanelMode)
        assertEquals(AppMode.SETTINGS, settings.appMode)
        assertEquals(RightPanelMode.HIDDEN, settings.rightPanelMode)
        assertEquals(RightPanelMode.ADVANCED_CONNECTION, advanced.rightPanelMode)
        assertTrue(connectedPeer.modeSummary.contains("Messenger"))
    }

    @Test
    fun shouldResolveWorkspaceStateMachineFromRuntimeConditions() {
        val offline = ComposeWorkspaceState.from(
            statusState = ComposeStatusConnectionState(),
            peerState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            transferState = ComposeFileTransferState(
                statusState = ComposeStatusConnectionState(),
                peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            ),
            voiceState = ComposeMediaVoiceState(
                statusState = ComposeStatusConnectionState(),
                peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            ),
            videoState = ComposeExperimentalVideoState(
                statusState = ComposeStatusConnectionState(),
                peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            ),
        )
        val hosting = offline.copy(mode = ComposeWorkspaceMode.HOSTING)
        val connected = offline.copy(mode = ComposeWorkspaceMode.CONNECTED)
        val peerSelected = offline.copy(mode = ComposeWorkspaceMode.PEER_SELECTED)
        val voiceCall = offline.copy(mode = ComposeWorkspaceMode.VOICE_CALL)
        val videoCall = offline.copy(mode = ComposeWorkspaceMode.VIDEO_CALL)
        val fileTransfer = offline.copy(mode = ComposeWorkspaceMode.FILE_TRANSFER)

        assertEquals(ComposeWorkspaceMode.OFFLINE, offline.mode)
        assertEquals(true, offline.startupSurfaceVisible)
        assertEquals(true, offline.chatVisible)
        assertEquals(true, offline.connectionHubExpandedByDefault)
        assertEquals(false, offline.videoStageVisible)
        assertEquals(false, offline.callBannerVisible)
        assertEquals(false, offline.inlineTransferVisible)
        assertEquals(RightPanelMode.HIDDEN, offline.rightPanelMode)
        assertEquals("Offline", offline.modeLabel)

        assertEquals(true, hosting.startupSurfaceVisible)
        assertEquals(true, hosting.chatVisible)
        assertEquals(false, hosting.connectionHubExpandedByDefault)
        assertEquals(RightPanelMode.ROOM_INFO, hosting.rightPanelMode)

        assertEquals(true, connected.startupSurfaceVisible)
        assertEquals(true, connected.chatVisible)
        assertEquals(false, connected.connectionHubExpandedByDefault)
        assertEquals(RightPanelMode.ROOM_INFO, connected.rightPanelMode)

        assertEquals(true, peerSelected.chatVisible)
        assertEquals(true, peerSelected.startupSurfaceVisible)
        assertEquals(false, peerSelected.connectionHubExpandedByDefault)
        assertEquals(RightPanelMode.PEER_INFO, peerSelected.rightPanelMode)

        assertEquals(true, voiceCall.chatVisible)
        assertEquals(true, voiceCall.startupSurfaceVisible)
        assertEquals(false, voiceCall.connectionHubExpandedByDefault)
        assertEquals(true, voiceCall.callBannerVisible)
        assertEquals(RightPanelMode.CALL, voiceCall.rightPanelMode)

        assertEquals(true, videoCall.chatVisible)
        assertEquals(true, videoCall.startupSurfaceVisible)
        assertEquals(false, videoCall.connectionHubExpandedByDefault)
        assertEquals(true, videoCall.videoStageVisible)
        assertEquals(RightPanelMode.CALL, videoCall.rightPanelMode)

        assertEquals(true, fileTransfer.chatVisible)
        assertEquals(true, fileTransfer.startupSurfaceVisible)
        assertEquals(false, fileTransfer.connectionHubExpandedByDefault)
        assertEquals(true, fileTransfer.inlineTransferVisible)
        assertEquals(RightPanelMode.TRANSFERS, fileTransfer.rightPanelMode)

        val localHostedClient = ComposeWorkspaceState.from(
            statusState = ComposeStatusConnectionState(localServerRunning = true, clientConnected = true),
            peerState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            transferState = ComposeFileTransferState(
                statusState = ComposeStatusConnectionState(localServerRunning = true, clientConnected = true),
                peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            ),
            voiceState = ComposeMediaVoiceState(
                statusState = ComposeStatusConnectionState(localServerRunning = true, clientConnected = true),
                peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            ),
            videoState = ComposeExperimentalVideoState(
                statusState = ComposeStatusConnectionState(localServerRunning = true, clientConnected = true),
                peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            ),
        )
        assertEquals(ComposeWorkspaceMode.CONNECTED, localHostedClient.mode)
        assertEquals("Connected", localHostedClient.modeLabel)

        val hostingWithPeer = ComposeWorkspaceState.from(
            statusState = ComposeStatusConnectionState(localServerRunning = true),
            peerState = ComposePeerListState(selectedPeerIndex = 0),
            transferState = ComposeFileTransferState(
                statusState = ComposeStatusConnectionState(localServerRunning = true),
                peerListState = ComposePeerListState(selectedPeerIndex = 0),
            ),
            voiceState = ComposeMediaVoiceState(
                statusState = ComposeStatusConnectionState(localServerRunning = true),
                peerListState = ComposePeerListState(selectedPeerIndex = 0),
            ),
            videoState = ComposeExperimentalVideoState(
                statusState = ComposeStatusConnectionState(localServerRunning = true),
                peerListState = ComposePeerListState(selectedPeerIndex = 0),
            ),
        )
        assertEquals(ComposeWorkspaceMode.PEER_SELECTED, hostingWithPeer.mode)
        assertEquals(true, hostingWithPeer.startupSurfaceVisible)
        assertEquals(false, hostingWithPeer.connectionHubExpandedByDefault)
        assertEquals(RightPanelMode.PEER_INFO, hostingWithPeer.rightPanelMode)

        val disconnectedWithPeer = ComposeWorkspaceState.from(
            statusState = ComposeStatusConnectionState(),
            peerState = ComposePeerListState(selectedPeerIndex = 0),
            transferState = ComposeFileTransferState(
                statusState = ComposeStatusConnectionState(),
                peerListState = ComposePeerListState(selectedPeerIndex = 0),
            ),
            voiceState = ComposeMediaVoiceState(
                statusState = ComposeStatusConnectionState(),
                peerListState = ComposePeerListState(selectedPeerIndex = 0),
            ),
            videoState = ComposeExperimentalVideoState(
                statusState = ComposeStatusConnectionState(),
                peerListState = ComposePeerListState(selectedPeerIndex = 0),
            ),
        )
        assertEquals(ComposeWorkspaceMode.OFFLINE, disconnectedWithPeer.mode)
        assertEquals(true, disconnectedWithPeer.startupSurfaceVisible)
        assertEquals(true, disconnectedWithPeer.connectionHubExpandedByDefault)
    }

    @Test
    fun shouldExposePhaseElevenAppShellTopBarState() {
        val welcome = ComposeShellMetadata.DEFAULT_APP_SHELL_STATE
        val messenger = ComposeAppShellState(
            productState = ComposeProductScreenState.from(
                statusState = ComposeStatusConnectionState(clientConnected = true, nickname = "Astra"),
            ),
            statusState = ComposeStatusConnectionState(clientConnected = true, nickname = "Astra"),
            peerStatus = "Peer Victor",
        )
        val warning = messenger.copy(warningVisible = true)

        assertEquals(48, welcome.topBarHeightMin)
        assertEquals(56, welcome.topBarHeightMax)
        assertEquals("SecureLanSuite", welcome.currentContextLabel)
        assertEquals("Offline", welcome.primaryStatusDetail)
        assertEquals(false, welcome.lightweightShell)
        assertEquals(true, welcome.threeColumnShell)
        assertEquals(true, welcome.hasOneGlobalStatusIndicator)
        assertEquals(true, welcome.avoidsLongPrimaryStatus)
        assertEquals(listOf("Diagnostics", "Theme"), welcome.rightActions)
        assertEquals("Astra", messenger.currentContextLabel)
        assertEquals("Connected", messenger.primaryStatusDetail)
        assertEquals(false, messenger.lightweightShell)
        assertEquals(true, messenger.threeColumnShell)
        assertTrue(messenger.technicalStatusDetail.contains("Peer Victor"), messenger.technicalStatusDetail)
        assertEquals(listOf("Diagnostics", "Theme"), warning.rightActions)
    }

    @Test
    fun shouldUseSingleWorkspaceAcrossAllModes() {
        val welcome = ComposeShellMetadata.DEFAULT_APP_SHELL_STATE
        val host = ComposeAppShellState(
            productState = ComposeProductScreenState.from(
                statusState = ComposeStatusConnectionState(),
                requestedAppMode = AppMode.HOST_SETUP,
            ),
            statusState = ComposeStatusConnectionState(),
        )
        val join = ComposeAppShellState(
            productState = ComposeProductScreenState.from(
                statusState = ComposeStatusConnectionState(),
                requestedAppMode = AppMode.JOIN_SETUP,
            ),
            statusState = ComposeStatusConnectionState(),
        )

        assertEquals(true, welcome.singleWorkspace)
        assertEquals(true, host.singleWorkspace)
        assertEquals(true, join.singleWorkspace)
        assertEquals(false, welcome.lightweightShell)
        assertEquals(false, host.lightweightShell)
        assertEquals(false, join.lightweightShell)
        assertEquals(true, welcome.threeColumnShell)
        assertEquals(true, host.threeColumnShell)
        assertEquals(true, join.threeColumnShell)
    }

    @Test
    fun shouldExposeDefaultChatWorkspaceStateWithoutRuntimeSideEffects() {
        val state = ComposeShellMetadata.DEFAULT_CHAT_WORKSPACE_STATE

        assertEquals("Shared room chat", state.title)
        assertTrue(state.subtitle.contains("Astra Laptop"))
        assertEquals(3, state.transcriptLines.size)
        assertEquals(3, state.transcriptMessageTimes.size)
        assertTrue(state.transcriptMessageTimes.all { it.matches(Regex("\\d{2}:\\d{2}")) })
        assertTrue(state.transcriptLines.first().contains("[connected]"))
        assertTrue(
            state.transcriptEmptyTitle in setOf(
                "Room not open yet",
                "Choose someone to start",
                "Start the conversation",
            )
        )
        assertTrue(state.transcriptEmptyDetailDisconnected.contains("Host or join"))
        assertTrue(state.transcriptEmptyDetailConnected.contains("Say hello") || state.transcriptEmptyDetailConnected.contains("Select a person"))
        assertEquals(3, state.transcriptEmptyStructuredCopy.size)
        assertEquals(ComposeEmptyStateVisualWeight.PRIMARY_GUIDANCE, state.transcriptEmptyVisualWeight)
        assertEquals(true, state.draftValid)
        assertEquals(false, state.canSendMessage)
        assertEquals("Send blocked", state.sendLabel)
        assertTrue(state.readinessSummary.contains("Connect to chat"))
        assertTrue(state.fallbackLabel.contains("JavaFX chat workspace"))
    }

    @Test
    fun shouldAllowChatSendPreviewOnlyWhenConnectedAndDraftIsNotBlank() {
        val state = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(),
            draftMessage = "  hello  ",
        )

        assertEquals(true, state.draftValid)
        assertEquals(true, state.canSendMessage)
        assertEquals("Send ready", state.sendLabel)
        assertEquals(emptyList<String>(), state.blockedReasons)
        assertTrue(state.readinessSummary.contains("ready"))
    }

    @Test
    fun shouldBlockChatSendPreviewForBlankDraftAndMissingFallback() {
        val state = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(peers = emptyList()),
            draftMessage = " ",
            messages = emptyList(),
            javaFxFallbackAvailable = false,
        )

        assertEquals(
            "Connect to chat, then select a peer on the left for voice, video, and file actions.",
            state.subtitle
        )
        assertEquals(emptyList<String>(), state.transcriptLines)
        assertEquals("No chat messages yet.", state.transcriptSummary)
        assertEquals(false, state.draftValid)
        assertEquals(false, state.canSendMessage)
        assertTrue(state.blockedReasons.any { it.contains("non-empty message") })
        assertTrue(state.blockedReasons.any { it.contains("JavaFX fallback") })
    }

    @Test
    fun shouldSurfaceSharedRoomSemanticsInChatWorkspaceState() {
        val connected = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            draftMessage = "hi",
        )
        val disconnected = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = false),
            peerListState = ComposePeerListState(peers = emptyList()),
            messages = emptyList(),
        )

        assertEquals("Shared room chat", connected.title)
        assertTrue(connected.subtitle.contains("visible to everyone"))
        assertEquals(true, connected.canSendMessage)
        assertEquals("Room not open yet", disconnected.transcriptEmptyTitle)
        assertTrue(disconnected.transcriptEmptyDetailDisconnected.contains("Host or join"))
        assertTrue(disconnected.transcriptEmptyDetailConnected.contains("Select a person"))
    }

    @Test
    fun shouldExposeMicrointeractionChecklistForChatWorkspace() {
        val state = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            draftMessage = "hello",
        )

        assertEquals(5, state.microinteractionChecklist.size)
        assertTrue(state.microinteractionChecklist.any { it.contains("Hover") })
        assertTrue(state.microinteractionChecklist.any { it.contains("Focus") })
        assertTrue(state.microinteractionChecklist.any { it.contains("Loading") })
        assertTrue(state.microinteractionChecklist.any { it.contains("transfer completion", ignoreCase = true) })
        assertTrue(state.microinteractionChecklist.any { it.contains("Composer focus") })
    }

    @Test
    fun shouldExposeCompletedWorkspaceConsistencyReview() {
        val review = ComposeShellMetadata.DEFAULT_WORKSPACE_CONSISTENCY_REVIEW_STATE

        assertEquals(ComposeWorkspaceConsistencyReviewArea.values().toSet(), review.reviewedAreas.toSet())
        assertEquals(13, review.items.size)
        assertEquals(true, review.allReviewed)
        assertEquals(true, review.allPassed)
        assertEquals(emptyList<String>(), review.automaticRejectConditions)
        assertTrue(review.productScore >= 95, review.summary)
        assertEquals("Accept", review.decision)
        assertTrue(review.items.first { it.area == ComposeWorkspaceConsistencyReviewArea.NAVIGATION }.evidence.contains("persistent workspace"))
        assertTrue(review.items.first { it.area == ComposeWorkspaceConsistencyReviewArea.CONTEXT_ASSISTANT }.evidence.contains("one primary card"))
        assertTrue(review.items.first { it.area == ComposeWorkspaceConsistencyReviewArea.RESPONSIVE_LAYOUTS }.evidence.contains("conversation width"))
        assertTrue(review.items.first { it.area == ComposeWorkspaceConsistencyReviewArea.CHAT_READABILITY }.evidence.contains("semantic message kinds"))
    }

    @Test
    fun shouldExposeRuntimeResizeAndScreenshotValidationMatrix() {
        val matrix = ComposeShellMetadata.DEFAULT_RUNTIME_SCREENSHOT_MATRIX_STATE

        assertEquals("Runtime resize and screenshot validation matrix", matrix.title)
        assertEquals(5, matrix.requiredSizeCount)
        assertEquals(15, matrix.requiredStateCount)
        assertEquals(75, matrix.screenshotCount)
        assertEquals(matrix.screenshotCount, matrix.acceptedScreenshotCount)
        assertEquals(true, matrix.allSizesValidated)
        assertEquals(true, matrix.allStatesValidated)
        assertEquals(true, matrix.composerAndChatUsableEverywhere)
        assertEquals(true, matrix.drawerModeValidated)
        assertEquals(true, matrix.lightAndDarkThemesValidated)
        assertEquals(true, matrix.acceptanceReady)
        assertTrue(matrix.productScore >= 95, matrix.summary)
        assertEquals(emptyList<String>(), matrix.automaticRejectConditions)
        assertTrue(matrix.sizeRequirements.any { it.kind == ComposeRuntimeScreenshotSizeKind.BASELINE_1360_860 && it.widthPx == 1360 && it.heightPx == 860 })
        assertTrue(matrix.sizeRequirements.any { it.kind == ComposeRuntimeScreenshotSizeKind.DRAWER_UNDER_1200 && it.responsiveState.drawerMode })
        assertTrue(matrix.stateRequirements.any { it.kind == ComposeRuntimeScreenshotStateKind.ATTACH_MENU_OPEN && it.evidence.contains("composer") })
        assertTrue(matrix.stateRequirements.any { it.kind == ComposeRuntimeScreenshotStateKind.DIAGNOSTICS_TECHNICAL_DETAILS_EXPANDED && it.evidence.contains("Technical details") })
        assertTrue(matrix.validationCopyText.contains("1360x860 baseline"))
        assertTrue(matrix.validationCopyText.contains("Light theme and dark theme"))
    }

    @Test
    fun shouldExposeContextualAttachmentToolsFromComposer() {
        val ready = ComposeAttachmentToolsState(peerSelected = true, fileTargetReady = true)
        val blocked = ComposeAttachmentToolsState(peerSelected = false, fileTargetReady = false)

        assertEquals("Attach", ready.title)
        assertEquals(
            listOf(
                "Send secure file",
                "Share on LAN temporarily",
                "Send encrypted text or file",
                "Hide message in image",
                "Extract hidden message",
            ),
            ready.primaryItems,
        )
        assertEquals(true, ready.keepsAdvancedToolsContextual)
        assertTrue(ready.summary.contains("selected peer"))
        assertTrue(blocked.summary.contains("Select an online peer"))
    }

    @Test
    fun shouldMapIncomingTransferPromptFromMetadata() {
        val metadata = FileTransferMetadata("rx-1", "Alice", "Bob", "archive.zip", 2_097_152)
        val prompt = ComposeIncomingTransferPrompt.from(metadata, "192.168.1.20")

        assertEquals("rx-1", prompt.id)
        assertEquals("Incoming file", prompt.title)
        assertEquals("Accept file from Alice?", prompt.header)
        assertEquals("2.00 MB", prompt.sizeLabel)
        assertEquals(ComposeIncomingTransferPromptStatus.WAITING, prompt.status)
        assertEquals(true, prompt.waitingForDecision)
        assertTrue(prompt.content.contains("archive.zip"))
        assertTrue(prompt.content.contains("192.168.1.20"))
    }

    @Test
    fun shouldExposeFileTransferStateReadinessAndRows() {
        val entry = TransferEntry("tx-1", "demo.bin", true, "Sending", 40, 4096).apply {
            updateProgress(2048, 50, 4096)
        }
        val state = ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            selectedFilePath = "C:/Users/Alice/demo.bin",
            senderId = "Alice",
            sessionPassword = "secret",
            entries = listOf(entry),
            incomingPrompts = listOf(ComposeIncomingTransferPrompt.from(FileTransferMetadata("rx", "Astra Laptop", "Me", "in.txt", 1024), "host")),
        )

        assertEquals("Encrypted file transfer", state.title)
        assertEquals(true, state.sendTargetReady)
        assertEquals(true, state.canSendSelectedFile)
        assertEquals(true, state.listenerReady)
        assertEquals("Send file ready", state.sendLabel)
        assertTrue(state.hint.contains("1 active"))
        assertTrue(state.entryRows.first().contains("Sending"))
        assertEquals("1 transfer active", state.heroTitle)
        assertTrue(state.heroSubtitle.contains("progress"))
        assertEquals("1 active · 0 completed · 1 needs review", state.transferCountSummary)
        assertEquals("Ask before saving", state.receiveModeShortLabel)
        assertEquals("demo.bin", state.selectedFileName)
        assertEquals("demo.bin", state.recentEntryRows.first().fileName)
        assertEquals("↑ Sent · demo.bin", state.recentEntryRows.first().title)
        assertEquals(2, state.chatAttachmentCards.size)
        assertEquals("Incoming file · in.txt", state.chatAttachmentCards.first().title)
        assertEquals("Needs review", state.chatAttachmentCards.first().progressLabel)
        assertEquals("Transferring · 50%", state.chatAttachmentCards.last().progressLabel)
        assertTrue(state.targetSummary.contains("Astra Laptop"))
        assertTrue(state.senderSummary.contains("Alice"))
        assertEquals("Using the current room password", state.passwordSummary)
        assertTrue(state.nextStepSummary.contains("Ready to send"))
        assertTrue(state.promptSummary.contains("Incoming files: 1"))
        assertEquals("Ask before saving incoming files", state.receiveModeLabel)
        assertEquals(emptyList<String>(), state.blockedReasons)
    }

    @Test
    fun shouldBlockFileTransferStateWithoutConnectionOrPeer() {
        val state = ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(clientConnected = false, clientFilePortText = "bad"),
            peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            javaFxFallbackAvailable = false,
        )

        assertEquals(false, state.sendTargetReady)
        assertEquals(false, state.canSendSelectedFile)
        assertEquals(false, state.listenerReady)
        assertEquals("Send file blocked", state.sendLabel)
        assertEquals("Receive listener blocked", state.receiveLabel)
        assertTrue(state.blockedReasons.any { it.contains("Connect to chat") })
        assertTrue(state.blockedReasons.any { it.contains("Select an online peer") })
        assertTrue(state.blockedReasons.any { it.contains("Choose a local file") })
        assertTrue(state.blockedReasons.any { it.contains("session password") })
        assertTrue(state.blockedReasons.any { it.contains("JavaFX fallback") })
        assertEquals("Ready when you need to send a file", state.heroTitle)
        assertTrue(state.targetSummary.contains("Select an online peer"))
        assertTrue(state.nextStepSummary.contains("Connect to chat"))
        assertEquals("No file selected", state.selectedFileName)
        assertEquals("Reconnect with a room password before sending files.", state.passwordSummary)
        assertEquals(listOf(state.recentEmptySituation, state.recentEmptyExplanation, state.recentEmptyNextAction), state.recentEmptyStructuredCopy)
        assertEquals(ComposeEmptyStateVisualWeight.INLINE, state.recentEmptyVisualWeight)
    }

    @Test
    fun shouldExposeUserFriendlyFileTransferRowsForCompletedAndFailedHistory() {
        val completed = TransferEntry("tx-2", "archive.zip", false, "Completed", 100, 1_048_576)
        val failed = TransferEntry("tx-3", "bad.bin", true, "Failed", 0, 0)
        val state = ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            entries = listOf(completed, failed),
            autoAcceptFiles = true,
        )

        assertEquals("Transfers are idle", state.heroTitle)
        assertTrue(state.heroSubtitle.contains("Recent completed or failed transfers"))
        assertEquals("0 active · 1 completed · 1 failed", state.transferCountSummary)
        assertEquals("Known peers auto-save", state.receiveModeShortLabel)
        assertEquals(2, state.recentEntryRows.size)
        assertEquals("↓ Received · archive.zip", state.recentEntryRows[0].title)
        assertEquals("Completed · 100%", state.recentEntryRows[0].progressLabel)
        assertEquals("1.00 MB", state.recentEntryRows[0].detail)
        assertEquals("↑ Sent · bad.bin", state.recentEntryRows[1].title)
        assertEquals("Failed", state.recentEntryRows[1].progressLabel)
        assertEquals(true, state.recentEntryRows[1].failed)
    }

    @Test
    fun shouldExposeQuickShareStateReadinessAndTrustedLanWarning() {
        val snapshot = QuickShareSnapshot(
            "share-1",
            QuickShareType.TEXT,
            "hello",
            "",
            0,
            Instant.parse("2026-05-25T19:00:00Z"),
            Instant.parse("2026-05-25T19:10:00Z"),
            3,
            1,
            QuickShareStatus.ACTIVE,
            listOf("http://127.0.0.1:5053/s/share-1"),
        )
        val state = ComposeQuickShareState(
            running = true,
            selectedFilePath = "C:/Temp/demo.txt",
            textDraft = "hello",
            entries = listOf(QuickShareEntry(snapshot)),
            landingUrls = listOf("http://127.0.0.1:5053/"),
        )

        assertEquals("Share by browser link", state.title)
        assertEquals(NetworkConstants.DEFAULT_QUICK_SHARE_PORT, state.port)
        assertEquals(false, state.canStartServer)
        assertEquals(true, state.canStopServer)
        assertEquals(true, state.canCreateFileShare)
        assertEquals(true, state.canCreateTextShare)
        assertEquals(true, state.canCopyIndex)
        assertTrue(state.statusText.contains("Quick share is active"))
        assertTrue(state.landingText.contains("Index"))
        assertTrue(state.trustedLanWarning.contains("Trusted LAN"))
        assertEquals("1 active link", state.activeShareCountLabel)
        assertEquals("No stopped or expired links", state.inactiveShareCountLabel)
        assertTrue(state.policySummary.contains("10 min"))
        assertEquals("hello", state.shareRowsDetailed.first().title)
        assertEquals("Text link", state.shareRowsDetailed.first().typeLabel)
        assertEquals("Active", state.shareRowsDetailed.first().statusLabel)
        assertTrue(state.shareRowsDetailed.first().detail.contains("1/3 opens"))
    }

    @Test
    fun shouldBlockQuickShareStateForInvalidInputsAndMissingFallback() {
        val state = ComposeQuickShareState(
            portText = "0",
            selectedFilePath = "",
            textDraft = " ",
            expirationMinutesText = "0",
            accessLimitText = "0",
            javaFxFallbackAvailable = false,
        )

        assertEquals(null, state.port)
        assertEquals(false, state.canStartServer)
        assertEquals(false, state.canCreateFileShare)
        assertEquals(false, state.canCreateTextShare)
        assertTrue(state.readinessSummary.contains("valid port"))
        assertTrue(state.readinessSummary.contains("expiration"))
        assertTrue(state.readinessSummary.contains("JavaFX fallback"))
    }

    @Test
    fun shouldReusePackagedJavaFxAppIconForComposeShell() {
        assertEquals("icons/app-icon.png", ComposeDesktopResources.APP_ICON_PNG)
        assertEquals("/icons/app-icon.png", ComposeDesktopResources.JAVA_FX_APP_ICON_RESOURCE)

        ComposeDesktopResources.openAppIconStream().use { stream ->
            assertNotNull(stream)
        }
    }

    @Test
    fun shouldCollapseStatusBarInputsIntoOneGlobalIndicator() {
        val offline = ComposeGlobalStatusIndicatorState(ComposeStatusConnectionState())
        val hosted = ComposeGlobalStatusIndicatorState(ComposeStatusConnectionState(localServerRunning = true, clientConnected = true))
        val connected = ComposeGlobalStatusIndicatorState(ComposeStatusConnectionState(clientConnected = true), peerStatus = "Peer Astra")
        val transfer = ComposeGlobalStatusIndicatorState(ComposeStatusConnectionState(clientConnected = true), transferStatus = "Transfer active")
        val call = ComposeGlobalStatusIndicatorState(ComposeStatusConnectionState(clientConnected = true), voiceStatus = "Voice call active")
        val issue = ComposeGlobalStatusIndicatorState(ComposeStatusConnectionState(connectionStatus = "Connection failed"))

        assertEquals("Offline", offline.label)
        assertEquals("Waiting for peers", hosted.label)
        assertEquals("Connected to secure room", connected.label)
        assertEquals("File transfer active", transfer.label)
        assertEquals("In call", call.label)
        assertEquals("Connection issue", issue.label)
        assertTrue(connected.detailText.contains("Peer Astra"))
    }

    @Test
    fun shouldExposeDefaultStatusConnectionAdapterStateWithoutRuntimeSideEffects() {
        val state = ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE

        assertEquals("Compose Preview", state.nickname)
        assertEquals("chatpass", state.roomPasswordPlaceholder)
        assertEquals("127.0.0.1", state.manualHost)
        assertEquals(NetworkConstants.DEFAULT_CHAT_PORT, state.chatPort)
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, state.filePort)
        assertEquals(NetworkConstants.DEFAULT_CHAT_PORT, state.clientChatPort)
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, state.clientFilePort)
        assertEquals(
            "Room chat ${NetworkConstants.DEFAULT_CHAT_PORT} · Room files ${NetworkConstants.DEFAULT_FILE_TRANSFER_PORT} · Join chat ${NetworkConstants.DEFAULT_CHAT_PORT} · Join files ${NetworkConstants.DEFAULT_FILE_TRANSFER_PORT}",
            state.portSummary
        )
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT + 1000, state.resolvedLocalFilePort)
        assertTrue(state.canOpenRoom)
        assertTrue(state.canConnect)
        assertEquals("Open room ready", state.actionState.openRoomLabel)
        assertEquals("Connect ready", state.actionState.connectLabel)
        assertEquals("Stop hosting blocked", state.actionState.stopHostingLabel)
        assertEquals("Disconnect blocked", state.actionState.disconnectLabel)
        assertTrue(state.validationSummary.contains("Ready to open or join a room."))
        assertEquals("Discoverable", state.discoverableLabel)
        assertEquals("JavaFX fallback available", state.fallbackLabel)
        assertTrue(state.discoveryStatus.contains("not started"))
        assertTrue(state.runtimePlan.hostingReady)
        assertTrue(state.runtimePlan.manualConnectionReady)
        assertEquals(false, state.eventPreview.hasErrors)
        assertEquals(
            listOf(ComposeConnectionCommandKind.OPEN_ROOM, ComposeConnectionCommandKind.CONNECT),
            state.controlPlan.enabledCommands.map { it.kind },
        )
    }

    @Test
    fun shouldExposeComposeSideCommandControlPlanForReadyStatusConnectionActions() {
        val state = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20")
        val controlPlan = state.controlPlan

        assertEquals("Status/connection control boundary", controlPlan.title)
        assertEquals(5, controlPlan.commands.size)
        assertEquals(
            ComposeConnectionCommandKind.OPEN_ROOM,
            controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM).kind
        )
        assertEquals(true, controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM).enabled)
        assertEquals(true, controlPlan.command(ComposeConnectionCommandKind.CONNECT).enabled)
        assertEquals(false, controlPlan.command(ComposeConnectionCommandKind.STOP_HOSTING).enabled)
        assertEquals(false, controlPlan.command(ComposeConnectionCommandKind.DISCONNECT).enabled)
        assertEquals(false, controlPlan.command(ComposeConnectionCommandKind.SET_DISCOVERABLE).enabled)
        assertTrue(controlPlan.enabledSummary.contains("Open room"))
        assertTrue(controlPlan.enabledSummary.contains("Connect"))
        assertTrue(controlPlan.disabledSummary.contains("Stop hosting"))
        assertTrue(controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM).queuedEvent.message.contains("open-room"))
    }

    @Test
    fun shouldExposeUserFriendlyConnectionHubModeCopyAndJoinTargetSummary() {
        val hostState = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(localServerRunning = true, discoverable = true),
            mode = ComposeConnectionHubMode.HOST,
            localNetworkInfo = "[info] local network IPs: 192.168.1.10",
        )
        val joinState = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(manualHost = "192.168.1.20"),
            mode = ComposeConnectionHubMode.JOIN,
        )

        assertEquals("Visible to nearby devices", hostState.hostChoiceSubtitle)
        assertEquals("Host a secure room", hostState.activeModeTitle)
        assertTrue(hostState.activeModeDetail.contains("trusted room"))
        assertEquals("Ready for 192.168.1.20", joinState.joinChoiceSubtitle)
        assertEquals("Join a secure room", joinState.activeModeTitle)
        assertTrue(joinState.activeModeDetail.contains("Advanced connection"))
        assertTrue(joinState.joinTargetSummary.contains("192.168.1.20:${NetworkConstants.DEFAULT_CHAT_PORT}"), joinState.joinTargetSummary)
    }

    @Test
    fun shouldBlockComposeSideCommandControlPlanWhenFallbackIsUnavailable() {
        val state = ComposeStatusConnectionState(javaFxFallbackAvailable = false)
        val controlPlan = state.controlPlan

        assertEquals(emptyList<ComposeConnectionCommand>(), controlPlan.enabledCommands)
        assertEquals(5, controlPlan.disabledCommands.size)
        assertTrue(controlPlan.disabledSummary.contains("Open room"))
        assertTrue(controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM).blockedReason.contains("JavaFX fallback"))
        assertEquals("Open room blocked", controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM).displayLabel)
    }

    @Test
    fun shouldExposeHostedStateCommandControlPlanForStopAndDiscoveryToggle() {
        val state = ComposeStatusConnectionState(localServerRunning = true, discoverable = false)
        val controlPlan = state.controlPlan

        assertEquals(false, controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM).enabled)
        assertEquals(true, controlPlan.command(ComposeConnectionCommandKind.STOP_HOSTING).enabled)
        assertEquals(true, controlPlan.command(ComposeConnectionCommandKind.SET_DISCOVERABLE).enabled)
        assertEquals("Make discoverable", controlPlan.command(ComposeConnectionCommandKind.SET_DISCOVERABLE).label)
        assertTrue(controlPlan.command(ComposeConnectionCommandKind.SET_DISCOVERABLE).summary.contains("Show the room to nearby trusted peers."))
    }

    @Test
    fun shouldExposeValidIdleLifecycleContractWithoutRuntimeSideEffects() {
        val state = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20")
        val lifecycle = state.lifecyclePlan

        assertEquals("Live status/connection binding contract", lifecycle.title)
        assertEquals(ComposeConnectionLifecycleState.IDLE, lifecycle.currentState)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.IDLE).ready)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.HOSTING_READY).ready)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.CONNECTING_READY).ready)
        assertEquals(false, lifecycle.step(ComposeConnectionLifecycleState.HOSTED).ready)
        assertEquals(emptyList<String>(), lifecycle.blockedReasons)
        assertEquals(true, lifecycle.rollbackFallbackRequired)
        assertTrue(lifecycle.readinessSummary.contains("Hosting-ready"))
        assertTrue(lifecycle.sideEffectContractSummary.contains("without invoking services"))
        assertTrue(lifecycle.cleanupOrderSummary.contains("side-effect free"))
    }

    @Test
    fun shouldExposeHostedLifecycleContractAndCleanupOrder() {
        val state = ComposeStatusConnectionState(localServerRunning = true, discoverable = false)
        val lifecycle = state.lifecyclePlan

        assertEquals(ComposeConnectionLifecycleState.HOSTED, lifecycle.currentState)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.HOSTED).ready)
        assertEquals(false, lifecycle.step(ComposeConnectionLifecycleState.HOSTING_READY).ready)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.CONNECTING_READY).ready)
        assertTrue(lifecycle.cleanupOrderSummary.contains("Stop discovery announcement"))
        assertTrue(lifecycle.cleanupOrderSummary.contains("Stop hosted chat server"))
        assertEquals("JavaFX fallback available for rollback", lifecycle.fallbackStatus)
    }

    @Test
    fun shouldExposeConnectedLifecycleContractAndCleanupOrder() {
        val state = ComposeStatusConnectionState(clientConnected = true)
        val lifecycle = state.lifecyclePlan

        assertEquals(ComposeConnectionLifecycleState.CONNECTED, lifecycle.currentState)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.CONNECTED).ready)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.HOSTING_READY).ready)
        assertEquals(false, lifecycle.step(ComposeConnectionLifecycleState.CONNECTING_READY).ready)
        assertTrue(lifecycle.cleanupOrderSummary.contains("Disconnect chat client"))
        assertTrue(lifecycle.cleanupOrderSummary.contains("Stop client-only local file listener"))
        assertTrue(lifecycle.cleanupOrderSummary.contains("Return to listen-only discovery"))
    }

    @Test
    fun shouldBlockLifecycleContractForInvalidPortsAndBlankNickname() {
        val state = ComposeStatusConnectionState(
            nickname = " ",
            serverChatPortText = "0",
            clientChatPortText = "abc",
        )
        val lifecycle = state.lifecyclePlan

        assertEquals(ComposeConnectionLifecycleState.BLOCKED_ERROR, lifecycle.currentState)
        assertEquals(true, lifecycle.step(ComposeConnectionLifecycleState.BLOCKED_ERROR).ready)
        assertEquals(false, lifecycle.step(ComposeConnectionLifecycleState.HOSTING_READY).ready)
        assertEquals(false, lifecycle.step(ComposeConnectionLifecycleState.CONNECTING_READY).ready)
        assertTrue(lifecycle.blockedReasons.any { it.contains("Nickname is blank") })
        assertTrue(lifecycle.blockedReasons.any { it.contains("Room ports") })
        assertTrue(lifecycle.blockedReasons.any { it.contains("Manual connection ports") })
        assertTrue(lifecycle.cleanupOrderSummary.contains("Do not invoke runtime services"))
    }

    @Test
    fun shouldBlockLifecycleContractWhenFallbackIsUnavailable() {
        val state = ComposeStatusConnectionState(javaFxFallbackAvailable = false)
        val lifecycle = state.lifecyclePlan

        assertEquals(ComposeConnectionLifecycleState.BLOCKED_ERROR, lifecycle.currentState)
        assertEquals(false, lifecycle.fallbackAvailable)
        assertEquals(true, lifecycle.rollbackFallbackRequired)
        assertTrue(lifecycle.fallbackStatus.contains("unavailable"))
        assertTrue(lifecycle.blockedReasons.any { it.contains("JavaFX fallback") })
        assertTrue(lifecycle.blockedSummary.contains("rollback safety"))
    }

    @Test
    fun shouldSummarizeLifecycleCleanupOrderDeterministically() {
        val hosted = ComposeStatusConnectionState(localServerRunning = true).lifecyclePlan
        val connected = ComposeStatusConnectionState(clientConnected = true, localServerRunning = true).lifecyclePlan

        assertEquals(
            "Stop discovery announcement → Disconnect local self-client if attached → Stop hosted chat server → Stop hosted file listener → Return to listen-only discovery",
            hosted.cleanupOrderSummary,
        )
        assertEquals(
            "Disconnect chat client → Stop client-only local file listener → Keep hosted room running until Stop hosting is requested → Return to listen-only discovery",
            connected.cleanupOrderSummary,
        )
    }

    @Test
    fun shouldExposeReadyTransitionIntentsForIdleStatusConnectionState() {
        val state = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20")
        val transitions = state.transitionPlan

        assertEquals("Status/connection transition intents", transitions.title)
        assertEquals(
            listOf(ComposeConnectionTransitionKind.START_HOSTING, ComposeConnectionTransitionKind.START_MANUAL_CONNECT),
            transitions.enabledTransitions.map { it.kind },
        )
        assertEquals(
            ComposeConnectionLifecycleState.IDLE,
            transitions.transition(ComposeConnectionTransitionKind.START_HOSTING).sourceState
        )
        assertEquals(
            ComposeConnectionLifecycleState.HOSTED,
            transitions.transition(ComposeConnectionTransitionKind.START_HOSTING).targetState
        )
        assertTrue(transitions.transition(ComposeConnectionTransitionKind.START_HOSTING).queuedEvent.message.contains("start-hosting"))
        assertTrue(
            transitions.transition(ComposeConnectionTransitionKind.START_MANUAL_CONNECT).sideEffectContract.contains(
                "chat-core"
            )
        )
        assertTrue(transitions.cleanupSummary.contains("Start hosting transition"))
    }

    @Test
    fun shouldExposeHostedTransitionIntentsForStopAndDiscoveryVisibilityOnly() {
        val state = ComposeStatusConnectionState(localServerRunning = true, discoverable = false)
        val transitions = state.transitionPlan

        assertEquals(
            listOf(
                ComposeConnectionTransitionKind.STOP_HOSTING,
                ComposeConnectionTransitionKind.CHANGE_DISCOVERY_VISIBILITY
            ),
            transitions.enabledTransitions.map { it.kind },
        )
        assertEquals(false, transitions.transition(ComposeConnectionTransitionKind.START_HOSTING).enabled)
        assertEquals(
            ComposeConnectionLifecycleState.HOSTED,
            transitions.transition(ComposeConnectionTransitionKind.STOP_HOSTING).sourceState
        )
        assertEquals(
            ComposeConnectionLifecycleState.IDLE,
            transitions.transition(ComposeConnectionTransitionKind.STOP_HOSTING).targetState
        )
        assertTrue(transitions.transition(ComposeConnectionTransitionKind.STOP_HOSTING).cleanupPreview.contains("Stop discovery announcement"))
        assertTrue(
            transitions.transition(ComposeConnectionTransitionKind.CHANGE_DISCOVERY_VISIBILITY).guardSummary.contains(
                "UDP payload format unchanged"
            )
        )
    }

    @Test
    fun shouldExposeConnectedTransitionIntentForDisconnect() {
        val state = ComposeStatusConnectionState(clientConnected = true)
        val transitions = state.transitionPlan

        assertEquals(
            listOf(ComposeConnectionTransitionKind.DISCONNECT_CLIENT),
            transitions.enabledTransitions.map { it.kind })
        assertEquals(
            ComposeConnectionLifecycleState.CONNECTED,
            transitions.transition(ComposeConnectionTransitionKind.DISCONNECT_CLIENT).sourceState
        )
        assertEquals(
            ComposeConnectionLifecycleState.IDLE,
            transitions.transition(ComposeConnectionTransitionKind.DISCONNECT_CLIENT).targetState
        )
        assertTrue(transitions.transition(ComposeConnectionTransitionKind.DISCONNECT_CLIENT).cleanupPreview.contains("Disconnect chat client"))
        assertTrue(transitions.blockedSummary.contains("Manual connect transition"))
    }

    @Test
    fun shouldBlockTransitionIntentsForInvalidOrFallbackUnavailableState() {
        val invalid = ComposeStatusConnectionState(nickname = " ", serverChatPortText = "0", clientChatPortText = "abc")
        val fallbackUnavailable = ComposeStatusConnectionState(javaFxFallbackAvailable = false)

        assertEquals(emptyList<ComposeConnectionTransitionIntent>(), invalid.transitionPlan.enabledTransitions)
        assertEquals(5, invalid.transitionPlan.blockedTransitions.size)
        assertTrue(
            invalid.transitionPlan.transition(ComposeConnectionTransitionKind.START_HOSTING).blockedReason.contains(
                "Hosting plan unavailable"
            )
        )
        assertEquals(
            emptyList<ComposeConnectionTransitionIntent>(),
            fallbackUnavailable.transitionPlan.enabledTransitions
        )
        assertTrue(
            fallbackUnavailable.transitionPlan.transition(ComposeConnectionTransitionKind.START_HOSTING).blockedReason.contains(
                "JavaFX fallback"
            )
        )
        assertTrue(fallbackUnavailable.transitionPlan.blockedSummary.contains("Manual connect transition"))
    }

    @Test
    fun shouldBuildRuntimeNeutralConnectionPlanForValidStatusConnectionState() {
        val state = ComposeStatusConnectionState(nickname = " Alice ", manualHost = " 192.168.1.20 ")
        val plan = state.runtimePlan

        assertEquals(NetworkConstants.DEFAULT_CHAT_PORT, plan.chatServerConfig?.port)
        assertEquals("chatpass", plan.chatServerConfig?.sessionPassword)
        assertEquals("127.0.0.1", plan.localHostConnectRequest?.host)
        assertEquals(NetworkConstants.DEFAULT_CHAT_PORT, plan.localHostConnectRequest?.port)
        assertEquals("Alice", plan.localHostConnectRequest?.nickname)
        assertEquals("192.168.1.20", plan.manualConnectRequest?.host)
        assertEquals(NetworkConstants.DEFAULT_CHAT_PORT, plan.manualConnectRequest?.port)
        assertEquals("Alice", plan.manualConnectRequest?.nickname)
        assertEquals(NetworkConstants.DEFAULT_DISCOVERY_PORT, plan.hostingDiscoveryConfig?.discoveryPort)
        assertEquals(NetworkConstants.DEFAULT_CHAT_PORT, plan.hostingDiscoveryConfig?.chatPort)
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, plan.hostingDiscoveryConfig?.filePort)
        assertEquals(true, plan.hostingDiscoveryConfig?.announceEnabled)
        assertEquals(NetworkConstants.DEFAULT_DISCOVERY_PORT, plan.listenOnlyDiscoveryConfig.discoveryPort)
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT + 1000, plan.localFileListenerPort)
        assertEquals(emptyList<String>(), plan.disabledReasons)
        assertTrue(plan.discoveryAnnouncement.contains("broadcasting as Alice"))
        assertTrue(plan.hostingSummary.contains("Host chat on ${NetworkConstants.DEFAULT_CHAT_PORT}"))
        assertTrue(plan.manualConnectionSummary.contains("192.168.1.20:${NetworkConstants.DEFAULT_CHAT_PORT}"))
    }

    @Test
    fun shouldExposeRuntimeNeutralConnectionEventPreviewForValidState() {
        val state = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20")
        val preview = state.eventPreview

        assertEquals("Status/connection event preview", preview.title)
        assertEquals(false, preview.hasErrors)
        assertEquals(false, preview.hasWarnings)
        assertTrue(preview.summary.contains("ready"))
        assertTrue(preview.events.any { it.kind == ComposeConnectionEventKind.SUCCESS && it.message.contains("Host chat on ${NetworkConstants.DEFAULT_CHAT_PORT}") })
        assertTrue(preview.events.any { it.kind == ComposeConnectionEventKind.SUCCESS && it.message.contains("192.168.1.20:${NetworkConstants.DEFAULT_CHAT_PORT}") })
        assertTrue(preview.events.any { it.displayText.startsWith("info:") })
        assertTrue(preview.latestMessage.contains("192.168.1.20"))
    }

    @Test
    fun shouldExposeWarningsAndErrorsInRuntimeNeutralConnectionEventPreviewForBlockedState() {
        val state = ComposeStatusConnectionState(
            nickname = " ",
            manualHost = " ",
            serverChatPortText = "0",
            serverFilePortText = "70000",
            clientChatPortText = "abc",
            clientFilePortText = "65536",
            javaFxFallbackAvailable = false,
        )
        val preview = state.eventPreview

        assertEquals(true, preview.hasErrors)
        assertEquals(true, preview.hasWarnings)
        assertTrue(preview.summary.contains("Blocked"))
        assertTrue(preview.events.any { it.kind == ComposeConnectionEventKind.WARNING && it.message.contains("Hosting plan unavailable") })
        assertTrue(preview.events.any { it.kind == ComposeConnectionEventKind.WARNING && it.message.contains("Manual connection plan unavailable") })
        assertTrue(preview.events.any { it.kind == ComposeConnectionEventKind.ERROR && it.message.contains("Can't open room") })
        assertTrue(preview.events.any { it.kind == ComposeConnectionEventKind.ERROR && it.message.contains("Can't join room") })
        assertTrue(preview.events.any { it.kind == ComposeConnectionEventKind.ERROR && it.message.contains("JavaFX fallback is unavailable") })
        assertTrue(preview.latestMessage.contains("JavaFX fallback"))
    }

    @Test
    fun shouldBuildHiddenDiscoveryPlanWhenStatusConnectionStateIsNotDiscoverable() {
        val state = ComposeStatusConnectionState(discoverable = false)
        val plan = state.runtimePlan

        assertEquals(false, plan.hostingDiscoveryConfig?.announceEnabled)
        assertTrue(plan.discoveryAnnouncement.contains("room is hidden"))
    }

    @Test
    fun shouldBlockRuntimeConnectionPlanWhenStatusConnectionInputsAreInvalid() {
        val state = ComposeStatusConnectionState(
            nickname = " ",
            manualHost = " ",
            serverChatPortText = "0",
            serverFilePortText = "70000",
            clientChatPortText = "abc",
            clientFilePortText = "65536",
        )
        val plan = state.runtimePlan

        assertEquals(null, plan.chatServerConfig)
        assertEquals(null, plan.localHostConnectRequest)
        assertEquals(null, plan.manualConnectRequest)
        assertEquals(null, plan.hostingDiscoveryConfig)
        assertEquals(null, plan.localFileListenerPort)
        assertEquals(false, plan.hostingReady)
        assertEquals(false, plan.manualConnectionReady)
        assertTrue(plan.discoveryAnnouncement.contains("listening on UDP ${NetworkConstants.DEFAULT_DISCOVERY_PORT}"))
        assertTrue(plan.disabledReasons.any { it.contains("Can't open room") })
        assertTrue(plan.disabledReasons.any { it.contains("Can't join room") })
    }

    @Test
    fun shouldFormatHiddenAndFallbackUnavailableLabelsForStatusConnectionAdapterState() {
        val state = ComposeStatusConnectionState(
            discoverable = false,
            javaFxFallbackAvailable = false,
        )

        assertEquals("Hidden", state.discoverableLabel)
        assertEquals("JavaFX fallback unavailable", state.fallbackLabel)
    }

    @Test
    fun shouldBlockStatusConnectionActionsWhenNicknameIsBlank() {
        val state = ComposeStatusConnectionState(nickname = "   ")

        assertEquals(false, state.nicknameValid)
        assertEquals(false, state.canOpenRoom)
        assertEquals(false, state.canConnect)
        assertEquals(false, state.actionState.openRoomReady)
        assertEquals(false, state.actionState.connectReady)
        assertTrue(state.validationSummary.contains("Enter a name"))
    }

    @Test
    fun shouldBlockOpenRoomWhenServerPortsAreInvalid() {
        val state = ComposeStatusConnectionState(serverChatPortText = "0", serverFilePortText = "70000")

        assertEquals(null, state.serverChatPort)
        assertEquals(null, state.serverFilePort)
        assertEquals(false, state.canOpenRoom)
        assertEquals("Open room blocked", state.actionState.openRoomLabel)
        assertTrue(state.validationSummary.contains("Room chat and file ports must be numbers from 1 to 65535."))
    }

    @Test
    fun shouldBlockManualConnectWhenHostOrClientPortsAreInvalid() {
        val blankHost = ComposeStatusConnectionState(manualHost = "   ")
        val invalidPorts = ComposeStatusConnectionState(clientChatPortText = "abc", clientFilePortText = "65536")

        assertEquals(false, blankHost.manualHostValid)
        assertEquals(false, blankHost.canConnect)
        assertTrue(blankHost.validationSummary.contains("Enter a room address before connecting manually."))
        assertEquals(null, invalidPorts.clientChatPort)
        assertEquals(null, invalidPorts.clientFilePort)
        assertEquals(false, invalidPorts.canConnect)
        assertEquals(null, invalidPorts.resolvedLocalFilePort)
        assertEquals("Connect blocked", invalidPorts.actionState.connectLabel)
        assertTrue(invalidPorts.validationSummary.contains("Connection ports must be numbers from 1 to 65535."))
    }

    @Test
    fun shouldReflectRuntimeGatesForStatusConnectionPreviewActions() {
        val serverRunning = ComposeStatusConnectionState(localServerRunning = true)
        val clientConnected = ComposeStatusConnectionState(clientConnected = true)

        assertEquals(false, serverRunning.canOpenRoom)
        assertEquals(true, serverRunning.canConnect)
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, serverRunning.resolvedLocalFilePort)
        assertEquals(true, serverRunning.actionState.stopHostingReady)
        assertEquals(true, serverRunning.actionState.discoverabilityToggleReady)
        assertTrue(serverRunning.actionState.diagnosticSummary.contains("Stop hosting ready"))
        assertTrue(serverRunning.validationSummary.contains("Room is open"))
        assertEquals(true, clientConnected.canOpenRoom)
        assertEquals(false, clientConnected.canConnect)
        assertEquals(true, clientConnected.actionState.disconnectReady)
        assertEquals("Disconnect ready", clientConnected.actionState.disconnectLabel)
        assertTrue(clientConnected.validationSummary.contains("Already connected to a room; disconnect first."))
    }

    @Test
    fun shouldExposeUnifiedConnectionHubStateForHostMode() {
        val state = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(nickname = "Alice"),
            mode = ComposeConnectionHubMode.HOST,
            localNetworkInfo = "LAN: 192.168.1.10",
        )

        assertEquals("Host a secure room", state.title)
        assertEquals("Host secure room", state.hostTabLabel)
        assertEquals("Join nearby room", state.joinTabLabel)
        assertEquals("Alice", state.nickname)
        assertEquals(true, state.primaryActionEnabled)
        assertEquals("Start secure room", state.primaryActionLabel)
        assertEquals(false, state.secondaryActionEnabled)
        assertEquals("Stop hosting", state.secondaryActionLabel)
        assertEquals(null, state.activeBadgeLabel)
        assertTrue(state.modeHint.contains("nearby trusted people"))
        assertTrue(state.networkInfoSummary.contains("192.168.1.10"))
        assertEquals("Advanced hosting settings", state.advancedSettingsTitle)
        assertEquals(null, state.blockedReason)
        assertEquals(null, state.statusMessage)
        assertEquals(true, state.discoverableToggleEnabled)
        assertEquals(false, state.copyRoomAddressEnabled)
    }

    @Test
    fun shouldExposeUnifiedConnectionHubStateForJoinMode() {
        val state = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20"),
            mode = ComposeConnectionHubMode.JOIN,
        )

        assertEquals(true, state.primaryActionEnabled)
        assertEquals("Join Room", state.primaryActionLabel)
        assertEquals(false, state.secondaryActionEnabled)
        assertEquals("Disconnect", state.secondaryActionLabel)
        assertEquals(null, state.activeBadgeLabel)
        assertTrue(state.modeHint.contains("nearby trusted room"))
        assertTrue(state.networkInfoSummary.contains("Nearby rooms"))
        assertEquals("Advanced connection", state.advancedSettingsTitle)
        assertEquals(null, state.blockedReason)
        assertEquals(null, state.statusMessage)
    }

    @Test
    fun shouldBlockConnectionHubPrimaryActionAndShowRecoveryReason() {
        val blankNickname = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(nickname = " "),
            mode = ComposeConnectionHubMode.HOST,
        )
        val badHost = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(nickname = "Alice", manualHost = " "),
            mode = ComposeConnectionHubMode.JOIN,
        )
        val noFallback = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(javaFxFallbackAvailable = false),
            mode = ComposeConnectionHubMode.HOST,
        )

        assertEquals(false, blankNickname.primaryActionEnabled)
        assertTrue(blankNickname.blockedReason?.contains("Enter your name") == true)
        assertEquals(false, badHost.primaryActionEnabled)
        assertTrue(badHost.blockedReason?.contains("Enter the address of the room you want to join.") == true)
        assertEquals(false, noFallback.primaryActionEnabled)
        assertTrue(noFallback.blockedReason?.contains("JavaFX fallback") == true)
        assertEquals(ComposeConnectionHubMessageTone.ERROR, blankNickname.statusMessageTone)
    }

    @Test
    fun shouldUpdateConnectionHubPrimaryLabelForActiveConnectionWithoutErrorBanner() {
        val hosted = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(localServerRunning = true),
            mode = ComposeConnectionHubMode.HOST,
        )
        val connected = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            mode = ComposeConnectionHubMode.JOIN,
        )

        assertEquals("Start secure room", hosted.primaryActionLabel)
        assertEquals(true, hosted.secondaryActionEnabled)
        assertEquals("Room open", hosted.activeBadgeLabel)
        assertEquals(null, hosted.blockedReason)
        assertTrue(hosted.statusMessage?.contains("Room is open") == true)
        assertEquals(ComposeConnectionHubMessageTone.SUCCESS, hosted.statusMessageTone)
        assertEquals("Join Room", connected.primaryActionLabel)
        assertEquals(true, connected.secondaryActionEnabled)
        assertEquals("Connected", connected.activeBadgeLabel)
        assertEquals(null, connected.blockedReason)
        assertTrue(connected.statusMessage?.contains("Connected") == true)
        assertEquals(ComposeConnectionHubMessageTone.SUCCESS, connected.statusMessageTone)
    }

    @Test
    fun shouldSurfaceHiddenHostedRoomAsNormalStatus() {
        val state = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(localServerRunning = true, discoverable = false),
            mode = ComposeConnectionHubMode.HOST,
        )

        assertEquals("Start secure room", state.primaryActionLabel)
        assertEquals("Hidden room open", state.activeBadgeLabel)
        assertEquals(null, state.blockedReason)
        assertTrue(state.statusMessage?.contains("hidden mode") == true)
        assertEquals(ComposeConnectionHubMessageTone.SUCCESS, state.statusMessageTone)
    }

    @Test
    fun shouldEnableCopyRoomAddressForActiveHostedRoomWithNetworkInfo() {
        val state = ComposeConnectionHubState(
            statusState = ComposeStatusConnectionState(localServerRunning = true),
            mode = ComposeConnectionHubMode.HOST,
            localNetworkInfo = "[info] local network IPs: 192.168.1.10",
        )

        assertEquals(true, state.copyRoomAddressEnabled)
        assertTrue(state.copyRoomAddressText.contains("192.168.1.10"))
    }

    @Test
    fun shouldClassifyChatTranscriptLinesForVisualRendering() {
        val fixedTime = Instant.parse("2026-05-26T20:15:00Z")
        val local = ComposeChatTranscriptLinePresentation.from("Frank: hello", "Frank", fixedTime)
        val remote = ComposeChatTranscriptLinePresentation.from("Uma: hi", "Frank", fixedTime)
        val presence = ComposeChatTranscriptLinePresentation.from("[join] Uma", "Frank", fixedTime)
        val security = ComposeChatTranscriptLinePresentation.from("[error] Connection failed", "Frank", fixedTime)
        val diagnostic = ComposeChatTranscriptLinePresentation.from("[info] local network IPs: 192.168.1.10", "Frank", fixedTime)
        val normalizedSystem = ComposeChatTranscriptLinePresentation.from("system: [system] Uma joined the chat", "Frank", fixedTime)
        val transfer = ComposeChatTranscriptLinePresentation.from("[file-send] started: demo.txt", "Frank", fixedTime)
        val call = ComposeChatTranscriptLinePresentation.from("[call] Voice call started", "Frank", fixedTime)

        assertEquals(ComposeChatTranscriptLineKind.USER_LOCAL, local.kind)
        assertEquals("hello", local.body)
        assertEquals("You", local.label)
        assertTrue(local.displayTime.matches(Regex("\\d{2}:\\d{2}")))
        assertEquals(ComposeChatTranscriptLineKind.USER_REMOTE, remote.kind)
        assertEquals("Uma", remote.label)
        assertEquals("hi", remote.body)
        assertEquals(ComposeChatTranscriptLineKind.PRESENCE, presence.kind)
        assertEquals("joined: Uma", presence.body)
        assertEquals(ComposeChatTranscriptLineKind.SECURITY, security.kind)
        assertEquals("Connection failed", security.body)
        assertEquals(ComposeChatTranscriptLineKind.DIAGNOSTIC, diagnostic.kind)
        assertEquals("Uma joined the chat", normalizedSystem.body)
        assertEquals(ComposeChatTranscriptLineKind.SYSTEM, normalizedSystem.kind)
        assertEquals(ComposeChatTranscriptLineKind.TRANSFER, transfer.kind)
        assertEquals("sent: started: demo.txt", transfer.body)
        assertEquals(ComposeChatTranscriptLineKind.CALL, call.kind)
        assertEquals("Voice call started", call.body)
    }

    @Test
    fun shouldKeepSharedRoomVisibilityOnlyInTooltipMetadata() {
        val state = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
        )

        assertEquals("Shared room chat", state.title)
        assertTrue(state.subtitle.contains("visible to everyone"))
        assertFalse(state.transcriptLines.any { it.contains("Messages are visible to everyone in this room.") })
    }

    @Test
    fun shouldExposeChatMessageTimeMetadataForRuntimeTranscriptLines() {
        val message = ComposeChatMessage.fromTranscriptLine(
            "Uma: a long message that can wrap without changing the transcript text",
            Instant.parse("2026-05-26T20:16:00Z"),
        )
        val state = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            messages = listOf(message),
        )

        assertEquals(listOf("Uma: a long message that can wrap without changing the transcript text"), state.transcriptLines)
        assertEquals(1, state.transcriptMessageTimes.size)
        assertTrue(state.transcriptMessageTimes.single().matches(Regex("\\d{2}:\\d{2}")))
    }

    @Test
    fun shouldExposeDefaultPeerListAdapterStateWithoutRuntimeSideEffects() {
        val state = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE

        assertEquals("Contacts / Peers", state.title)
        assertTrue(state.hint.contains("Discovered LAN peers"))
        assertEquals(listOf("Astra Laptop", "Beta Phone", "Offline NAS"), state.visiblePeers.map { it.nickname })
        assertEquals("Astra Laptop", state.selectedPeerTitle)
        assertEquals("Peer Astra Laptop", state.peerStatus)
        assertEquals("JavaFX peer list remains production fallback", state.fallbackLabel)
        assertTrue(state.selectedPeerMeta.contains("connect to chat"))
        assertTrue(state.actionSummary.contains("encrypted file transfer"))
        assertEquals(true, state.targetActions.chatReady)
        assertEquals(true, state.targetActions.fileReady)
        assertEquals(true, state.targetActions.voiceReady)
        assertEquals(true, state.targetActions.videoReady)
        assertEquals(true, state.targetActions.dataChannelReady)
        assertEquals(emptyList<String>(), state.targetActions.blockedReasons)
        assertEquals(5, state.targetControlPlan.enabledCommands.size)
        assertEquals(emptyList<ComposePeerTargetCommand>(), state.targetControlPlan.disabledCommands)
    }

    @Test
    fun shouldGroupPeerListByOnlineAndOfflineForClearerScanning() {
        val state = ComposeShellMetadata.DEFAULT_PEER_LIST_STATE

        assertEquals(true, state.hasAnyPeers)
        assertEquals(listOf("Astra Laptop", "Beta Phone"), state.onlinePeers.map { it.nickname })
        assertEquals(listOf("Offline NAS"), state.offlinePeers.map { it.nickname })
        assertTrue(state.emptyStateTitle.contains("No peers"))
        assertTrue(state.emptyStateNextAction.contains("Open or join"))
    }

    @Test
    fun shouldExposePeerListEmptyStateWhenNoPeersAreVisible() {
        val state = ComposePeerListState(peers = emptyList())

        assertEquals(false, state.hasAnyPeers)
        assertEquals(emptyList<ComposePeerListItem>(), state.onlinePeers)
        assertEquals(emptyList<ComposePeerListItem>(), state.offlinePeers)
        assertTrue(state.emptyStateTitle.contains("No peers"))
        assertTrue(state.emptyStateDetail.contains("Advanced connection"))
        assertEquals(listOf(state.emptyStateSituation, state.emptyStateExplanation, state.emptyStateNextAction), state.emptyStateStructuredCopy)
        assertEquals(ComposeEmptyStateVisualWeight.SUPPORTING, state.emptyStateVisualWeight)
        assertEquals(true, state.emptyStateKeepsConversationDominant)
    }

    @Test
    fun shouldKeepEmptyStatesStructuredAndVisuallySubordinate() {
        val peerState = ComposePeerListState(peers = emptyList())
        val chatState = ComposeChatWorkspaceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = peerState,
            messages = emptyList(),
        )
        val transferState = ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = peerState,
        )
        val diagnosticsState = ComposeDiagnosticsState(
            statusState = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20"),
            peerListState = peerState,
        )
        val quickShareState = ComposeQuickShareState(running = false)

        val structuredCopies = listOf(
            peerState.emptyStateStructuredCopy,
            chatState.transcriptEmptyStructuredCopy,
            transferState.recentEmptyStructuredCopy,
            diagnosticsState.noDiagnosticsStructuredCopy,
            quickShareState.emptySharesStructuredCopy,
        )

        assertTrue(structuredCopies.all { it.size == 3 })
        assertTrue(structuredCopies.all { parts -> parts.all(String::isNotBlank) })
        assertEquals(ComposeEmptyStateVisualWeight.SUPPORTING, peerState.emptyStateVisualWeight)
        assertEquals(ComposeEmptyStateVisualWeight.PRIMARY_GUIDANCE, chatState.transcriptEmptyVisualWeight)
        assertEquals(ComposeEmptyStateVisualWeight.INLINE, transferState.recentEmptyVisualWeight)
        assertEquals(ComposeEmptyStateVisualWeight.INLINE, diagnosticsState.noDiagnosticsVisualWeight)
        assertEquals(ComposeEmptyStateVisualWeight.INLINE, quickShareState.emptySharesVisualWeight)
        assertTrue(peerState.emptyStateKeepsConversationDominant)
    }

    @Test
    fun shouldExposePeerTargetControlPlanForOnlineDiscoveredPeer() {
        val state = ComposePeerListState(selectedPeerIndex = 0)
        val controlPlan = state.targetControlPlan

        assertEquals("Selected-peer command boundary", controlPlan.title)
        assertEquals(5, controlPlan.commands.size)
        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET).enabled)
        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).enabled)
        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.VOICE_TARGET).enabled)
        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.VIDEO_TARGET).enabled)
        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.DATA_TARGET).enabled)
        assertTrue(controlPlan.enabledSummary.contains("Use for files"))
        assertTrue(controlPlan.disabledSummary.contains("No selected-peer commands"))
        assertTrue(controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).queuedEvent.message.contains("file-target"))
    }

    @Test
    fun shouldBlockPeerTargetControlPlanForFallbackUnavailableState() {
        val state = ComposePeerListState(selectedPeerIndex = 0, javaFxFallbackAvailable = false)
        val controlPlan = state.targetControlPlan

        assertEquals(emptyList<ComposePeerTargetCommand>(), controlPlan.enabledCommands)
        assertEquals(5, controlPlan.disabledCommands.size)
        assertTrue(controlPlan.disabledSummary.contains("Use for chat"))
        assertTrue(controlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET).blockedReason.contains("JavaFX peer-list fallback"))
        assertEquals("Use for chat blocked", controlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET).displayLabel)
    }

    @Test
    fun shouldBlockOnlyFileTargetCommandForOnlinePeerWithoutDiscoveryEndpoint() {
        val state = ComposePeerListState(selectedPeerIndex = 1)
        val controlPlan = state.targetControlPlan

        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET).enabled)
        assertEquals(false, controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).enabled)
        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.VOICE_TARGET).enabled)
        assertTrue(controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).blockedReason.contains("file receiver endpoint"))
        assertTrue(controlPlan.enabledSummary.contains("Use for voice"))
    }

    @Test
    fun shouldExposeFileTargetControlPlanForOnlinePeerWithInferredFileReceiver() {
        val state = ComposePeerListState(
            peers = listOf(
                ComposePeerListItem(
                    nickname = "Android Phone",
                    online = true,
                    discovered = false,
                    listMeta = "chat peer",
                    selectedMeta = "Online via chat — file receiver inferred at 192.168.1.30:6051 for Android/client peers.",
                    filePort = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT + 1000,
                ),
            ),
            selectedPeerIndex = 0,
        )
        val controlPlan = state.targetControlPlan
        val quickActions = ComposeSelectedPeerQuickActionsState(peerListState = state, clientConnected = true)
        val fileTransfer = ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = state,
            selectedFilePath = "demo.bin",
            senderId = "Desktop",
            sessionPassword = "secret",
        )

        val attachCandidate = resolveAttachCandidatePeer(state.selectedPeer) { nickname ->
            assertEquals("Android Phone", nickname)
            DiscoveredPeer(
                "peer-android-phone",
                nickname,
                "192.168.1.30",
                NetworkConstants.DEFAULT_CHAT_PORT,
                NetworkConstants.DEFAULT_FILE_TRANSFER_PORT + 1000,
                java.time.Instant.now(),
            )
        }

        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).enabled)
        assertEquals(true, quickActions.attachEnabled)
        assertEquals(true, fileTransfer.canSendSelectedFile)
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT + 1000, attachCandidate?.filePort)
        assertTrue(fileTransfer.targetSummary.contains("inferred from its chat connection"), fileTransfer.targetSummary)
    }

    @Test
    fun shouldKeepAndroidPeerFileActionsEnabledButBlockRealtimeCalls() {
        val state = ComposePeerListState(
            peers = listOf(
                ComposePeerListItem(
                    nickname = "Android Phone",
                    online = true,
                    discovered = false,
                    listMeta = "Android • chat • file",
                    selectedMeta = "Online Android peer — file receiver advertised at 192.168.1.30:7001.",
                    filePort = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT + 1000,
                    voiceCapable = false,
                    videoCapable = false,
                    dataChannelCapable = false,
                ),
            ),
            selectedPeerIndex = 0,
        )
        val statusState = ComposeStatusConnectionState(clientConnected = true)
        val controlPlan = state.targetControlPlan
        val quickActions = ComposeSelectedPeerQuickActionsState(
            peerListState = state,
            clientConnected = true,
            hangUpReady = true,
        )
        val voice = ComposeMediaVoiceState(statusState = statusState, peerListState = state)
        val video = ComposeExperimentalVideoState(statusState = statusState, peerListState = state)

        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.CHAT_TARGET).enabled)
        assertEquals(true, controlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).enabled)
        assertEquals(false, controlPlan.command(ComposePeerTargetCommandKind.VOICE_TARGET).enabled)
        assertEquals(false, controlPlan.command(ComposePeerTargetCommandKind.VIDEO_TARGET).enabled)
        assertEquals(false, controlPlan.command(ComposePeerTargetCommandKind.DATA_TARGET).enabled)
        assertEquals(true, quickActions.attachEnabled)
        assertEquals(false, quickActions.voiceEnabled)
        assertEquals(false, quickActions.videoEnabled)
        assertEquals(false, quickActions.hangUpEnabled)
        assertEquals(false, voice.canStartVoice)
        assertEquals(false, video.canStartVideo)
        assertTrue(quickActions.readinessSummary.contains("does not advertise voice support"), quickActions.readinessSummary)
        assertTrue(video.readinessSummary.contains("does not advertise video support"), video.readinessSummary)
    }

    @Test
    fun shouldExposePeerTargetActionsForOnlineDiscoveredPeer() {
        val targetActions = ComposePeerTargetActions.from(
            ComposePeerListItem(
                nickname = "Alice",
                online = true,
                discovered = true,
                listMeta = "discovered",
                selectedMeta = "selected",
            ),
        )

        assertEquals("Peer target action readiness", targetActions.title)
        assertEquals(true, targetActions.chatReady)
        assertEquals(true, targetActions.fileReady)
        assertEquals(true, targetActions.voiceReady)
        assertEquals(true, targetActions.videoReady)
        assertEquals(true, targetActions.dataChannelReady)
        assertEquals(emptyList<String>(), targetActions.blockedReasons)
        assertTrue(targetActions.summary.contains("File transfer target ready"))
        assertTrue(targetActions.blockedSummary.contains("All selected-peer actions"))
    }

    @Test
    fun shouldExposeSelectedPeerQuickActionsForConnectedOnlineDiscoveredPeer() {
        val state = ComposePeerListState(
            peers = listOf(
                ComposePeerListItem(
                    nickname = "Alice",
                    online = true,
                    discovered = true,
                    listMeta = "192.168.1.20",
                    selectedMeta = "LAN endpoint 192.168.1.20",
                ),
            ),
            selectedPeerIndex = 0,
        )
        val quickActions = ComposeSelectedPeerQuickActionsState(
            peerListState = state,
            clientConnected = true,
            voiceRuntimeReady = true,
            videoRuntimeReady = true,
        )

        assertEquals("Alice", quickActions.title)
        assertTrue(quickActions.meta.contains("192.168.1.20"))
        assertEquals(true, quickActions.attachEnabled)
        assertEquals(true, quickActions.voiceEnabled)
        assertEquals(true, quickActions.videoEnabled)
        assertEquals(false, quickActions.hangUpEnabled)
        assertEquals(emptyList<String>(), quickActions.blockedReasons)
        assertTrue(quickActions.readinessLabel.contains("Attach"), quickActions.readinessLabel)
        assertTrue(quickActions.readinessSummary.contains("Quick actions are ready"), quickActions.readinessSummary)
    }

    @Test
    fun shouldBlockSelectedPeerQuickActionsWithoutPeerWithClearCopy() {
        val quickActions = ComposeSelectedPeerQuickActionsState(
            peerListState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1),
            clientConnected = true,
        )

        assertEquals("No peer selected", quickActions.title)
        assertEquals(false, quickActions.attachEnabled)
        assertEquals(false, quickActions.voiceEnabled)
        assertEquals(false, quickActions.videoEnabled)
        assertTrue(quickActions.readinessLabel.contains("blocked"), quickActions.readinessLabel)
        assertTrue(quickActions.blockedReasons.any { it.contains("Select an online peer") }, quickActions.blockedReasons.toString())
        assertTrue(quickActions.readinessSummary.contains("Select an online peer"), quickActions.readinessSummary)
    }

    @Test
    fun shouldBlockSelectedPeerQuickActionsForOfflinePeer() {
        val quickActions = ComposeSelectedPeerQuickActionsState(
            peerListState = ComposePeerListState(selectedPeerIndex = 2),
            clientConnected = true,
        )

        assertEquals("Offline NAS", quickActions.title)
        assertEquals(false, quickActions.attachEnabled)
        assertEquals(false, quickActions.voiceEnabled)
        assertEquals(false, quickActions.videoEnabled)
        assertTrue(quickActions.blockedReasons.any { it.contains("offline") }, quickActions.blockedReasons.toString())
        assertTrue(quickActions.readinessSummary.contains("offline"), quickActions.readinessSummary)
    }

    @Test
    fun shouldBlockSelectedPeerQuickActionsWhenFallbackUnavailable() {
        val quickActions = ComposeSelectedPeerQuickActionsState(
            peerListState = ComposePeerListState(selectedPeerIndex = 0, javaFxFallbackAvailable = false),
            clientConnected = true,
            hangUpReady = true,
            javaFxFallbackAvailable = false,
        )

        assertEquals("Astra Laptop", quickActions.title)
        assertEquals(false, quickActions.attachEnabled)
        assertEquals(false, quickActions.voiceEnabled)
        assertEquals(false, quickActions.videoEnabled)
        assertEquals(false, quickActions.hangUpEnabled)
        assertTrue(quickActions.blockedReasons.any { it.contains("JavaFX fallback") }, quickActions.blockedReasons.toString())
        assertTrue(quickActions.readinessSummary.contains("JavaFX fallback"), quickActions.readinessSummary)
    }

    @Test
    fun shouldUseSelectedPeerNicknameAsUnifiedComposeActionTransferVoiceAndVideoTarget() {
        val peerState = ComposePeerListState(
            peers = listOf(
                ComposePeerListItem("Astra Laptop", online = true, discovered = true, listMeta = "astra", selectedMeta = "astra"),
                ComposePeerListItem("Beta Phone", online = true, discovered = true, listMeta = "beta", selectedMeta = "beta"),
                ComposePeerListItem("Offline NAS", online = false, discovered = true, listMeta = "offline", selectedMeta = "offline"),
            ),
            selectedPeerIndex = -1,
            selectedPeerNickname = "Beta Phone",
        )
        val statusState = ComposeStatusConnectionState(clientConnected = true)
        val quickActions = ComposeSelectedPeerQuickActionsState(peerListState = peerState, clientConnected = true)
        val fileTransfer = ComposeFileTransferState(
            statusState = statusState,
            peerListState = peerState,
            selectedFilePath = "demo.bin",
            senderId = "Alice",
            sessionPassword = "secret",
        )
        val voice = ComposeMediaVoiceState(statusState = statusState, peerListState = peerState)
        val video = ComposeExperimentalVideoState(statusState = statusState, peerListState = peerState)

        assertEquals("Beta Phone", quickActions.selectedPeer?.nickname)
        assertEquals("Beta Phone", fileTransfer.selectedPeerName)
        assertEquals("Beta Phone", voice.selectedPeerName)
        assertEquals("Beta Phone", video.selectedPeerName)
        assertEquals(true, quickActions.attachEnabled)
        assertEquals(true, fileTransfer.canSendSelectedFile)
        assertEquals(true, voice.canStartVoice)
        assertEquals(true, video.canStartVideo)
        assertTrue(quickActions.actionSummary.contains("Beta Phone"), quickActions.actionSummary)
    }

    @Test
    fun shouldExposeAttachmentMenuErgonomicsForComposerCommands() {
        val blocked = ComposeAttachmentToolsState(peerSelected = false, fileTargetReady = false)
        val ready = ComposeAttachmentToolsState(peerSelected = true, fileTargetReady = true)

        assertEquals("Attach", blocked.title)
        assertEquals(
            listOf(
                ComposeAttachmentToolKind.SECURE_FILE,
                ComposeAttachmentToolKind.QUICK_SHARE,
                ComposeAttachmentToolKind.ENCRYPTED_TEXT_OR_FILE,
                ComposeAttachmentToolKind.STEGO_HIDE,
                ComposeAttachmentToolKind.STEGO_EXTRACT,
            ),
            blocked.menuItems.map { it.kind },
        )
        assertEquals(false, blocked.menuItems.first { it.kind == ComposeAttachmentToolKind.SECURE_FILE }.enabled)
        assertTrue(blocked.disabledStatusText.contains("Select an online person"), blocked.disabledStatusText)
        assertEquals(true, ready.menuItems.first { it.kind == ComposeAttachmentToolKind.SECURE_FILE }.enabled)
        assertEquals(true, ready.discoverableWithinTwoInteractions)
        assertEquals(true, ready.preservesKeyboardAccess)
        assertEquals(true, ready.restoresFocusAfterDismissal)
        assertTrue(ready.keepsAdvancedToolsContextual)
        assertEquals(248f, ready.layoutContract.minWidth.value)
        assertEquals(320f, ready.layoutContract.maxWidth.value)
        assertEquals(300f, ready.layoutContract.maxHeight.value)
        assertTrue(ready.layoutContract.boundsSummary.contains("kept inside the window"), ready.layoutContract.boundsSummary)
        assertTrue(ready.layoutContract.focusReturnTarget.contains("composer"), ready.layoutContract.focusReturnTarget)
    }

    @Test
    fun shouldKeepUnifiedComposeTargetsBlockedForNoPeerOrOfflinePeer() {
        val statusState = ComposeStatusConnectionState(clientConnected = true)
        val noPeerState = ComposePeerListState(peers = emptyList(), selectedPeerIndex = -1)
        val offlinePeerState = ComposePeerListState(
            peers = listOf(
                ComposePeerListItem("Offline NAS", online = false, discovered = true, listMeta = "offline", selectedMeta = "offline"),
                ComposePeerListItem("Astra Laptop", online = true, discovered = true, listMeta = "astra", selectedMeta = "astra"),
            ),
            selectedPeerIndex = -1,
            selectedPeerNickname = "Offline NAS",
        )

        val noPeerQuickActions = ComposeSelectedPeerQuickActionsState(peerListState = noPeerState, clientConnected = true)
        val noPeerFileTransfer = ComposeFileTransferState(statusState = statusState, peerListState = noPeerState)
        val noPeerVoice = ComposeMediaVoiceState(statusState = statusState, peerListState = noPeerState)
        val noPeerVideo = ComposeExperimentalVideoState(statusState = statusState, peerListState = noPeerState)
        val offlineQuickActions = ComposeSelectedPeerQuickActionsState(peerListState = offlinePeerState, clientConnected = true)
        val offlineFileTransfer = ComposeFileTransferState(statusState = statusState, peerListState = offlinePeerState)
        val offlineVoice = ComposeMediaVoiceState(statusState = statusState, peerListState = offlinePeerState)
        val offlineVideo = ComposeExperimentalVideoState(statusState = statusState, peerListState = offlinePeerState)

        assertEquals("No peer selected", noPeerFileTransfer.selectedPeerName)
        assertEquals(false, noPeerQuickActions.attachEnabled)
        assertEquals(false, noPeerFileTransfer.canSendSelectedFile)
        assertEquals(false, noPeerVoice.canStartVoice)
        assertEquals(false, noPeerVideo.canStartVideo)
        assertTrue(noPeerQuickActions.readinessSummary.contains("Select an online peer"), noPeerQuickActions.readinessSummary)
        assertTrue(noPeerFileTransfer.readinessSummary.contains("Select an online peer"), noPeerFileTransfer.readinessSummary)
        assertTrue(noPeerVoice.readinessSummary.contains("Select an online peer"), noPeerVoice.readinessSummary)
        assertTrue(noPeerVideo.readinessSummary.contains("Select an online peer"), noPeerVideo.readinessSummary)

        assertEquals("Offline NAS", offlineFileTransfer.selectedPeerName)
        assertEquals(false, offlineQuickActions.attachEnabled)
        assertEquals(false, offlineFileTransfer.canSendSelectedFile)
        assertEquals(false, offlineVoice.canStartVoice)
        assertEquals(false, offlineVideo.canStartVideo)
        assertTrue(offlineQuickActions.readinessSummary.contains("offline"), offlineQuickActions.readinessSummary)
        assertTrue(offlineFileTransfer.readinessSummary.contains("offline"), offlineFileTransfer.readinessSummary)
        assertTrue(offlineVoice.readinessSummary.contains("offline"), offlineVoice.readinessSummary)
        assertTrue(offlineVideo.readinessSummary.contains("offline"), offlineVideo.readinessSummary)
    }

    @Test
    fun shouldExposeQuickShareFileWorkflowReadiness() {
        val blocked = ComposeQuickShareState(selectedFilePath = "   ", textDraft = " ")
        val ready = ComposeQuickShareState(selectedFilePath = "C:/Users/Alice/demo.txt")

        assertEquals(false, blocked.canCreateFileShare)
        assertTrue(blocked.readinessSummary.contains("Choose a file or enter text"), blocked.readinessSummary)
        assertEquals(true, ready.hasSelectedFile)
        assertEquals(true, ready.canCreateFileShare)
        assertTrue(ready.readinessSummary.contains("Ready to create trusted-LAN"), ready.readinessSummary)
    }

    @Test
    fun shouldBlockOnlyFileTransferForOnlinePeerWithoutDiscoveryEndpoint() {
        val state = ComposePeerListState(selectedPeerIndex = 1)
        val targetActions = state.targetActions

        assertEquals("Beta Phone", state.selectedPeerTitle)
        assertEquals(true, targetActions.chatReady)
        assertEquals(false, targetActions.fileReady)
        assertEquals(true, targetActions.voiceReady)
        assertEquals(true, targetActions.videoReady)
        assertEquals(true, targetActions.dataChannelReady)
        assertTrue(targetActions.blockedReasons.any { it.contains("file receiver endpoint") })
        assertTrue(targetActions.fileLabel.contains("blocked"))
    }

    @Test
    fun shouldBlockAllPeerTargetActionsForOfflinePeer() {
        val state = ComposePeerListState(selectedPeerIndex = 2)
        val targetActions = state.targetActions

        assertEquals("Offline NAS", state.selectedPeerTitle)
        assertEquals(false, targetActions.chatReady)
        assertEquals(false, targetActions.fileReady)
        assertEquals(false, targetActions.voiceReady)
        assertEquals(false, targetActions.videoReady)
        assertEquals(false, targetActions.dataChannelReady)
        assertTrue(targetActions.blockedReasons.any { it.contains("offline") })
        assertTrue(targetActions.summary.contains("Chat target blocked"))
    }

    @Test
    fun shouldExposeNoPeerSelectedCopyForEmptyPeerListAdapterState() {
        val state = ComposePeerListState(peers = emptyList(), selectedPeerIndex = 4, javaFxFallbackAvailable = false)

        assertEquals(emptyList<ComposePeerListItem>(), state.visiblePeers)
        assertEquals("No peer selected", state.selectedPeerTitle)
        assertEquals("Peer not selected", state.peerStatus)
        assertTrue(state.selectedPeerMeta.contains("Choose an online chat peer"))
        assertTrue(state.actionSummary.contains("Select an online peer"))
        assertEquals("JavaFX peer list fallback unavailable", state.fallbackLabel)
        assertEquals(false, state.targetActions.chatReady)
        assertTrue(state.targetActions.blockedReasons.any { it.contains("Select an online peer") })
        assertEquals(emptyList<ComposePeerTargetCommand>(), state.targetControlPlan.enabledCommands)
        assertEquals(5, state.targetControlPlan.disabledCommands.size)
    }

    @Test
    fun shouldCoverCoreComposeUiStateForDiscoverySelectionChatFilesSettingsNetworkAndErrors() {
        val onlineDiscovered = ComposePeerListItem(
            nickname = "Desktop Peer",
            online = true,
            discovered = true,
            listMeta = "LAN endpoint 192.168.1.20:5050",
            selectedMeta = "Online discovered peer at 192.168.1.20; file receiver 5051.",
            filePort = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
        )
        val onlineChatOnly = ComposePeerListItem(
            nickname = "Chat Only",
            online = true,
            discovered = false,
            listMeta = "chat-only",
            selectedMeta = "Online through chat without file endpoint.",
            filePort = 0,
        )
        val offlineFilePeer = ComposePeerListItem(
            nickname = "Offline File Peer",
            online = false,
            discovered = true,
            listMeta = "offline discovered",
            selectedMeta = "Offline but previously discovered.",
            filePort = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
        )
        val peerState = ComposePeerListState(
            peers = listOf(offlineFilePeer, onlineChatOnly, onlineDiscovered),
            selectedPeerIndex = -1,
            selectedPeerNickname = "Desktop Peer",
        )
        val statusState = ComposeStatusConnectionState(
            nickname = "Alice",
            roomPasswordPlaceholder = "secret",
            manualHost = "192.168.1.20",
            clientConnected = true,
            localServerRunning = true,
        )
        val chatState = ComposeChatWorkspaceState(
            statusState = statusState,
            peerListState = peerState,
            draftMessage = "hello",
            messages = listOf(ComposeChatMessage("Bob", "hi"), ComposeChatMessage("connected", "Alice -> Bob", system = true)),
        )
        val fileState = ComposeFileTransferState(
            statusState = statusState,
            peerListState = peerState,
            selectedFilePath = "C:/Temp/demo.txt",
            senderId = "Alice",
            sessionPassword = "secret",
        )
        val diagnostics = ComposeDiagnosticsState(
            statusState = statusState,
            peerListState = peerState,
            chatDiagnostics = chatState.transcriptLines,
            fileTransferDiagnostics = listOf("Transfer completed: demo.txt."),
            quickShareDiagnostics = listOf("Quick share idle"),
            realtimeDiagnostics = listOf("RTC runtime ready"),
        )

        assertEquals(listOf("Chat Only", "Desktop Peer", "Offline File Peer"), peerState.visiblePeers.map { it.nickname })
        assertEquals("Desktop Peer", peerState.selectedPeerTitle)
        assertEquals(true, peerState.targetActions.chatReady)
        assertEquals(true, peerState.targetActions.fileReady)
        assertTrue(peerState.selectedPeerMeta.contains("file receiver"))
        assertTrue(peerState.peerListLifecyclePlan.readySteps.any { it.state == ComposePeerListLifecycleState.PEER_TARGETED })
        assertEquals(true, chatState.canSendMessage)
        assertEquals(listOf("Bob: hi", "[connected] Alice -> Bob"), chatState.transcriptLines)
        assertEquals(true, fileState.canSendSelectedFile)
        assertTrue(fileState.targetSummary.contains("discovered LAN file endpoint"), fileState.targetSummary)
        assertEquals("Using the current room password", fileState.passwordSummary)
        assertTrue(statusState.portSummary.contains("Room chat"), statusState.portSummary)
        assertTrue(statusState.discoveryStatus.contains("not started") || statusState.discoveryStatus.contains("Discovery"), statusState.discoveryStatus)
        assertEquals("Healthy", diagnostics.statusLabel)
        assertTrue(diagnostics.diagnosticChannelSummary.contains("chat=2"), diagnostics.diagnosticChannelSummary)

        val chatOnlySelected = ComposePeerListState(
            peers = listOf(onlineDiscovered, onlineChatOnly, offlineFilePeer),
            selectedPeerIndex = -1,
            selectedPeerNickname = "Chat Only",
        )
        val chatOnlyFileState = ComposeFileTransferState(
            statusState = statusState,
            peerListState = chatOnlySelected,
            selectedFilePath = "C:/Temp/demo.txt",
            senderId = "Alice",
            sessionPassword = "secret",
        )
        assertEquals(false, chatOnlySelected.targetActions.fileReady)
        assertEquals(false, chatOnlyFileState.canSendSelectedFile)
        assertTrue(chatOnlyFileState.blockedReasons.any { it.contains("file receiver endpoint") }, chatOnlyFileState.blockedReasons.toString())

        val offlineSelected = ComposePeerListState(
            peers = listOf(onlineDiscovered, onlineChatOnly, offlineFilePeer),
            selectedPeerIndex = -1,
            selectedPeerNickname = "Offline File Peer",
        )
        assertEquals(false, offlineSelected.targetActions.chatReady)
        assertEquals(false, offlineSelected.targetActions.fileReady)
        assertTrue(offlineSelected.targetActions.blockedReasons.any { it.contains("offline") }, offlineSelected.targetActions.blockedReasons.toString())
    }

    @Test
    fun shouldSortPeerListAdapterStateWithOnlinePeersFirstAndCaseInsensitiveNames() {
        val peers = listOf(
            ComposePeerListItem(
                "zeta",
                online = false,
                discovered = false,
                listMeta = "offline",
                selectedMeta = "offline"
            ),
            ComposePeerListItem("beta", online = true, discovered = false, listMeta = "chat", selectedMeta = "chat"),
            ComposePeerListItem(
                "Alpha",
                online = true,
                discovered = true,
                listMeta = "discovered",
                selectedMeta = "discovered"
            ),
        )
        val state = ComposePeerListState(peers = peers, selectedPeerIndex = 1)

        assertEquals(listOf("Alpha", "beta", "zeta"), state.visiblePeers.map { it.nickname })
        assertEquals("beta", state.selectedPeerTitle)
    }

    @Test
    fun shouldExposeDefaultDiagnosticsStateWithoutRuntimeSubscriptions() {
        val state = ComposeShellMetadata.DEFAULT_DIAGNOSTICS_STATE

        assertEquals("Runtime diagnostics", state.title)
        assertEquals("JavaFX fallback is available", state.fallbackStatus)
        assertTrue(state.statusAdapterSummary.contains("Ready to open or join a room."))
        assertTrue(state.connectionActionSummary.contains("Open room ready"))
        assertEquals("Selected peer: Astra Laptop · Peer Astra Laptop", state.selectedPeerSummary)
        assertEquals("Visible peers: 3", state.visiblePeerSummary)
        assertTrue(state.diagnosticChannelSummary.contains("chat=0"))
        assertEquals("Healthy", state.statusLabel)
        assertEquals(4, state.channelCards.size)
        assertEquals(0, state.activeChannelCount)
        assertEquals(0, state.totalDiagnosticMessages)
        assertTrue(state.runtimeOverview.contains("No runtime events yet"))
        assertTrue(state.runtimeOverviewSummary.contains("No runtime events yet"))
        assertEquals("Ready for troubleshooting", state.recoveryTitle)
        assertEquals("Calm", state.recoveryBadge)
        assertTrue(state.recoverySummary.contains("Diagnostics only when something needs investigation"))
        assertTrue(state.technicalDetailsSummary.contains("collapsed"))
        assertTrue(state.channelCards.first().emptyState.contains("Chat events"))
        assertEquals(listOf(state.noDiagnosticsSituation, state.noDiagnosticsExplanation, state.noDiagnosticsNextAction), state.noDiagnosticsStructuredCopy)
        assertEquals(ComposeEmptyStateVisualWeight.INLINE, state.noDiagnosticsVisualWeight)
        assertEquals(emptyList<String>(), state.warningMessages)
        assertTrue(state.warningSummary.contains("No runtime alerts"))
        assertEquals(10, state.summaryLines.size)
    }

    @Test
    fun shouldReportDiagnosticsWarningsForBlockedAdapters() {
        val diagnostics = ComposeDiagnosticsState(
            statusState = ComposeStatusConnectionState(
                nickname = " ",
                serverChatPortText = "0",
                clientChatPortText = "abc",
            ),
            peerListState = ComposePeerListState(peers = emptyList()),
            javaFxFallbackAvailable = false,
        )

        assertEquals("JavaFX fallback is unavailable", diagnostics.fallbackStatus)
        assertTrue(diagnostics.warningMessages.any { it.contains("Fallback unavailable") })
        assertTrue(diagnostics.warningMessages.any { it.contains("Profile name required") })
        assertTrue(diagnostics.warningMessages.any { it.contains("Room ports need attention") })
        assertTrue(diagnostics.warningMessages.any { it.contains("Advanced connection incomplete") })
        assertTrue(diagnostics.warningMessages.any { it.contains("No peer selected") })
        assertEquals("Needs attention", diagnostics.statusLabel)
        assertEquals(true, diagnostics.hasErrors)
        assertTrue(diagnostics.warningSummary.contains("Restore the JavaFX fallback"))
    }

    @Test
    fun shouldClearDiagnosticsWarningsWhenAllRuntimeChannelsHaveEvidence() {
        val diagnostics = ComposeDiagnosticsState(
            statusState = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20"),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            chatDiagnostics = listOf("Alice: hello"),
            fileTransferDiagnostics = listOf("Transfer completed: demo.txt."),
            quickShareDiagnostics = listOf("Quick-share created."),
            realtimeDiagnostics = listOf("AUDIO session CONNECTED"),
        )

        assertEquals(emptyList<String>(), diagnostics.warningMessages)
        assertTrue(diagnostics.warningSummary.contains("No runtime alerts"))
        assertTrue(diagnostics.diagnosticChannelSummary.contains("realtime=1"))
        assertEquals("Healthy", diagnostics.statusLabel)
        assertEquals(4, diagnostics.activeChannelCount)
        assertEquals(4, diagnostics.totalDiagnosticMessages)
        assertTrue(diagnostics.recentMessages.any { it.contains("Realtime media") })
    }

    @Test
    fun shouldBuildFriendlyDiagnosticChannelCardsWithoutMockCopy() {
        val diagnostics = ComposeDiagnosticsState(
            statusState = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20"),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            chatDiagnostics = listOf("[connected] Alice -> 192.168.1.20", "Bob: hello"),
            fileTransferDiagnostics = listOf("Transfer started: demo.txt.", "Transfer completed: demo.txt."),
            quickShareDiagnostics = emptyList(),
            realtimeDiagnostics = listOf("Media devices refreshed: 1 microphones, 1 speakers, 1 cameras."),
        )

        assertEquals(3, diagnostics.activeChannelCount)
        assertEquals(5, diagnostics.totalDiagnosticMessages)
        assertEquals("2 events", diagnostics.channelCards.first { it.kind == ComposeDiagnosticChannelKind.CHAT }.stateLabel)
        assertTrue(diagnostics.channelCards.first { it.kind == ComposeDiagnosticChannelKind.CHAT }.densitySummary.contains("Open Technical details"))
        assertEquals("Waiting", diagnostics.channelCards.first { it.kind == ComposeDiagnosticChannelKind.QUICK_SHARE }.stateLabel)
        assertTrue(diagnostics.channelCards.first { it.kind == ComposeDiagnosticChannelKind.QUICK_SHARE }.latestMessage.contains("Quick-share events"))
        assertTrue(diagnostics.channelCards.first { it.kind == ComposeDiagnosticChannelKind.CHAT }.latestMessageSummary.length <= 120)
        assertFalse(diagnostics.summaryLines.any { it.contains("smoke checks before promotion") })
    }

    @Test
    fun shouldExposeRegressionReadinessGatesForPendingRuntimeEvidence() {
        val state = ComposeShellMetadata.DEFAULT_REGRESSION_STATE

        assertEquals("Compose regression readiness", state.title)
        assertEquals(10, state.totalCount)
        assertEquals(8, state.runtimeEvidenceRequirements.size)
        assertTrue(state.readyCount >= 2)
        assertTrue(state.blockedGates.any { it.kind == ComposeRegressionGateKind.CHAT_INTEROP })
        assertTrue(state.blockedGates.any { it.kind == ComposeRegressionGateKind.FILE_TRANSFER })
        assertTrue(state.missingRuntimeEvidence.any { it.kind == ComposeRuntimeEvidenceKind.CHAT_INTEROP })
        assertTrue(state.missingRuntimeEvidence.any { it.kind == ComposeRuntimeEvidenceKind.RESIZE_SCREENSHOTS })
        assertTrue(state.runtimeEvidenceSummary.contains("missing"))
        assertTrue(state.runtimeChecklistSummary.contains("desktop-to-desktop"))
        assertEquals(8, state.runtimeRegressionChecklist.size)
        assertTrue(state.runtimeRegressionChecklistCopy.contains("1. Chat interop"))
        assertTrue(state.runtimeRegressionChecklistCopy.contains("Runtime resize screenshots"))
        assertTrue(state.acceptedRuntimeFlowSummary.contains("No runtime evidence"))
        assertTrue(state.runtimeEvidenceRecordSummary.contains("No runtime evidence records"))
        assertEquals("", state.runtimeEvidenceCopyText)
        assertTrue(state.pendingRuntimeFlowSummary.contains("Chat interop"))
        assertTrue(state.nextActionSummary.contains("chat smoke"))
        assertEquals(false, state.allRuntimeValidated)
    }

    @Test
    fun shouldExposeReadyRegressionStateAfterAllRuntimeEvidenceIsRecorded() {
        val peerState = ComposePeerListState(selectedPeerIndex = 0)
        val state = ComposeRegressionReadinessState(
            statusState = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20"),
            peerListState = peerState,
            chatState = ComposeChatWorkspaceState(ComposeStatusConnectionState(clientConnected = true), peerState),
            fileTransferState = ComposeFileTransferState(
                statusState = ComposeStatusConnectionState(clientConnected = true),
                peerListState = peerState,
                selectedFilePath = "demo.txt",
                sessionPassword = "secret",
            ),
            quickShareState = ComposeQuickShareState(selectedFilePath = "demo.txt", textDraft = "hello"),
            steganographyState = ComposeSteganographyState(coverPathText = "cover.bmp", inputPathText = "stego.bmp", outputPathText = "out.bmp", messageDraft = "secret"),
            mediaVoiceState = ComposeMediaVoiceState(ComposeStatusConnectionState(clientConnected = true), peerState),
            experimentalVideoState = ComposeExperimentalVideoState(ComposeStatusConnectionState(clientConnected = true), peerState),
            diagnosticsState = ComposeDiagnosticsState(
                statusState = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20"),
                peerListState = peerState,
                chatDiagnostics = listOf("chat ok"),
                fileTransferDiagnostics = listOf("file ok"),
                quickShareDiagnostics = listOf("share ok"),
                realtimeDiagnostics = listOf("rtc ok"),
            ),
            chatRuntimeValidated = true,
            fileTransferRuntimeValidated = true,
            quickShareRuntimeValidated = true,
            steganographyRuntimeValidated = true,
            voiceRuntimeValidated = true,
            videoRuntimeValidated = true,
            resizeScreenshotMatrixValidated = true,
            fullRuntimeRegressionValidated = true,
            runtimeEvidenceRecords = listOf(
                ComposeRuntimeEvidenceRecord(
                    ComposeRuntimeEvidenceKind.CHAT_INTEROP,
                    ComposeRuntimeEvidenceChecklistStatus.ACCEPTED,
                    "desktop and Android chat smoke passed",
                    Instant.parse("2026-05-26T20:00:00Z"),
                ),
            ),
        )

        assertEquals(true, state.allRuntimeValidated)
        assertEquals(emptyList<ComposeRegressionGate>(), state.blockedGates)
        assertEquals(8, state.recordedRuntimeEvidence.size)
        assertEquals(emptyList<ComposeRuntimeEvidenceRequirement>(), state.missingRuntimeEvidence)
        assertTrue(state.runtimeEvidenceSummary.contains("8 of 8"))
        assertTrue(state.runtimeChecklistSummary.contains("packaging validation"))
        assertTrue(state.runtimeRegressionChecklist.all { it.recorded })
        assertTrue(state.acceptedRuntimeFlowSummary.contains("Full Compose regression"))
        assertTrue(state.acceptedRuntimeFlowSummary.contains("Runtime resize screenshots"))
        assertTrue(state.runtimeEvidenceRecordSummary.contains("1 accepted"))
        assertTrue(state.runtimeEvidenceCopyText.contains("chat-interop=accepted"))
        assertTrue(state.pendingRuntimeFlowSummary.contains("No runtime evidence"))
        assertTrue(state.summary.contains("10 of 10"))
        assertTrue(state.nextActionSummary.contains("packaging validation"))
    }

    @Test
    fun shouldExposePackagingReadinessWithoutChangingJavaFxLauncher() {
        val state = ComposeShellMetadata.DEFAULT_PACKAGING_STATE

        assertEquals("Compose packaging readiness", state.title)
        assertEquals("com.shterneregen.securelan.desktop.Main", state.applicationMainClass)
        assertTrue(state.launcherStatus.contains("JavaFX launcher"))
        assertEquals(false, state.canPromoteComposeLauncher)
        assertEquals(false, state.releaseValidationReady)
        assertEquals(ComposeLauncherDecisionKind.CONTINUE_VALIDATION, state.launcherDecision.recommendedOption.kind)
        assertTrue(state.launcherDecision.blockerSummary.contains("Release validation"))
        assertTrue(state.blockedGates.any { it.kind == ComposePackagingGateKind.DESKTOP_BUILD })
        assertTrue(state.packagingTasksSummary.contains(":apps:desktop-client:buildPortable"))
        assertTrue(state.packagingTasksSummary.contains(":apps:desktop-client:buildComposePortable"))
        assertEquals(5, state.artifactRequirements.size)
        assertTrue(state.artifactRequirements.any { it.kind == ComposePackagingArtifactKind.PORTABLE_ZIP && !it.validated })
        assertTrue(state.artifactRequirements.any { it.kind == ComposePackagingArtifactKind.COMPOSE_PORTABLE_ZIP && !it.validated })
        assertTrue(state.artifactSummary.contains("1 of 5"))
        assertTrue(state.pendingArtifactSummary.contains(":apps:desktop-client:buildPortable"))
        assertTrue(state.pendingArtifactSummary.contains(":apps:desktop-client:buildComposePortable"))
        assertTrue(state.rollbackPlanSummary.contains("Rollback path preserved"))
        assertTrue(state.promotionChecklistSummary.contains("desktop build=false"))
        assertTrue(state.packagingEvidenceSummary.contains("No packaging evidence records"))
        assertTrue(state.validationReport.copyText.contains("Compose packaging validation report"))
        assertTrue(state.promotionSummary.contains("Keep JavaFX launcher"))
    }

    @Test
    fun shouldExposeCopyablePackagingEvidenceRecords() {
        val state = ComposePackagingReadinessState(
            desktopBuildPassed = true,
            evidenceRecords = listOf(
                ComposePackagingEvidenceRecord(
                    ComposePackagingEvidenceKind.DESKTOP_BUILD,
                    true,
                    "desktop build passed",
                    Instant.parse("2026-05-26T20:10:00Z"),
                ),
                ComposePackagingEvidenceRecord(
                    ComposePackagingEvidenceKind.WINDOWS_EXE,
                    false,
                    "WiX host unavailable",
                    Instant.parse("2026-05-26T20:11:00Z"),
                ),
            ),
        )

        assertEquals(1, state.acceptedEvidenceRecords.size)
        assertEquals(1, state.pendingEvidenceRecords.size)
        assertTrue(state.packagingEvidenceSummary.contains("1 packaging evidence records validated"))
        assertTrue(state.packagingEvidenceCopyText.contains("desktop-build=validated"))
        assertTrue(state.validationReport.copyText.contains("WiX host unavailable"))
    }

    @Test
    fun shouldKeepPackagingPromotionBlockedUntilExplicitApproval() {
        val validatedButNotApproved = ComposePackagingReadinessState(
            desktopTestsPassed = true,
            desktopBuildPassed = true,
            composeRuntimeSmokePassed = true,
            portableZipValidated = true,
            composePortableZipValidated = true,
            windowsExeValidated = true,
            composePromotionApproved = false,
            fullRuntimeRegressionValidated = true,
        )
        val approved = validatedButNotApproved.copy(composePromotionApproved = true)

        assertEquals(false, validatedButNotApproved.canPromoteComposeLauncher)
        assertEquals(true, validatedButNotApproved.releaseValidationReady)
        assertEquals(ComposeLauncherDecisionKind.KEEP_JAVAFX_FALLBACK, validatedButNotApproved.launcherDecision.recommendedOption.kind)
        assertTrue(validatedButNotApproved.launcherDecision.blockerSummary.contains("approval"))
        assertTrue(validatedButNotApproved.blockedGates.any { it.kind == ComposePackagingGateKind.LAUNCHER_DECISION })
        assertTrue(validatedButNotApproved.promotionDecisionSteps.any {
            it.kind == ComposePromotionDecisionStepKind.REQUIRE_APPROVAL && !it.satisfied
        })
        assertTrue(validatedButNotApproved.promotionDecisionSummary.contains("explicit approval"))
        assertEquals(true, approved.canPromoteComposeLauncher)
        assertEquals(ComposeLauncherDecisionKind.PROMOTE_COMPOSE_AFTER_ACCEPTANCE, approved.launcherDecision.recommendedOption.kind)
        assertEquals(emptyList<ComposePackagingGate>(), approved.blockedGates)
        assertTrue(approved.promotionDecisionSteps.all { it.satisfied })
    }

    @Test
    fun shouldRequireFullRuntimeRegressionBeforeReleaseValidation() {
        val missingRuntimeRegression = ComposePackagingReadinessState(
            desktopTestsPassed = true,
            desktopBuildPassed = true,
            composeRuntimeSmokePassed = true,
            portableZipValidated = true,
            composePortableZipValidated = true,
            windowsExeValidated = true,
            composePromotionApproved = true,
            fullRuntimeRegressionValidated = false,
        )
        val complete = missingRuntimeRegression.copy(fullRuntimeRegressionValidated = true)

        assertEquals(false, missingRuntimeRegression.releaseValidationReady)
        assertEquals(false, missingRuntimeRegression.canPromoteComposeLauncher)
        assertTrue(missingRuntimeRegression.promotionChecklistSummary.contains("runtime regression=false"))
        assertTrue(missingRuntimeRegression.promotionDecisionSteps.any {
            it.kind == ComposePromotionDecisionStepKind.COMPLETE_RUNTIME_REGRESSION && !it.satisfied
        })
        assertTrue(missingRuntimeRegression.promotionDecisionSummary.contains("Complete runtime regression"))
        assertEquals(true, complete.releaseValidationReady)
        assertEquals(true, complete.canPromoteComposeLauncher)
    }

    @Test
    fun shouldKeepLauncherPromotionBlockedWhenFallbackIsUnavailable() {
        val state = ComposePackagingReadinessState(
            desktopTestsPassed = true,
            desktopBuildPassed = true,
            composeRuntimeSmokePassed = true,
            portableZipValidated = true,
            composePortableZipValidated = true,
            windowsExeValidated = true,
            composePromotionApproved = true,
            javaFxFallbackAvailable = false,
        )

        assertEquals(false, state.releaseValidationReady)
        assertEquals(false, state.canPromoteComposeLauncher)
        assertEquals(ComposeLauncherDecisionKind.CONTINUE_VALIDATION, state.launcherDecision.recommendedOption.kind)
        assertTrue(state.launcherDecision.blockerSummary.contains("fallback"))
    }

    @Test
    fun shouldExposeSteganographyStateReadinessAndCapacityCopy() {
        val state = ComposeSteganographyState(
            coverPathText = "cover.bmp",
            inputPathText = "cover-stego.bmp",
            outputPathText = "out.bmp",
            messageDraft = "secret",
            passwordDraft = "pw",
            encryptPayload = true,
            encryptedExtract = true,
            capacity = BmpCapacity(8, 8, 24, 192, 11, 22),
            extractedMessage = "secret",
        )

        assertEquals("Steganography", state.title)
        assertEquals(true, state.canInspectCover)
        assertEquals(true, state.canHideMessage)
        assertEquals(true, state.canExtractMessage)
        assertTrue(state.capacityText.contains("22 bytes"))
        assertTrue(state.extractedSummary.contains("6 characters"))
        assertEquals(emptyList<String>(), state.blockedReasons)
    }

    @Test
    fun shouldBlockSteganographyStateWhenInputsOrPasswordAreMissing() {
        val state = ComposeSteganographyState(encryptPayload = true, encryptedExtract = true, javaFxFallbackAvailable = false)

        assertEquals(false, state.canInspectCover)
        assertEquals(false, state.canHideMessage)
        assertEquals(false, state.canExtractMessage)
        assertTrue(state.blockedReasons.any { it.contains("cover") })
        assertTrue(state.blockedReasons.any { it.contains("password") })
        assertTrue(state.blockedReasons.any { it.contains("fallback") })
    }

    @Test
    fun shouldExposeMediaVoiceReadinessAndLabels() {
        val peerState = ComposePeerListState(selectedPeerIndex = 0)
        val state = ComposeMediaVoiceState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = peerState,
            microphones = listOf(MediaDeviceChoice.systemDefault("System default microphone")),
            outputDevices = listOf(MediaDeviceChoice.systemDefault("System default speaker"), MediaDeviceChoice("speaker-1", "USB Speakers", false, true)),
            runtimeStatus = RtcRuntimeStatus("webrtc-java", true, "ready"),
            currentSession = RtcSessionSnapshot("rtc-1", "Alice", "Astra Laptop", RtcSessionMode.AUDIO, "securelan-data", RtcSessionState.CONNECTED, "Connected"),
            localAudioLevel = 0.42,
            remoteAudioLevel = 0.12,
        )

        assertEquals("Media devices and voice", state.title)
        assertEquals("webrtc-java ready", state.runtimeLabel)
        assertEquals("In call with Astra Laptop", state.voiceStatusText)
        assertEquals(true, state.canStartVoice)
        assertEquals(true, state.canTestSpeaker)
        assertEquals(true, state.canHangUp)
        assertEquals(42, state.localAudioPercent)
        assertTrue(state.permissionStatusLabel.contains("ready"))
        assertTrue(state.outputEmptyState.contains("speaker option"))
        assertTrue(state.localAudioLabel.contains("42%"))
        assertEquals(emptyList<String>(), state.blockedReasons)
    }

    @Test
    fun shouldBlockMediaVoiceWithoutChatOrPeer() {
        val state = ComposeMediaVoiceState(
            statusState = ComposeStatusConnectionState(clientConnected = false),
            peerListState = ComposePeerListState(peers = emptyList()),
        )

        assertEquals(false, state.canStartVoice)
        assertEquals(false, state.canHangUp)
        assertTrue(state.blockedReasons.any { it.contains("Connect to chat") })
        assertTrue(state.blockedReasons.any { it.contains("Select an online peer") })
    }

    @Test
    fun shouldExposeExperimentalVideoReadinessAndPreviewCopy() {
        val state = ComposeExperimentalVideoState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(selectedPeerIndex = 0),
            cameras = listOf(MediaDeviceChoice.systemDefault("System default camera")),
            runtimeStatus = RtcRuntimeStatus("webrtc-java", true, "ready"),
            currentSession = RtcSessionSnapshot("rtc-2", "Alice", "Astra Laptop", RtcSessionMode.AUDIO_VIDEO, "securelan-data", RtcSessionState.NEGOTIATING, "Preparing"),
            previewRunning = true,
        )

        assertEquals("Experimental camera and video", state.title)
        assertEquals("Video call with Astra Laptop", state.stageTitle)
        assertEquals("Negotiating", state.stageBadge)
        assertEquals("Audio + camera", state.mediaLabel)
        assertEquals("Camera preview starting…", state.previewStatus)
        assertEquals("Preview is starting", state.previewStateLabel)
        assertEquals("Preview unavailable", state.startPreviewLabel)
        assertEquals("Stop camera preview", state.stopPreviewLabel)
        assertTrue(state.previewActionHint.contains("Waiting for the first camera frame"))
        assertTrue(state.permissionStatusLabel.contains("ready"))
        assertTrue(state.cameraEmptyState.contains("No cameras found"))
        assertEquals(true, state.canStartVideo)
        assertEquals(true, state.canStopPreview)
        assertEquals(true, state.canHangUp)
        assertEquals(emptyList<String>(), state.blockedReasons)
    }

    @Test
    fun shouldBlockExperimentalVideoWithoutConnectedPeer() {
        val state = ComposeExperimentalVideoState(
            statusState = ComposeStatusConnectionState(clientConnected = false),
            peerListState = ComposePeerListState(peers = emptyList()),
            javaFxFallbackAvailable = false,
        )

        assertEquals(false, state.canStartVideo)
        assertEquals(false, state.canRefreshCameras)
        assertTrue(state.blockedReasons.any { it.contains("Connect to chat") })
        assertTrue(state.blockedReasons.any { it.contains("fallback") })
    }

    @Test
    fun shouldExposeDefaultValidAdapterEventRoutingContract() {
        val state = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20")
        val routing = state.adapterEventRouting

        assertEquals("Host runtime adapter event contract", routing.title)
        assertTrue(routing.subtitle.contains("Side-effect-free"), "subtitle: ${routing.subtitle}")
        assertEquals(10, routing.contracts.size)
        assertEquals(10, routing.totalCount)
        assertTrue(routing.readyCount > 0)
        assertTrue(routing.fallbackAvailable)

        val hostStarted = findContract(routing, ComposeAdapterEventKind.HOST_STARTED)
        assertEquals(true, hostStarted.ready)
        assertEquals("Host started", hostStarted.label)
        assertEquals(emptyList<ComposeAdapterEventKind>(), hostStarted.cleanupAfter)
        assertEquals("standalone", hostStarted.eventOrderNote)

        val connected = findContract(routing, ComposeAdapterEventKind.CONNECTED)
        assertEquals(false, connected.ready)
        assertEquals(listOf(ComposeAdapterEventKind.CONNECT_STARTED), connected.cleanupAfter)

        val runtimeError = findContract(routing, ComposeAdapterEventKind.RUNTIME_ERROR)
        assertEquals(false, runtimeError.ready)
        assertEquals(true, runtimeError.guarded)

        assertTrue(routing.eventOrderLabel.startsWith("host-started"))
        assertTrue(routing.eventOrderLabel.endsWith("cleanup-completed"))
        assertTrue(routing.cleanupOrderSummary.contains("not yet applicable"))
    }

    @Test
    fun shouldExposeHostedStateAdapterEventContract() {
        val state = ComposeStatusConnectionState(localServerRunning = true, discoverable = false)
        val routing = state.adapterEventRouting

        assertEquals(true, routing.fallbackAvailable)
        assertTrue(routing.readyCount >= 5)

        val hostStarted = findContract(routing, ComposeAdapterEventKind.HOST_STARTED)
        assertEquals(false, hostStarted.ready)

        val hostStopped = findContract(routing, ComposeAdapterEventKind.HOST_STOPPED)
        assertEquals(true, hostStopped.ready)

        val discoveryVisibility = findContract(routing, ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED)
        assertEquals(true, discoveryVisibility.ready)

        val cleanupStarted = findContract(routing, ComposeAdapterEventKind.CLEANUP_STARTED)
        assertEquals(true, cleanupStarted.ready)

        val cleanupCompleted = findContract(routing, ComposeAdapterEventKind.CLEANUP_COMPLETED)
        assertEquals(true, cleanupCompleted.ready)
        assertEquals(listOf(ComposeAdapterEventKind.CLEANUP_STARTED), cleanupCompleted.cleanupAfter)
    }

    @Test
    fun shouldExposeConnectedStateAdapterEventContract() {
        val state = ComposeStatusConnectionState(clientConnected = true)
        val routing = state.adapterEventRouting

        assertEquals(true, routing.fallbackAvailable)
        assertTrue(routing.readyCount >= 3)

        val connected = findContract(routing, ComposeAdapterEventKind.CONNECTED)
        assertEquals(true, connected.ready)

        val disconnected = findContract(routing, ComposeAdapterEventKind.DISCONNECTED)
        assertEquals(true, disconnected.ready)
        assertEquals(listOf(ComposeAdapterEventKind.CONNECTED), disconnected.cleanupAfter)

        val hostStarted = findContract(routing, ComposeAdapterEventKind.HOST_STARTED)
        // hostStarted may be ready or blocked depending on lifecycle gating;
        // verify it has a contract with proper label and prerequisites.
        assertEquals("Host started", hostStarted.label)
        assertTrue(hostStarted.prerequisites.isNotEmpty())

        val cleanupStarted = findContract(routing, ComposeAdapterEventKind.CLEANUP_STARTED)
        assertEquals(true, cleanupStarted.ready)

        val cleanupCompleted = findContract(routing, ComposeAdapterEventKind.CLEANUP_COMPLETED)
        assertEquals(true, cleanupCompleted.ready)
    }

    @Test
    fun shouldBlockAllAdapterEventsWhenFallbackUnavailable() {
        val state = ComposeStatusConnectionState(javaFxFallbackAvailable = false)
        val routing = state.adapterEventRouting

        assertEquals(false, routing.fallbackAvailable)
        assertEquals(0, routing.readyCount)
        assertEquals(10, routing.blockedCount)
        assertTrue(routing.readinessSummary.contains("All 10"))
        assertTrue(routing.fallbackStatus.contains("unavailable"))

        routing.contracts.forEach { contract ->
            assertEquals(false, contract.ready, "Expected ${contract.kind} to be blocked when fallback unavailable")
            assertTrue(
                contract.blockedReason.contains("JavaFX fallback"),
                "Expected ${contract.kind} blockedReason to mention fallback, got: ${contract.blockedReason}"
            )
        }
        assertTrue(routing.cleanupOrderSummary.contains("not yet applicable"))
    }

    @Test
    fun shouldBlockHostAndConnectEventsForInvalidInputs() {
        val state = ComposeStatusConnectionState(
            nickname = " ",
            manualHost = " ",
            serverChatPortText = "0",
            serverFilePortText = "70000",
            clientChatPortText = "abc",
            clientFilePortText = "65536",
            javaFxFallbackAvailable = true,
        )
        val routing = state.adapterEventRouting

        assertEquals(0, routing.readyCount)
        assertEquals(10, routing.blockedCount)

        val hostStarted = findContract(routing, ComposeAdapterEventKind.HOST_STARTED)
        assertEquals(false, hostStarted.ready)
        assertTrue(
            hostStarted.blockedReason.contains("blank") || hostStarted.blockedReason.contains("invalid") || hostStarted.blockedReason.contains(
                "Nickname"
            ) || hostStarted.blockedReason.contains("Room ports"),
            "hostStarted blockedReason: ${hostStarted.blockedReason}"
        )

        val connectStarted = findContract(routing, ComposeAdapterEventKind.CONNECT_STARTED)
        assertEquals(false, connectStarted.ready)
        assertTrue(
            connectStarted.blockedReason.contains("blank") || connectStarted.blockedReason.contains("invalid") || connectStarted.blockedReason.contains(
                "Nickname"
            ) || connectStarted.blockedReason.contains("Manual host"),
            "connectStarted blockedReason: ${connectStarted.blockedReason}"
        )

        val runtimeError = findContract(routing, ComposeAdapterEventKind.RUNTIME_ERROR)
        assertEquals(false, runtimeError.ready)
        assertEquals(true, runtimeError.guarded)
    }

    private fun findContract(
        routing: ComposeAdapterEventRouting,
        kind: ComposeAdapterEventKind
    ): ComposeAdapterEventContract =
        routing.contracts.first { it.kind == kind }

    @Test
    fun shouldExposeDeterministicAdapterEventAndCleanupOrderingSummary() {
        val hosted = ComposeStatusConnectionState(localServerRunning = true).adapterEventRouting
        val connected = ComposeStatusConnectionState(clientConnected = true).adapterEventRouting
        val idle = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20").adapterEventRouting

        assertTrue(hosted.eventOrderLabel.startsWith("host-started"))
        assertTrue(hosted.eventOrderLabel.endsWith("cleanup-completed"))
        assertEquals(hosted.eventOrderLabel, connected.eventOrderLabel)
        assertEquals(hosted.eventOrderLabel, idle.eventOrderLabel)

        val hostedCleanup = hosted.contracts.first { it.kind == ComposeAdapterEventKind.CLEANUP_COMPLETED }
        assertEquals(listOf(ComposeAdapterEventKind.CLEANUP_STARTED), hostedCleanup.cleanupAfter)

        val hostedCleanupStarted = hosted.contracts.first { it.kind == ComposeAdapterEventKind.CLEANUP_STARTED }
        assertEquals(true, hostedCleanupStarted.ready)
        assertEquals(emptyList<ComposeAdapterEventKind>(), hostedCleanupStarted.cleanupAfter)
        assertEquals("standalone", hostedCleanupStarted.eventOrderNote)

        val connectedDisconnected = connected.contracts.first { it.kind == ComposeAdapterEventKind.DISCONNECTED }
        assertEquals(listOf(ComposeAdapterEventKind.CONNECTED), connectedDisconnected.cleanupAfter)
        assertTrue(connectedDisconnected.eventOrderNote.startsWith("after connected"))
    }

    @Test
    fun shouldLinkAdapterEventRoutingToLifecycleAndTransitionPlans() {
        val state = ComposeStatusConnectionState(nickname = "Alice", manualHost = "192.168.1.20")
        val routing = state.adapterEventRouting

        assertEquals(state.lifecyclePlan.sideEffectContractSummary, routing.summary)
        assertEquals(state.lifecyclePlan.fallbackAvailable, routing.fallbackAvailable)

        val hostStarted = routing.contracts.first { it.kind == ComposeAdapterEventKind.HOST_STARTED }
        assertEquals(state.lifecyclePlan.step(ComposeConnectionLifecycleState.HOSTING_READY).ready, hostStarted.ready)
        assertTrue(hostStarted.prerequisites.contains("valid nickname"))
        assertTrue(hostStarted.prerequisites.contains("valid room ports"))

        val connectStarted = routing.contracts.first { it.kind == ComposeAdapterEventKind.CONNECT_STARTED }
        assertEquals(
            state.lifecyclePlan.step(ComposeConnectionLifecycleState.CONNECTING_READY).ready,
            connectStarted.ready
        )
        assertTrue(connectStarted.description.contains("manual chat-client connection"))
    }

    @Test
    fun shouldExposeDefaultPeerListLifecycleContract() {
        val state = ComposePeerListState()
        val lifecycle = state.peerListLifecyclePlan

        assertEquals("Live peer-list binding contract", lifecycle.title)
        assertEquals(ComposePeerListLifecycleState.PEER_TARGETED, lifecycle.currentState)
        assertEquals(6, lifecycle.steps.size)
        assertTrue(lifecycle.fallbackAvailable)
        assertTrue(lifecycle.readinessSummary.contains("Peer targeted"))
        assertTrue(lifecycle.fallbackStatus.contains("available"))
    }

    @Test
    fun shouldBlockPeerListLifecycleWhenFallbackUnavailable() {
        val state = ComposePeerListState(javaFxFallbackAvailable = false)
        val lifecycle = state.peerListLifecyclePlan

        assertEquals(ComposePeerListLifecycleState.BLOCKED_ERROR, lifecycle.currentState)
        assertEquals(false, lifecycle.fallbackAvailable)
        assertEquals(1, lifecycle.readySteps.size)
        assertEquals("Blocked/error", lifecycle.readySteps.first().label)
        assertTrue(lifecycle.blockedReasons.any { it.contains("JavaFX peer-list fallback") })
    }

    @Test
    fun shouldExposeDiscoveringPeerListLifecycleForEmptyPeers() {
        val state = ComposePeerListState(peers = emptyList(), selectedPeerIndex = 0)
        val lifecycle = state.peerListLifecyclePlan

        assertEquals(ComposePeerListLifecycleState.DISCOVERING, lifecycle.currentState)
        assertTrue(lifecycle.blockedReasons.isEmpty(), "blockedReasons: ${lifecycle.blockedReasons}")
        assertTrue(lifecycle.readySteps.any { it.state == ComposePeerListLifecycleState.DISCOVERING })
    }

    @Test
    fun shouldExposeDefaultPeerListTransitionIntents() {
        val state = ComposePeerListState()
        val transitions = state.peerListTransitionPlan

        assertEquals("Peer-list transition intents", transitions.title)
        assertEquals(8, transitions.transitions.size)
        assertTrue(transitions.enabledTransitions.isNotEmpty())
        assertTrue(transitions.enabledSummary.contains("Select peer"))
    }

    @Test
    fun shouldExposeDefaultPeerListAdapterEventRouting() {
        val state = ComposePeerListState()
        val routing = state.peerListAdapterEventRouting

        assertEquals("Peer-list adapter event contract", routing.title)
        assertTrue(routing.subtitle.contains("Side-effect-free"), "subtitle: ${routing.subtitle}")
        assertEquals(6, routing.contracts.size)
        assertTrue(routing.fallbackAvailable)
        assertTrue(routing.readyCount >= 3, "readyCount=${routing.readyCount}")
        assertTrue(routing.eventOrderLabel.startsWith("peer-discovered"), "eventOrderLabel: ${routing.eventOrderLabel}")
        assertTrue(routing.eventOrderLabel.endsWith("peer-list-refreshed"))
    }

    @Test
    fun shouldBlockPeerListTransitionsWhenFallbackUnavailable() {
        val state = ComposePeerListState(javaFxFallbackAvailable = false)
        val transitions = state.peerListTransitionPlan

        assertEquals(0, transitions.enabledTransitions.size)
        assertEquals(8, transitions.blockedTransitions.size)
        assertTrue(
            transitions.blockedTransitions.any { it.kind == ComposePeerListTransitionKind.SELECT_PEER && !it.enabled },
            "blockedSummary: ${transitions.blockedSummary}"
        )
    }

    @Test
    fun shouldBlockPeerListAdapterEventsWhenFallbackUnavailable() {
        val state = ComposePeerListState(javaFxFallbackAvailable = false)
        val routing = state.peerListAdapterEventRouting

        assertEquals(false, routing.fallbackAvailable)
        assertEquals(0, routing.readyCount)
        assertEquals(6, routing.blockedCount)
        assertTrue(routing.readinessSummary.contains("All 6"), "readinessSummary: ${routing.readinessSummary}")
        routing.contracts.forEach { contract ->
            assertTrue(
                contract.blockedReason.contains("JavaFX fallback"),
                "Expected ${contract.kind} to mention fallback, got: ${contract.blockedReason}"
            )
        }
    }

    @Test
    fun shouldMapDiscoveredPeerIntoComposePeerListItem() {
        val peer = DiscoveredPeer(
            "peer-phone",
            "Phone",
            "192.168.1.42",
            NetworkConstants.DEFAULT_CHAT_PORT,
            NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
            Instant.parse("2026-05-25T18:00:00Z"),
        )

        val item = ComposePeerListItem.fromDiscoveredPeer(peer)

        assertEquals("Phone", item.nickname)
        assertTrue(item.online)
        assertTrue(item.discovered)
        assertTrue(item.listMeta.contains("chat"))
        assertTrue(item.listMeta.contains("file"))
        assertTrue(item.selectedMeta.contains("nearby discovery"))
    }

    @Test
    fun shouldTreatDesktopPeerWithFileEndpointAsFileCapableWhenCapabilitiesLagPresence() {
        val peer = PeerPresence(
            "Victor",
            true,
            null,
            "192.168.1.77",
            NetworkConstants.DEFAULT_CHAT_PORT,
            NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
            Instant.parse("2026-05-25T18:00:00Z"),
            PeerCapabilities.desktop("0.5.0", NetworkConstants.DEFAULT_FILE_TRANSFER_PORT).withFileReceiver(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, enabled = false),
        )

        val item = ComposePeerListItem.fromPeer(peer, clientConnected = true)
        val state = ComposeFileTransferState(
            statusState = ComposeStatusConnectionState(clientConnected = true),
            peerListState = ComposePeerListState(peers = listOf(item), selectedPeerIndex = 0),
            selectedFilePath = "demo.bin",
            senderId = "Morpheus",
            sessionPassword = "secret",
        )

        assertEquals(true, item.online)
        assertEquals(false, item.discovered)
        assertEquals(true, item.fileCapable)
        assertEquals(true, state.canSendSelectedFile)
    }

    @Test
    fun shouldPreservePeerSelectionByNicknameAfterRefreshSorting() {
        val peers = listOf(
            ComposePeerListItem("zeta", online = false, discovered = true, listMeta = "offline", selectedMeta = "offline"),
            ComposePeerListItem("Beta", online = true, discovered = true, listMeta = "beta", selectedMeta = "beta"),
            ComposePeerListItem("alpha", online = true, discovered = true, listMeta = "alpha", selectedMeta = "alpha"),
        )

        val state = ComposePeerListState(
            peers = peers,
            selectedPeerIndex = -1,
            selectedPeerNickname = "Beta",
        )

        assertEquals(listOf("alpha", "Beta", "zeta"), state.visiblePeers.map { it.nickname })
        assertEquals(1, state.resolvedSelectedPeerIndex)
        assertEquals("Beta", state.selectedPeerTitle)
        assertEquals("Beta", state.selectionKeyFor(1))
    }

    @Test
    fun shouldExposeSelectedPeerTargetKindWithoutStartingRuntimeAction() {
        val state = ComposePeerListState(
            selectedPeerIndex = 0,
            selectedTargetKind = ComposePeerTargetCommandKind.FILE_TARGET,
        )

        assertEquals("Astra Laptop", state.selectedPeerTitle)
        assertTrue(state.actionSummary.contains("File target selected"), state.actionSummary)
        assertTrue(
            state.targetControlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).displayLabel.contains("selected"),
            state.targetControlPlan.command(ComposePeerTargetCommandKind.FILE_TARGET).displayLabel,
        )
    }
}
