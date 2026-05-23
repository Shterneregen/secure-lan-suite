package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.event.ChatConnectedEvent
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent
import com.shterneregen.securelan.chat.event.ChatSignalReceivedEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.chat.service.RandomNicknameService
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest
import com.shterneregen.securelan.filetransfer.service.FileTransferClientService
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import com.shterneregen.securelan.common.model.FileTransferProgress
import com.shterneregen.securelan.common.model.TransferStatus
import com.shterneregen.securelan.webrtc.event.RtcAudioLevelEvent
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcMediaDevice
import com.shterneregen.securelan.webrtc.service.RtcMediaDeviceService
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest
import com.shterneregen.securelan.webrtc.service.RtcSessionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.time.Duration
import java.time.Instant
import java.util.Optional

class ComposeDesktopHostAdapterTest {
    @Test
    fun shouldMirrorJavaFxChatTranscriptForChatEvents() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.chatEventPublisher.publish(ChatConnectedEvent("Alice", "127.0.0.1"))
        adapter.chatEventPublisher.publish(ChatMessageReceivedEvent("Bob", "hello"))
        adapter.chatEventPublisher.publish(ChatMessageSentEvent("Alice", "hello back"))
        adapter.chatEventPublisher.publish(ChatUserJoinedEvent("Carol", "127.0.0.2"))
        adapter.chatEventPublisher.publish(ChatUserLeftEvent("Bob"))
        adapter.chatEventPublisher.publish(ChatDisconnectedEvent("Alice", "closed"))
        adapter.chatEventPublisher.publish(ChatErrorEvent("boom", IllegalStateException("details")))

        assertEquals(
            listOf(
                "[connected] Alice -> 127.0.0.1",
                "Bob: hello",
                "[join] Carol",
                "[left] Bob",
                "[disconnected] Alice - closed",
                "[error] boom -> details",
            ),
            adapter.chatTranscript,
        )
    }

    @Test
    fun shouldMirrorJavaFxPeerListFromChatJoinLeaveWithoutAddingSelf() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.openRoom("Alice", "chatpass", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, discoverable = true)
        adapter.chatEventPublisher.publish(ChatUserJoinedEvent(adapter.statusState.nickname, "127.0.0.1:55000"))
        adapter.chatEventPublisher.publish(ChatUserJoinedEvent("Bob", "127.0.0.2:55001"))
        adapter.chatEventPublisher.publish(ChatMessageReceivedEvent("Carol", "hello"))
        adapter.chatEventPublisher.publish(ChatUserLeftEvent("Bob"))

        assertEquals(listOf("Carol", "Bob"), adapter.visiblePeerItems.map { it.nickname() })
        assertEquals(listOf(true, false), adapter.visiblePeerItems.map { it.online() })
        assertTrue(adapter.visiblePeerItems.none { it.nickname().equals(adapter.statusState.nickname, ignoreCase = true) })
        assertTrue(adapter.visiblePeerItems.first { it.nickname() == "Bob" }.discovered())
    }

    @Test
    fun shouldHideLocalDiscoveryAnnouncementFromVisiblePeerList() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.openRoom("Alice", "chatpass", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, discoverable = true)
        fixture.discovery.discover(
            DiscoveredPeer(
                "external-peer",
                "Beta",
                "192.168.1.20",
                NetworkConstants.DEFAULT_CHAT_PORT,
                NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
                Instant.now(),
            ),
        )
        fixture.discovery.discover(
            DiscoveredPeer(
                "loopback-peer",
                "Alice",
                "127.0.0.1",
                NetworkConstants.DEFAULT_CHAT_PORT,
                NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
                Instant.now(),
            ),
        )

        assertEquals(listOf("Beta"), adapter.visiblePeers.map { it.nickname })
        assertEquals(listOf("Beta"), adapter.visiblePeerItems.map { it.nickname() })
    }

    @Test
    fun shouldInitializeStatusNicknameFromRandomNicknameService() {
        val fixture = AdapterFixture()

        assertEquals("Alice", fixture.adapter.statusState.nickname)
    }

    @Test
    fun shouldUpdateStatusNicknameAndConnectRequestWhenManualClientConnects() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.connect("127.0.0.1", "New Compose Client", "chatpass", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT)

        assertEquals("New Compose Client", adapter.statusState.nickname)
        assertEquals("New Compose Client", fixture.chatClient.requests.single().nickname)
        assertEquals("127.0.0.1", adapter.statusState.manualHost)
    }

    @Test
    fun shouldKeepRtcSignalsInChatCoreDiagnosticsWithoutChangingWireFormat() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter
        val signal = RtcSignalEnvelope.offer(
            "Alice",
            "Bob",
            RtcSessionMode.AUDIO,
            "securelan-data",
            "sdp-offer",
        )

        adapter.chatEventPublisher.publish(ChatSignalReceivedEvent(signal))

        assertEquals(emptyList<String>(), adapter.chatTranscript)
        assertTrue(adapter.peerListDiagnostics.last().contains("RTC signal preserved through chat-core"))
        assertTrue(adapter.peerListDiagnostics.last().contains("OFFER"))
    }

    @Test
    fun shouldGuardChatSendWhenDisconnectedOrBlank() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.sendMessage("  ")
        adapter.sendMessage("hello")

        assertEquals(emptyList<String>(), fixture.chatClient.sentMessages)
        assertTrue(adapter.adapterEvents.any { it.message.contains("Message is empty") })
        assertTrue(adapter.adapterEvents.any { it.message.contains("Connect to a room") })
    }

    @Test
    fun shouldSendTrimmedChatMessageWhenConnected() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter
        fixture.chatClient.connected = true

        adapter.sendMessage("  hello compose  ")

        assertEquals(listOf("hello compose"), fixture.chatClient.sentMessages)
        assertTrue(adapter.chatConnected)
    }

    @Test
    fun shouldExposeManualPeersForRuntimeTargetSelection() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.addManualPeer("Beta", "192.168.1.20", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT)
        adapter.addManualPeer("alpha", "192.168.1.21", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT)

        assertEquals(listOf("alpha", "Beta"), adapter.visiblePeers.map { it.nickname })
        assertTrue(adapter.peerListDiagnostics.last().contains("visible=2"))
    }

    @Test
    fun shouldMirrorFileTransferEventsIntoComposeTransferRowsAndTranscript() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.fileTransferEventPublisher.publish(FileTransferStartedEvent("tx-1", "demo.txt", 2048, true))
        adapter.fileTransferEventPublisher.publish(
            FileTransferProgressEvent("tx-1", FileTransferProgress("tx-1", 1024, 2048, TransferStatus.IN_PROGRESS), true),
        )
        adapter.fileTransferEventPublisher.publish(FileTransferCompletedEvent("tx-1", "demo.txt", Path.of("demo.txt"), 2048, true))
        adapter.fileTransferEventPublisher.publish(FileTransferFailedEvent("tx-2", "bad.txt", "boom", IllegalStateException("boom"), false))

        assertEquals(listOf("demo.txt", "bad.txt"), adapter.transferEntries.map { it.fileName })
        assertEquals(listOf("Completed", "Failed"), adapter.transferEntries.map { it.status })
        assertTrue(adapter.chatTranscript.any { it.contains("[file-send] started: demo.txt") })
        assertTrue(adapter.chatTranscript.any { it.contains("[file-recv] failed: boom") })
        assertTrue(adapter.transferDiagnostics.any { it.contains("Transfer completed") })
    }

    @Test
    fun shouldCaptureIncomingTransferPromptsAndGuardAcceptance() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter
        val metadata = FileTransferMetadata("rx-1", "Beta", "Alice", "incoming.txt", 4096)

        assertFalse(adapter.acceptIncomingFileTransfer(metadata, "192.168.1.20", autoAccept = true))
        fixture.chatClient.connected = true
        adapter.addManualPeer("Beta", "192.168.1.20", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT)

        assertTrue(adapter.acceptIncomingFileTransfer(metadata, "192.168.1.20", autoAccept = true))
        adapter.recordIncomingFileDecision("rx-1", accepted = false)

        assertEquals(2, adapter.incomingTransferPrompts.size)
        assertTrue(adapter.incomingTransferPrompts.last().header.contains("Beta"))
        assertTrue(adapter.chatTranscript.any { it.contains("auto-accepted incoming.txt") })
        assertTrue(adapter.chatTranscript.any { it.contains("rejected incoming.txt") })
    }

    @Test
    fun shouldSendOutgoingFileToSelectedPeerThroughComposeAdapter() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter
        val file = Files.createTempFile("securelan-compose-send", ".txt")
        Files.writeString(file, "hello")
        fixture.chatClient.connected = true
        val peer = DiscoveredPeer("peer-beta", "Beta", "192.168.1.20", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, Instant.now())

        val transferId = adapter.sendFileToPeer(file, " Alice ", peer, "secret")

        assertEquals("tx-1", transferId)
        assertEquals(1, fixture.fileTransferClient.requests.size)
        assertEquals("192.168.1.20", fixture.fileTransferClient.requests.first().host)
        assertEquals(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, fixture.fileTransferClient.requests.first().port)
        assertEquals("Alice", fixture.fileTransferClient.requests.first().senderId)
        assertEquals("Beta", fixture.fileTransferClient.requests.first().recipientId)
        assertTrue(adapter.transferDiagnostics.any { it.contains("Outgoing file send started") })
    }

    @Test
    fun shouldExposeQuickShareStatusRowsAndDiagnostics() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.startQuickShare(NetworkConstants.DEFAULT_QUICK_SHARE_PORT)
        adapter.createTextQuickShare(" hello quick share ", 5, 2)

        assertTrue(adapter.quickShareRunning)
        assertEquals("Quick share running on port ${NetworkConstants.DEFAULT_QUICK_SHARE_PORT}", adapter.quickShareStatus)
        assertEquals(1, adapter.quickShareEntries.size)
        assertTrue(adapter.quickShareEntries.first().url().contains("http://127.0.0.1"))
        assertTrue(adapter.chatTranscript.any { it.contains("[quick-share] text link copied") })

        adapter.stopQuickShareEntry(adapter.quickShareEntries.first().id())
        assertFalse(adapter.quickShareEntries.first().active())

        adapter.stopQuickShare()
        assertFalse(adapter.quickShareRunning)
        assertEquals("Quick share idle", adapter.quickShareStatus)
    }

    @Test
    fun shouldExposeMediaDeviceRefreshAndRtcEventsForComposeVoiceVideoCards() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.refreshMediaDevices()
        adapter.testMicrophone("mic-1")
        adapter.testCamera("cam-1")
        adapter.rtcEventPublisher.publish(RtcAudioLevelEvent("rtc-1", "Alice", true, 0.55, true))
        adapter.rtcEventPublisher.publish(RtcStateChangedEvent("rtc-1", "Beta", RtcSessionMode.AUDIO, RtcSessionState.CONNECTED, "Connected"))

        assertEquals(listOf("System default microphone", "USB Microphone"), adapter.mediaVoiceState.microphones.map { it.label })
        assertEquals(listOf("System default camera", "USB Camera"), adapter.experimentalVideoState.cameras.map { it.label })
        assertEquals("Microphone is available: mic-1", adapter.mediaVoiceState.microphoneTestStatus)
        assertEquals("Camera is available: cam-1", adapter.experimentalVideoState.cameraTestStatus)
        assertEquals(0.55, adapter.mediaVoiceState.localAudioLevel)
        assertTrue(adapter.realtimeDiagnostics.any { it.contains("AUDIO session CONNECTED") })
    }

    @Test
    fun shouldStartAndCloseRealtimeSessionThroughComposeAdapter() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter
        fixture.chatClient.connected = true

        adapter.startRealtimeSession("Alice", "Beta", RtcSessionMode.AUDIO, audioDeviceId = "mic-1")

        assertEquals(1, fixture.rtcSession.requests.size)
        assertEquals("Alice", fixture.rtcSession.requests.first().localPeer())
        assertEquals("Beta", fixture.rtcSession.requests.first().remotePeer())
        assertEquals(RtcSessionMode.AUDIO, fixture.rtcSession.requests.first().mode())
        assertTrue(adapter.realtimeDiagnostics.any { it.contains("AUDIO session NEGOTIATING") })

        adapter.closeRealtimeSession()
        assertEquals(true, fixture.rtcSession.closedCurrentSession)
    }

    @Test
    fun shouldManageCameraPreviewLifecycleThroughComposeAdapter() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.startCameraPreview("cam-1")

        assertEquals(true, adapter.experimentalVideoState.previewRunning)
        assertEquals("Camera preview live • 2x2", adapter.experimentalVideoState.previewStatus)
        assertTrue(adapter.realtimeDiagnostics.any { it.contains("Camera preview started") })

        adapter.closeCameraPreview()
        assertEquals(false, adapter.experimentalVideoState.previewRunning)
        assertEquals(true, fixture.mediaDevice.previewClosed)
    }

    @Test
    fun shouldAggregateLiveDiagnosticsAndRegressionReadinessEvidence() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter
        fixture.chatClient.connected = true
        adapter.addManualPeer("Beta", "192.168.1.20", NetworkConstants.DEFAULT_CHAT_PORT, NetworkConstants.DEFAULT_FILE_TRANSFER_PORT)
        adapter.chatEventPublisher.publish(ChatMessageReceivedEvent("Beta", "hello"))
        adapter.fileTransferEventPublisher.publish(FileTransferStartedEvent("tx-1", "demo.txt", 2048, true))
        adapter.startQuickShare(NetworkConstants.DEFAULT_QUICK_SHARE_PORT)
        adapter.refreshMediaDevices()

        val diagnostics = adapter.diagnosticsState
        assertTrue(diagnostics.chatDiagnostics.any { it.contains("hello") })
        assertTrue(diagnostics.diagnosticChannelSummary.contains("quick-share="))
        assertTrue(diagnostics.quickShareDiagnostics.any { it.contains("Quick share") || it.contains("Landing") })

        adapter.updateRuntimeValidationEvidence(
            chatRuntimeValidated = true,
            fileTransferRuntimeValidated = true,
            quickShareRuntimeValidated = true,
            steganographyRuntimeValidated = true,
            voiceRuntimeValidated = true,
            videoRuntimeValidated = true,
            fullRuntimeRegressionValidated = true,
        )

        assertEquals(true, adapter.regressionReadinessState.allRuntimeValidated)
        assertTrue(adapter.regressionReadinessState.summary.contains("Compose regression gates"))
        assertTrue(adapter.regressionReadinessState.runtimeEvidenceSummary.contains("7 of 7"))
        assertEquals(emptyList<ComposeRuntimeEvidenceRequirement>(), adapter.regressionReadinessState.missingRuntimeEvidence)
        assertEquals(emptyList<ComposeRegressionGate>(), adapter.regressionReadinessState.blockedGates)
    }

    @Test
    fun shouldRecordPackagingEvidenceWithoutPromotingComposeLauncher() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.updatePackagingValidationEvidence(
            desktopBuildPassed = true,
            composeRuntimeSmokePassed = true,
            portableZipValidated = true,
        )

        assertEquals("com.shterneregen.securelan.desktop.Main", adapter.packagingReadinessState.applicationMainClass)
        assertEquals(false, adapter.packagingReadinessState.canPromoteComposeLauncher)
        assertEquals(false, adapter.packagingReadinessState.releaseValidationReady)
        assertEquals(ComposeLauncherDecisionKind.CONTINUE_VALIDATION, adapter.packagingReadinessState.launcherDecision.recommendedOption.kind)
        assertTrue(adapter.packagingReadinessState.blockedGates.any { it.kind == ComposePackagingGateKind.WINDOWS_EXE })
        assertTrue(adapter.packagingReadinessState.blockedGates.any { it.kind == ComposePackagingGateKind.LAUNCHER_DECISION })
    }

    @Test
    fun shouldRecordCompletePackagingEvidenceButStillRequirePromotionApproval() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.updatePackagingValidationEvidence(
            desktopBuildPassed = true,
            composeRuntimeSmokePassed = true,
            portableZipValidated = true,
            windowsExeValidated = true,
            fullRuntimeRegressionValidated = true,
        )

        assertEquals(true, adapter.packagingReadinessState.releaseValidationReady)
        assertEquals(false, adapter.packagingReadinessState.canPromoteComposeLauncher)
        assertEquals(ComposeLauncherDecisionKind.KEEP_JAVAFX_FALLBACK, adapter.packagingReadinessState.launcherDecision.recommendedOption.kind)
        assertTrue(adapter.packagingReadinessState.launcherDecision.blockerSummary.contains("approval"))
    }

    @Test
    fun shouldRecordIndividualRuntimeEvidenceChecklistItems() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.recordRuntimeEvidence(ComposeRuntimeEvidenceKind.CHAT_INTEROP)
        adapter.recordRuntimeEvidence(ComposeRuntimeEvidenceKind.QUICK_SHARE)

        assertEquals(true, adapter.regressionReadinessState.chatRuntimeValidated)
        assertEquals(true, adapter.regressionReadinessState.quickShareRuntimeValidated)
        assertEquals(false, adapter.regressionReadinessState.fileTransferRuntimeValidated)
        assertTrue(adapter.regressionReadinessState.acceptedRuntimeFlowSummary.contains("Chat interop"))
        assertTrue(adapter.regressionReadinessState.pendingRuntimeFlowSummary.contains("Encrypted file transfer"))
        assertTrue(adapter.regressionReadinessState.runtimeRegressionChecklist.any {
            it.requirementKind == ComposeRuntimeEvidenceKind.CHAT_INTEROP && it.recorded
        })
    }

    @Test
    fun shouldRecordIndividualPackagingArtifactEvidence() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.recordPackagingArtifactEvidence(ComposePackagingArtifactKind.COMPOSE_ENTRYPOINT)
        adapter.recordPackagingArtifactEvidence(ComposePackagingArtifactKind.PORTABLE_ZIP)

        assertEquals(true, adapter.packagingReadinessState.composeRuntimeSmokePassed)
        assertEquals(true, adapter.packagingReadinessState.portableZipValidated)
        assertEquals(false, adapter.packagingReadinessState.windowsExeValidated)
        assertTrue(adapter.packagingReadinessState.artifactSummary.contains("3 of 4"))
        assertTrue(adapter.packagingReadinessState.pendingArtifactSummary.contains("WiX 5.0.2"))
        assertTrue(adapter.packagingReadinessState.artifactRequirements.any {
            it.kind == ComposePackagingArtifactKind.JAVAFX_LAUNCHER && it.validated
        })
    }

    @Test
    fun shouldMirrorFullRegressionEvidenceIntoPackagingGate() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.recordFullRegressionPackagingEvidence()

        assertEquals(true, adapter.regressionReadinessState.fullRuntimeRegressionValidated)
        assertEquals(true, adapter.packagingReadinessState.fullRuntimeRegressionValidated)
        assertTrue(adapter.packagingReadinessState.promotionChecklistSummary.contains("runtime regression=true"))
        assertTrue(adapter.packagingReadinessState.promotionDecisionSteps.any {
            it.kind == ComposePromotionDecisionStepKind.COMPLETE_RUNTIME_REGRESSION && it.satisfied
        })
    }

    @Test
    fun shouldCaptureRuntimeEvidenceRecordsForRegressionReview() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.recordRuntimeEvidenceRecord(
            ComposeRuntimeEvidenceKind.CHAT_INTEROP,
            "desktop-to-desktop chat smoke passed",
            ComposeRuntimeEvidenceChecklistStatus.RECORDED,
            Instant.parse("2026-05-26T20:20:00Z"),
        )
        adapter.recordRuntimeEvidenceRecord(
            ComposeRuntimeEvidenceKind.FILE_TRANSFER,
            "desktop and Android file transfer accepted",
            ComposeRuntimeEvidenceChecklistStatus.ACCEPTED,
            Instant.parse("2026-05-26T20:21:00Z"),
        )

        assertEquals(false, adapter.regressionReadinessState.chatRuntimeValidated)
        assertEquals(true, adapter.regressionReadinessState.fileTransferRuntimeValidated)
        assertEquals(1, adapter.regressionReadinessState.acceptedRuntimeEvidenceRecords.size)
        assertTrue(adapter.regressionReadinessState.runtimeEvidenceRecordSummary.contains("1 accepted"))
        assertTrue(adapter.regressionReadinessState.runtimeEvidenceCopyText.contains("file-transfer=accepted"))
    }

    @Test
    fun shouldCapturePackagingEvidenceRecordsWithoutChangingLauncher() {
        val fixture = AdapterFixture()
        val adapter = fixture.adapter

        adapter.recordPackagingEvidenceRecord(
            ComposePackagingEvidenceKind.DESKTOP_BUILD,
            true,
            "desktop build passed",
            Instant.parse("2026-05-26T20:30:00Z"),
        )
        adapter.recordPackagingEvidenceRecord(
            ComposePackagingEvidenceKind.PORTABLE_ZIP,
            true,
            "portable zip launched with JavaFX fallback",
            Instant.parse("2026-05-26T20:31:00Z"),
        )
        adapter.recordPackagingEvidenceRecord(
            ComposePackagingEvidenceKind.WINDOWS_EXE,
            false,
            "WiX validation pending",
            Instant.parse("2026-05-26T20:32:00Z"),
        )

        assertEquals("com.shterneregen.securelan.desktop.Main", adapter.packagingReadinessState.applicationMainClass)
        assertEquals(true, adapter.packagingReadinessState.desktopBuildPassed)
        assertEquals(true, adapter.packagingReadinessState.portableZipValidated)
        assertEquals(false, adapter.packagingReadinessState.windowsExeValidated)
        assertEquals(2, adapter.packagingReadinessState.acceptedEvidenceRecords.size)
        assertTrue(adapter.packagingReadinessState.packagingEvidenceCopyText.contains("portable-zip=validated"))
        assertTrue(adapter.packagingReadinessState.validationReport.copyText.contains("WiX validation pending"))
    }

    private class AdapterFixture {
        val chatServer = FakeChatServerService()
        val chatClient = FakeChatClientService()
        val fileTransferServer = FakeFileTransferServerService()
        val fileTransferClient = FakeFileTransferClientService()
        val quickShare = FakeQuickShareService()
        val discovery = FakePeerDiscoveryService()
        val rtcSession = FakeRtcSessionService()
        val mediaDevice = FakeRtcMediaDeviceService()
        val adapter = ComposeDesktopHostAdapter(
            chatServerService = chatServer,
            chatClientService = chatClient,
            fileTransferServerService = fileTransferServer,
            quickShareService = quickShare,
            discoveryService = discovery,
            randomNicknameService = object : RandomNicknameService {
                override fun generate(): String = "Alice"
            },
            fileTransferClientService = fileTransferClient,
            rtcSessionService = rtcSession,
            rtcMediaDeviceService = mediaDevice,
        )
    }

    private class FakeFileTransferClientService : FileTransferClientService {
        val requests = mutableListOf<FileTransferClientRequest>()

        override fun sendFile(request: FileTransferClientRequest): String {
            requests += request
            return "tx-${requests.size}"
        }
    }

    private class FakeChatServerService : ChatServerService {
        var running = false

        override fun start(config: ChatServerConfig) {
            running = true
        }

        override fun stop() {
            running = false
        }

        override fun isRunning(): Boolean = running

        override fun connectedUsers(): Int = 0
    }

    private class FakeChatClientService : ChatClientService {
        var connected = false
        val sentMessages = mutableListOf<String>()
        val requests = mutableListOf<ChatClientConnectRequest>()

        override fun connect(request: ChatClientConnectRequest): Boolean {
            requests += request
            connected = true
            return true
        }

        override fun disconnect() {
            connected = false
        }

        override fun sendMessage(text: String?) {
            sentMessages += text ?: ""
        }

        override fun sendSignal(signal: RtcSignalEnvelope?) = Unit

        override fun isConnected(): Boolean = connected
    }

    private class FakeFileTransferServerService : FileTransferServerService {
        var running = false

        override fun start(config: FileTransferServerConfig) {
            running = true
        }

        override fun stop() {
            running = false
        }

        override fun isRunning(): Boolean = running
    }

    private class FakeQuickShareService : QuickShareService {
        private var running = false
        private var port = NetworkConstants.DEFAULT_QUICK_SHARE_PORT
        private val shares = LinkedHashMap<String, QuickShareSnapshot>()

        @Throws(IOException::class)
        override fun start(config: QuickShareServerConfig) {
            running = true
            port = config.port()
        }

        override fun stop() {
            running = false
        }

        override fun isRunning(): Boolean = running

        override fun port(): Int = port

        @Throws(IOException::class)
        override fun share(request: QuickShareCreateRequest): QuickShareSnapshot {
            val id = "share-${shares.size + 1}"
            val snapshot = QuickShareSnapshot(
                id,
                request.type(),
                request.displayName(),
                request.file()?.fileName?.toString() ?: "",
                0,
                Instant.parse("2026-05-25T19:00:00Z"),
                Instant.parse("2026-05-25T19:05:00Z"),
                request.accessLimit(),
                0,
                QuickShareStatus.ACTIVE,
                listOf("http://127.0.0.1:$port/s/$id"),
            )
            shares[id] = snapshot
            return snapshot
        }

        override fun findShare(id: String): Optional<QuickShareSnapshot> = Optional.ofNullable(shares[id])

        override fun shares(): List<QuickShareSnapshot> = shares.values.toList()

        override fun stopShare(id: String): Boolean {
            val snapshot = shares[id] ?: return false
            shares[id] = QuickShareSnapshot(
                snapshot.id(),
                snapshot.type(),
                snapshot.displayName(),
                snapshot.fileName(),
                snapshot.fileSize(),
                snapshot.createdAt(),
                snapshot.expiresAt(),
                snapshot.accessLimit(),
                snapshot.accessCount(),
                QuickShareStatus.STOPPED,
                snapshot.urls(),
            )
            return true
        }

        override fun landingUrls(): List<String> = if (running) listOf("http://127.0.0.1:$port/") else emptyList()
    }

    private class FakePeerDiscoveryService : PeerDiscoveryService {
        private var running = false
        private var announceEnabled = false
        private var listener: PeerDiscoveryListener? = null
        private val peers = mutableListOf<DiscoveredPeer>()

        override fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener) {
            running = true
            announceEnabled = config.announceEnabled
            this.listener = listener
        }

        override fun stop() {
            running = false
            peers.clear()
        }

        override fun setAnnounceEnabled(announceEnabled: Boolean) {
            this.announceEnabled = announceEnabled
        }

        override fun isRunning(): Boolean = running

        override fun snapshot(): List<DiscoveredPeer> = peers.toList()

        @Suppress("unused")
        fun discover(peer: DiscoveredPeer = DiscoveredPeer("peer", "Peer", "127.0.0.1", 5050, 5051, Instant.now())) {
            peers += peer
            listener?.onPeerDiscovered(peer)
        }
    }

    private class FakeRtcSessionService : RtcSessionService {
        val requests = mutableListOf<RtcSessionRequest>()
        var closedCurrentSession = false
        private var current: com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot? = null

        override fun runtimeStatus(): RtcRuntimeStatus = RtcRuntimeStatus("fake-rtc", true, "ready")

        override fun currentSession(): Optional<com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot> = Optional.ofNullable(current)

        override fun startSession(request: RtcSessionRequest): com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot {
            requests += request
            val snapshot = com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot(
                "rtc-${requests.size}",
                request.localPeer(),
                request.remotePeer(),
                request.mode(),
                request.dataChannelLabel(),
                RtcSessionState.NEGOTIATING,
                "Preparing fake session",
            )
            current = snapshot
            return snapshot
        }

        override fun acceptInboundSignal(localPeer: String?, signal: RtcSignalEnvelope?) = Unit

        override fun sendDataMessage(payload: String?) = Unit

        override fun closeCurrentSession() {
            closedCurrentSession = true
        }
    }

    private class FakeRtcMediaDeviceService : RtcMediaDeviceService {
        var previewClosed = false

        override fun audioCaptureDevices(): List<RtcMediaDevice> = listOf(RtcMediaDevice("mic-1", "USB Microphone", true))

        override fun videoCaptureDevices(): List<RtcMediaDevice> = listOf(RtcMediaDevice("cam-1", "USB Camera", true))

        override fun testAudioCaptureDevice(deviceId: String?): String = "Microphone is available: ${deviceId.orEmpty()}"

        override fun testVideoCaptureDevice(deviceId: String?): String = "Camera is available: ${deviceId.orEmpty()}"

        override fun startVideoPreview(deviceId: String?, frameConsumer: Consumer<com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent>): RtcMediaDeviceService.CameraPreviewSession {
            frameConsumer.accept(com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent("preview", "local", true, 2, 2, 0, ByteArray(16)))
            return object : RtcMediaDeviceService.CameraPreviewSession {
                override fun statusMessage(): String = "Camera preview started: ${deviceId.orEmpty()}"

                override fun close() {
                    previewClosed = true
                }
            }
        }
    }
}
