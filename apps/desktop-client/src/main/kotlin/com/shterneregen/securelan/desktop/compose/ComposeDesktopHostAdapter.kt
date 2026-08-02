package com.shterneregen.securelan.desktop.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.setValue
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.event.*
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.*
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.compose.logging.SecureLanLogger
import com.shterneregen.securelan.desktop.compose.settings.*
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatMessage
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeAdapterEventKind
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionEvent
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionEventKind
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeIncomingTransferPrompt
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeIncomingTransferPromptStatus
import com.shterneregen.securelan.desktop.ui.*
import com.shterneregen.securelan.filetransfer.event.*
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest
import com.shterneregen.securelan.filetransfer.service.FileTransferClientService
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import com.shterneregen.securelan.stego.StegoServices
import com.shterneregen.securelan.stego.model.BmpCapacity
import com.shterneregen.securelan.stego.service.SteganographyService
import com.shterneregen.securelan.webrtc.event.*
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcMediaDeviceService
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest
import com.shterneregen.securelan.webrtc.service.RtcSessionService
import com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

internal val transferEntriesMutationPolicy: SnapshotMutationPolicy<List<TransferEntry>> =
    referentialEqualityPolicy()

/**
 * Live desktop host adapter for status/connection, peer-list, and chat workspace wiring.
 *
 * Wraps [ChatServerService], [ChatClientService], [PeerDiscoveryService], and optional [ChatEventPublisher]
 * to manage room hosting, manual connection, discovery visibility, peer snapshots, and shared-room chat.
 * Publishes [ComposeConnectionEvent] events consumed by the Compose shell.
 */
class ComposeDesktopHostAdapter(
    private val chatServerService: ChatServerService,
    private val chatClientService: ChatClientService,
    private val fileTransferServerService: FileTransferServerService,
    private val quickShareService: QuickShareService,
    private val discoveryService: PeerDiscoveryService,
    private val randomNicknameService: RandomNicknameService,
    private val fileTransferClientService: FileTransferClientService? = null,
    private val steganographyService: SteganographyService = StegoServices.createDefault().steganographyService(),
    private val rtcSessionService: RtcSessionService? = null,
    private val rtcMediaDeviceService: RtcMediaDeviceService? = null,
    private val settingsController: DesktopAppSettingsController = DesktopAppSettingsController(),
    downloadsPath: Path = settingsController.settings.downloadsPath(),
    private val uiStateDispatcher: (((() -> Unit)) -> Unit)? = null,
) : AutoCloseable {
    private var downloadsPath: Path = downloadsPath.toAbsolutePath().normalize()

    /** Live status/connection state, updated after each action. */
    var statusState: ComposeStatusConnectionState by mutableStateOf(
        ComposeStatusConnectionState(
            nickname = settingsController.settings.displayName
                ?: randomNicknameService.generate(),
            manualHost = settingsController.settings.network.recentRooms.firstOrNull()?.host ?: "127.0.0.1",
            serverChatPortText = settingsController.settings.network.serverChatPort.toString(),
            serverFilePortText = settingsController.settings.network.serverFilePort.toString(),
            clientChatPortText = settingsController.settings.network.clientChatPort.toString(),
            clientFilePortText = settingsController.settings.network.clientFilePort.toString(),
            discoverable = settingsController.settings.network.discoverable,
        ),
    )
        private set

    /** Last room password used by hosting/manual connect; reused by encrypted file send UI. */
    var currentRoomPassword: String by mutableStateOf(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.roomPasswordPlaceholder)
        private set

    /** Mirrors the transfer checkbox used by the listener acceptance callback. */
    var autoAcceptIncomingFiles: Boolean by mutableStateOf(
        settingsController.settings.transfers.incomingFileConfirmation ==
            IncomingFileConfirmationMode.AUTO_ACCEPT_KNOWN_PEERS,
    )
        private set

    /** Adapter events since the last clear, in chronological order. */
    var adapterEvents: List<ComposeConnectionEvent> by mutableStateOf(emptyList())
        private set

    /** Lightweight workspace feedback for hover/focus-adjacent microinteractions and one-shot state changes. */
    var microinteractionEvents: List<ComposeConnectionEvent> by mutableStateOf(emptyList())
        private set

    /** Snapshot of discovered peers from the discovery service. */
    var discoveredPeers: List<DiscoveredPeer> by mutableStateOf(emptyList())
        private set

    /** Chat-room peers observed through USER_JOINED, chat, signal, and USER_LEFT events. */
    var chatPeers: List<PeerPresence> by mutableStateOf(emptyList())
        private set

    /** Discovered peer list rendered by the Compose shell. */
    val visiblePeers: List<DiscoveredPeer>
        get() = discoveredPeers
            .filterNot(::isLocalPeer)
            .distinctBy { it.peerId }
            .sortedWith(Comparator.comparing(DiscoveredPeer::nickname, String.CASE_INSENSITIVE_ORDER))

    /** Combined peer list: discovered LAN targets plus connected chat participants. */
    val visiblePeerItems: List<PeerPresence>
        get() {
            val merged = LinkedHashMap<String, PeerPresence>()
            chatPeers.filterNot(::isLocalPeer).forEach { peer ->
                merged[peerKey(peer)] = peer
            }
            visiblePeers.forEach { peer ->
                val existing = chatPeers.firstOrNull { DesktopMainViewHelpers.samePeer(it, peer.nickname, peer.peerId) }
                if (existing != null) {
                    merged.remove(peerKeyForDiscovered(peer))
                    val enriched = DesktopMainViewHelpers.mergeChatAndDiscoveredPeer(existing, peer)
                    merged.remove(peerKey(existing))
                    merged[peerKey(enriched)] = enriched
                } else {
                    val presence = PeerPresence(peer.nickname, true, peer.peerId, peer.host, peer.chatPort, peer.filePort, peer.lastSeen)
                    merged[peerKey(presence)] = presence
                }
            }
            return merged.values
                .sortedWith(
                    Comparator.comparing(PeerPresence::online).reversed()
                        .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
                )
        }

    /** Chat transcript lines for workspace wiring. */
    var chatTranscript: List<String> by mutableStateOf(emptyList())
        private set

    /** Chat transcript messages with UI-only receive timestamps; wire-format text stays unchanged. */
    var chatMessages: List<ComposeChatMessage> by mutableStateOf(emptyList())
        private set

    /** File transfer rows for encrypted-transfer workspace wiring. */
    // TransferEntry keeps mutable speed/progress counters. Compare list snapshots by identity so
    // every published event invalidates Compose even when the list contains the same entry objects.
    var transferEntries: List<TransferEntry> by mutableStateOf(
        emptyList(),
        transferEntriesMutationPolicy,
    )
        private set

    /** Incoming receive prompts captured before acceptance decisions. */
    var incomingTransferPrompts: List<ComposeIncomingTransferPrompt> by mutableStateOf(emptyList())
        private set

    private val pendingIncomingTransferDecisions = ConcurrentHashMap<String, CompletableFuture<Boolean>>()

    /** Dedicated coroutine scope for blocking desktop file-transfer client work. */
    private val fileTransferIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Quick-share rows for browser-link workspace wiring. */
    var quickShareEntries: List<QuickShareEntry> by mutableStateOf(emptyList())
        private set

    /** Quick-share status copy shown by Compose. */
    var quickShareStatus: String by mutableStateOf("Quick share idle")
        private set

    /** Quick-share landing URL copy shown by Compose. */
    var quickShareLanding: String by mutableStateOf("Start quick share to get LAN browser links.")
        private set

    /** Last Quick Share server error, shown next to the affected advanced setting. */
    var quickShareError: String? by mutableStateOf(null)
        private set

    /** Steganography UI state for Compose wiring. */
    var stegoState: ComposeSteganographyState by mutableStateOf(ComposeSteganographyState())
        private set

    /** Media/voice UI state for Compose wiring. */
    var mediaVoiceState: ComposeMediaVoiceState by mutableStateOf(
        ComposeMediaVoiceState(
            statusState = statusState,
            peerListState = ComposePeerListState(peers = emptyList()),
            selectedMicrophoneId = settingsController.settings.media.microphoneDeviceId,
            selectedOutputDeviceId = settingsController.settings.media.outputDeviceId,
        ),
    )
        private set

    /** Experimental camera/video UI state for Compose wiring. */
    var experimentalVideoState: ComposeExperimentalVideoState by mutableStateOf(
        ComposeExperimentalVideoState(
            statusState = statusState,
            peerListState = ComposePeerListState(peers = emptyList()),
            selectedCameraId = settingsController.settings.media.cameraDeviceId,
        ),
    )
        private set

    /** Whether the chat client is connected (for send readiness). */
    val chatConnected: Boolean get() = chatClientService.isConnected()
    val preferredConnectionMode: DesktopConnectionMode get() = settingsController.settings.network.lastConnectionMode

    /** Whether quick share is running. */
    var quickShareRunning: Boolean by mutableStateOf(quickShareService.isRunning())
        private set

    /** Quick-share landing URLs currently exposed by the local browser-link server. */
    var quickShareLandingUrls: List<String> by mutableStateOf(quickShareService.landingUrls())
        private set

    /** Local LAN addresses shown by the Compose profile card. */
    var localNetworkInfo: String by mutableStateOf(DesktopMainViewHelpers.localNetworkInfoMessage(emptyList()))
        private set

    /** Send an RTC signaling payload through the connected chat transport. */
    fun chatClientServiceSendSignal(signal: com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope?) {
        chatClientService.sendSignal(signal)
    }

    private val shuttingDown = AtomicBoolean(false)
    private val localPeerId = UUID.randomUUID().toString()
    private var currentConfig: PeerDiscoveryConfig? = null
    private val transferEntryMap = LinkedHashMap<String, TransferEntry>()
    private var cameraPreviewSession: RtcMediaDeviceService.CameraPreviewSession? = null

    val chatEventPublisher: ChatEventPublisher = ChatEventPublisher { event ->
        when (event) {
            is ChatConnectedEvent -> {
                // Do not render a local-client connection line in the chat transcript.
                clearChatPeers()
            }
            is ChatDisconnectedEvent -> {
                dispatchUiStateUpdate {
                    appendChatTranscript(disconnectedTranscriptLine(event))
                    clearChatPeers()
                    statusState = statusState.withClientDisconnected()
                    refreshRealtimeState()
                    publish(ComposeConnectionEventKind.INFO, event.reason ?: "Chat disconnected")
                }
            }
            is ChatMessageReceivedEvent -> {
                val sender = event.senderNickname ?: "unknown"
                val systemLikeMessage = isSystemSender(sender) || isSystemPrefixedChatText(event.text)
                val text = normalizeChatText(event.text, sender)
                if (systemLikeMessage) {
                    updatePeerPresenceFromSystemMessage(text)
                } else {
                    upsertChatPeer(sender, online = true)
                }
                appendChatTranscript(formatChatMessage(if (systemLikeMessage) "system" else sender, text))
            }
            is ChatMessageSentEvent -> {
                // The message is already shown through the normal chat receive flow; avoid duplicates.
            }
            is ChatUserJoinedEvent -> {
                // The server already broadcasts a "[system] <nickname> joined the chat" message,
                // which is the single user-facing presence event. Do not add an extra transcript line
                // here, otherwise hosts (which see both server-side and client-side events) show
                // duplicate join entries.
                upsertJoinedPeer(event)
            }
            is ChatUserLeftEvent -> {
                // Same as above: the "[system] <nickname> left the chat" message is the only
                // rendered leave event. Mark the peer offline without appending a duplicate line.
                markChatPeerOffline(event.nickname)
            }
            is ChatSignalReceivedEvent -> {
                upsertChatPeer(event.signal.fromPeer(), online = true)
                SecureLanLogger.logConnection("RTC signal preserved through chat-core: ${event.signal.type()} from ${event.signal.fromPeer()} to ${event.signal.toPeer()}.")
                rtcSessionService?.acceptInboundSignal(statusState.nickname.trim(), event.signal)
            }
            is ChatErrorEvent -> {
                val detail = "[error] ${event.message ?: "Chat error"}${event.cause?.message?.let { " -> $it" } ?: ""}"
                appendChatTranscript(detail)
                publish(ComposeConnectionEventKind.ERROR, event.message ?: "Chat error")
            }
        }
    }

    val fileTransferEventPublisher = com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher { event ->
        dispatchUiStateUpdate { handleFileTransferEvent(event) }
    }

    val quickShareEventPublisher = com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher { event ->
        handleQuickShareEvent(event)
    }

    val rtcEventPublisher = com.shterneregen.securelan.webrtc.service.RtcEventPublisher { event ->
        handleRtcEvent(event)
    }

    private val discoveryListener = object : PeerDiscoveryListener {
        override fun onPeerDiscovered(peer: DiscoveredPeer) {
            discoveredPeers = discoveryService.snapshot()
            SecureLanLogger.logConnection(DesktopMainViewHelpers.discoveryPeerFoundDiagnostics(peer) + "; snapshot=${discoveredPeers.size}.")
        }

        override fun onPeerExpired(peer: DiscoveredPeer) {
            discoveredPeers = discoveryService.snapshot()
            SecureLanLogger.logConnection(DesktopMainViewHelpers.discoveryPeerExpiredDiagnostics(peer) + "; snapshot=${discoveredPeers.size}.")
        }

        override fun onDiscoveryError(message: String, cause: Throwable) {
            SecureLanLogger.logConnection(DesktopMainViewHelpers.discoveryErrorDiagnostics(message, cause))
            if (!discoveryService.isRunning()) {
                publish(ComposeConnectionEventKind.ERROR, DesktopMainViewHelpers.discoveryChatMessage(message))
            }
            refreshState()
        }
    }

    init {
        publishLocalNetworkInfo()
        startPeerDiscoveryListener()
        refreshState()
    }

    // ---- public actions ----

    /**
     * Start hosting a room on the given ports, then optionally start discovery announcements.
     * Fires [ComposeAdapterEventKind.HOST_STARTED] on success.
     */
    fun openRoom(
        nickname: String,
        password: String,
        chatPort: Int,
        filePort: Int,
        discoverable: Boolean,
    ) {
        if (shuttingDown.get()) return
        val trimmedNick = nickname.trim()
        if (trimmedNick.isEmpty()) {
            publish(ComposeConnectionEventKind.WARNING, "Nickname is required to open a room.")
            return
        }
        if (chatServerService.isRunning()) {
            publish(ComposeConnectionEventKind.WARNING, "Room is already running; stop it first.")
            return
        }
        publishEventKind(ComposeAdapterEventKind.CLEANUP_STARTED, "Preparing to open room.")

        try {
            currentRoomPassword = password
            statusState = statusState.copy(
                nickname = trimmedNick,
                serverChatPortText = chatPort.toString(),
                serverFilePortText = filePort.toString(),
                discoverable = discoverable,
            )
            settingsController.update { settings ->
                settings.copy(
                    displayName = trimmedNick,
                    network = settings.network.copy(
                        discoverable = discoverable,
                        serverChatPort = chatPort,
                        serverFilePort = filePort,
                        lastConnectionMode = DesktopConnectionMode.HOST,
                    ),
                )
            }
            chatServerService.start(ChatServerConfig(chatPort, password))
            startFileTransferListener(filePort, password)
            publish(ComposeConnectionEventKind.SUCCESS, "Room opened. Chat on $chatPort, files on $filePort.")
            publishEventKind(ComposeAdapterEventKind.HOST_STARTED, "Host started: $trimmedNick")

            connectLocalHostedChat(chatPort, trimmedNick, password)

            val discoveryConfig = PeerDiscoveryConfig.defaults(localPeerId, trimmedNick, chatPort, filePort, discoverable)
            currentConfig = discoveryConfig
            discoveryService.start(discoveryConfig, discoveryListener)
            discoveredPeers = discoveryService.snapshot()
            SecureLanLogger.logConnection(
                "Discovery started on port ${discoveryConfig.discoveryPort}; announce=${discoveryConfig.announceEnabled}; snapshot=${discoveredPeers.size}.",
            )

            if (discoverable) {
                publish(ComposeConnectionEventKind.SUCCESS, "Room is visible to nearby peers.")
                publishEventKind(ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED, "Discoverable: true")
            } else {
                publish(ComposeConnectionEventKind.INFO, "Room is hidden from nearby peers.")
                publishEventKind(ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED, "Discoverable: false")
            }
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Failed to open room: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.RUNTIME_ERROR, e.message ?: "Unknown room hosting error")
        }

        refreshState()
    }

    /** Stop the room, discovery, and client if connected. */
    fun stopHosting() {
        if (shuttingDown.get()) return
        if (!chatServerService.isRunning()) {
            publish(ComposeConnectionEventKind.WARNING, "No room is currently running.")
            return
        }
        publishEventKind(ComposeAdapterEventKind.CLEANUP_STARTED, "Stopping hosting.")

        try {
            if (chatClientService.isConnected()) {
                chatClientService.disconnect()
                publish(ComposeConnectionEventKind.INFO, "Disconnected before stopping room.")
            }
            if (fileTransferServerService.isRunning()) {
                fileTransferServerService.stop()
                publish(ComposeConnectionEventKind.INFO, "File transfer listener stopped.")
            }
            discoveryService.stop()
            currentConfig = null
            discoveredPeers = emptyList()
            clearChatPeers()
            SecureLanLogger.logConnection("Discovery stopped; peer snapshot cleared.")
            chatServerService.stop()
            publish(ComposeConnectionEventKind.SUCCESS, "Room stopped.")
            publishEventKind(ComposeAdapterEventKind.HOST_STOPPED, "Host stopped.")
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Error stopping room: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.RUNTIME_ERROR, e.message ?: "Unknown stop hosting error")
        }

        refreshState()
    }

    /** Connect to a remote room manually. */
    fun connect(
        host: String,
        nickname: String,
        password: String,
        chatPort: Int,
        filePort: Int,
    ) {
        if (shuttingDown.get()) return
        val trimmedHost = host.trim()
        val trimmedNick = nickname.trim()
        if (trimmedHost.isEmpty() || trimmedNick.isEmpty()) {
            publish(ComposeConnectionEventKind.WARNING, "Room address and nickname are required to connect.")
            return
        }
        if (chatClientService.isConnected()) {
            publish(ComposeConnectionEventKind.WARNING, "Already connected; disconnect first.")
            return
        }
        publishEventKind(ComposeAdapterEventKind.CONNECT_STARTED, "Connecting to $trimmedHost")

        try {
            currentRoomPassword = password
            statusState = statusState.copy(
                nickname = trimmedNick,
                manualHost = trimmedHost,
                clientChatPortText = chatPort.toString(),
                clientFilePortText = filePort.toString(),
            )
            settingsController.update { settings ->
                settings.copy(
                    displayName = trimmedNick,
                    network = settings.network.copy(
                        clientChatPort = chatPort,
                        clientFilePort = filePort,
                        lastConnectionMode = DesktopConnectionMode.JOIN,
                    ),
                )
            }
            val request = ChatClientConnectRequest(trimmedHost, chatPort, trimmedNick, password, desktopCapabilities(filePort))
            val connected = chatClientService.connect(request)
            if (connected) {
                settingsController.update { settings ->
                    settings.copy(
                        network = settings.network.withRecentRoom(DesktopRecentRoom(trimmedHost, chatPort, filePort)),
                    )
                }
                startFileTransferListener(filePort, password)
                startPeerDiscoveryListenOnly(trimmedNick)
                publish(ComposeConnectionEventKind.SUCCESS, "Connected to $trimmedHost as $trimmedNick.")
                publishEventKind(ComposeAdapterEventKind.CONNECTED, "Connected: $trimmedNick")
            } else {
                publish(ComposeConnectionEventKind.ERROR, "Connection to $trimmedHost failed.")
                publishEventKind(ComposeAdapterEventKind.CONNECT_FAILED, "Connection failed: $trimmedHost")
            }
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Connection error: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.CONNECT_FAILED, e.message ?: "Unknown connection failure")
        }

        refreshState()
    }

    /** Disconnect the chat client. */
    fun disconnect() {
        if (shuttingDown.get()) return
        if (!chatClientService.isConnected()) {
            publish(ComposeConnectionEventKind.WARNING, "Not currently connected.")
            return
        }

        try {
            chatClientService.disconnect()
            clearChatPeers()
            if (!chatServerService.isRunning()) {
                startPeerDiscoveryListenOnly(statusState.nickname)
            }
            publish(ComposeConnectionEventKind.SUCCESS, "Disconnected.")
            publishEventKind(ComposeAdapterEventKind.DISCONNECTED, "Disconnected.")
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Disconnect error: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.RUNTIME_ERROR, e.message ?: "Unknown disconnect error")
        }

        refreshState()
    }

    /** Toggle discovery announcement mode. Only meaningful while a room is hosted. */
    fun setDiscoverable(enabled: Boolean) {
        if (shuttingDown.get()) return
        if (!discoveryService.isRunning()) {
            publish(ComposeConnectionEventKind.WARNING, "No room is open; start hosting first.")
            return
        }
        try {
            discoveryService.setAnnounceEnabled(enabled)
            settingsController.update { settings ->
                settings.copy(network = settings.network.copy(discoverable = enabled))
            }
            discoveredPeers = discoveryService.snapshot()
            val label = if (enabled) "Discoverable" else "Hidden"
            publish(ComposeConnectionEventKind.SUCCESS, "Room set to $label mode.")
            publishEventKind(ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED, label)
            SecureLanLogger.logConnection("Discovery visibility changed to $label; snapshot=${discoveredPeers.size}.")
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Failed to change discovery mode: ${e.message}")
            SecureLanLogger.logConnection("Failed to change discovery visibility: ${e.message ?: "unknown error"}.")
        }
        refreshState()
    }

    /** Resolve a selected Compose peer row to its advertised LAN file endpoint. */
    fun discoveredPeerFor(nickname: String): DiscoveredPeer? {
        val discovered = discoveredPeers.firstOrNull { it.nickname.equals(nickname, ignoreCase = true) }
        if (discovered != null) {
            return discovered
        }
        return visiblePeerItems.firstOrNull { it.nickname().equals(nickname, ignoreCase = true) && DesktopMainViewHelpers.selectedPeerFileCapable(it) }
            ?.let { peer ->
                DiscoveredPeer(
                    peer.peerId() ?: "peer-${peer.nickname().lowercase()}",
                    peer.nickname(),
                    peer.host().orEmpty(),
                    peer.chatPort(),
                    peer.filePort(),
                    peer.lastSeen() ?: Instant.now(),
                )
            }
    }

    /** Resolve a selected Compose peer row to the address fields used by the Join room form. */
    fun joinTargetFor(nickname: String): ComposeConnectionJoinTarget? = discoveredPeerFor(nickname)
        ?.takeIf { it.host.isNotBlank() && it.chatPort > 0 && it.filePort > 0 }
        ?.let(ComposeConnectionJoinTarget::fromDiscoveredPeer)

    /** Generate a default random nickname. */
    fun generateNickname(): String = randomNicknameService.generate()

    /** Send a chat message through the connected client. */
    fun sendMessage(text: String) {
        if (shuttingDown.get()) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            publish(ComposeConnectionEventKind.WARNING, "Message is empty; nothing sent.")
            return
        }
        if (!chatClientService.isConnected()) {
            publish(ComposeConnectionEventKind.WARNING, "Connect to a room before sending a chat message.")
            return
        }

        try {
            chatClientService.sendMessage(trimmed)
        } catch (e: Exception) {
            val message = "Chat send failed: ${e.message ?: "unknown error"}"
            appendChatTranscript("[error] $message")
            publish(ComposeConnectionEventKind.ERROR, message)
        }
    }

    /** Record an incoming receive prompt and return the deterministic Compose acceptance decision. */
    fun acceptIncomingFileTransfer(
        metadata: FileTransferMetadata,
        remoteAddress: String,
        autoAccept: Boolean = false,
    ): Boolean {
        if (shuttingDown.get()) return false

        var immediateDecision: Boolean? = null
        var pendingDecision: CompletableFuture<Boolean>? = null
        var pendingPromptId: String? = null
        runUiStateUpdateAndWait {
            if (!chatClientService.isConnected()) {
                val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.REJECTED)
                incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
                appendChatTranscript(DesktopTransferFormatters.fileRejectedDisconnectedMessage(metadata.fileName, metadata.senderId))
                SecureLanLogger.logTransfer("Incoming file rejected because chat is not connected: ${metadata.fileName}.")
                immediateDecision = false
                return@runUiStateUpdateAndWait
            }
            val knownPeer = findPeerForIncomingFile(metadata, remoteAddress) != null
            if (!knownPeer) {
                val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.REJECTED)
                incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
                appendChatTranscript(DesktopTransferFormatters.fileRejectedUnknownPeerMessage(metadata.fileName, metadata.senderId))
                SecureLanLogger.logTransfer("Incoming file rejected from unknown/offline peer ${metadata.senderId}.")
                immediateDecision = false
                return@runUiStateUpdateAndWait
            }
            if (autoAccept || autoAcceptIncomingFiles) {
                val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.AUTO_ACCEPTED)
                incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
                appendChatTranscript(DesktopTransferFormatters.fileAutoAcceptedMessage(metadata.fileName, metadata.senderId))
                SecureLanLogger.logTransfer("Incoming file auto-accepted: ${metadata.fileName} from ${metadata.senderId}.")
                immediateDecision = true
                return@runUiStateUpdateAndWait
            }

            val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.WAITING)
            incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
            playTransferNotificationSound(completion = false)
            SecureLanLogger.logTransfer("Incoming file prompt waiting for user decision: ${prompt.header}.")
            val decision = CompletableFuture<Boolean>()
            pendingIncomingTransferDecisions[prompt.id] = decision
            pendingPromptId = prompt.id
            pendingDecision = decision
        }
        immediateDecision?.let { return it }
        val decision = pendingDecision ?: return false
        if (SwingUtilities.isEventDispatchThread()) {
            SecureLanLogger.logTransfer("Incoming file prompt rejected because waiting on the UI thread would freeze the Compose event loop.")
            pendingPromptId?.let { pendingIncomingTransferDecisions.remove(it) }
            return false
        }
        return try {
            decision.get()
        } catch (e: Exception) {
            dispatchUiStateUpdate { SecureLanLogger.logTransfer(DesktopTransferFormatters.fileConfirmationFailedDiagnostics(e.message)) }
            false
        } finally {
            pendingPromptId?.let { pendingIncomingTransferDecisions.remove(it) }
        }
    }

    /** Record an explicit Compose receive-prompt decision. */
    fun recordIncomingFileDecision(promptId: String, accepted: Boolean) {
        val prompt = incomingTransferPrompts.firstOrNull { it.id == promptId } ?: return
        pendingIncomingTransferDecisions[promptId]?.complete(accepted)
        incomingTransferPrompts = incomingTransferPrompts.map {
            if (it.id == promptId) {
                it.withStatus(
                    if (accepted) {
                        ComposeIncomingTransferPromptStatus.ACCEPTED
                    } else {
                        ComposeIncomingTransferPromptStatus.REJECTED
                    },
                )
            } else {
                it
            }
        }
        appendChatTranscript(DesktopTransferFormatters.fileConfirmationResultMessage(accepted, prompt.fileName, prompt.senderId))
        SecureLanLogger.logTransfer("Incoming file ${if (accepted) "accepted" else "rejected"}: ${prompt.fileName} from ${prompt.senderId}.")
    }

    private fun upsertIncomingTransferPrompt(prompt: ComposeIncomingTransferPrompt): List<ComposeIncomingTransferPrompt> =
        (incomingTransferPrompts.filterNot { it.id == prompt.id } + prompt).takeLast(20)

    private fun findPeerForIncomingFile(metadata: FileTransferMetadata, remoteAddress: String): PeerPresence? {
        val remoteHost = remoteHostOnly(remoteAddress)
        return visiblePeerItems.firstOrNull { peer ->
            peer.online() &&
                (
                    peer.nickname().equals(metadata.senderId, ignoreCase = true) ||
                        peer.peerId() == metadata.senderId ||
                        (!peer.host().isNullOrBlank() && peer.host() == remoteHost)
                    )
        }
    }

    private fun remoteHostOnly(remoteAddress: String): String? =
        runCatching {
            val socketAddress = InetSocketAddress.createUnresolved(remoteAddress.substringBeforeLast(':'), 0)
            socketAddress.hostString
                .removePrefix("/")
                .takeIf { it.isNotBlank() && it != remoteAddress }
        }.getOrNull()
            ?: remoteAddress
                .removePrefix("/")
                .substringBeforeLast(':')
                .takeIf { it.isNotBlank() && it != remoteAddress }

    /** Apply the auto-accept setting to the live file-transfer listener callback. */
    fun updateAutoAcceptIncomingFiles(enabled: Boolean) {
        if (autoAcceptIncomingFiles == enabled) {
            return
        }
        autoAcceptIncomingFiles = enabled
        settingsController.update { settings ->
            settings.copy(
                transfers = settings.transfers.copy(
                    incomingFileConfirmation = if (enabled) {
                        IncomingFileConfirmationMode.AUTO_ACCEPT_KNOWN_PEERS
                    } else {
                        IncomingFileConfirmationMode.ASK
                    },
                ),
            )
        }
    }

    fun updateLastConnectionMode(mode: DesktopConnectionMode) {
        settingsController.update { settings ->
            settings.copy(network = settings.network.copy(lastConnectionMode = mode))
        }
    }

    fun updateDownloadsDirectory(path: Path) {
        downloadsPath = path.toAbsolutePath().normalize()
        settingsController.update { settings -> settings.copy(downloadsDirectory = downloadsPath.toString()) }
    }

    fun updatePreferredNickname(nickname: String) {
        val trimmed = nickname.trim()
        settingsController.update { settings -> settings.copy(displayName = trimmed.takeIf(String::isNotEmpty)) }
        if (!statusState.localServerRunning && !statusState.clientConnected && trimmed.isNotEmpty()) {
            statusState = statusState.copy(nickname = trimmed)
            refreshRealtimeState()
        }
    }

    fun updateNetworkDefaults(network: DesktopNetworkSettings) {
        val normalized = network.normalized()
        settingsController.update { settings -> settings.copy(network = normalized) }
        if (!statusState.localServerRunning && !statusState.clientConnected) {
            statusState = statusState.copy(
                discoverable = normalized.discoverable,
                serverChatPortText = normalized.serverChatPort.toString(),
                serverFilePortText = normalized.serverFilePort.toString(),
                clientChatPortText = normalized.clientChatPort.toString(),
                clientFilePortText = normalized.clientFilePort.toString(),
                manualHost = normalized.recentRooms.firstOrNull()?.host ?: statusState.manualHost,
            )
            refreshRealtimeState()
        }
    }

    fun sendFileToPeer(filePath: Path, senderId: String, recipient: DiscoveredPeer, sessionPassword: String): CompletableFuture<String?> {
        if (shuttingDown.get()) return CompletableFuture.completedFuture(null)
        val completion = CompletableFuture<String?>()
        val client = fileTransferClientService
        if (client == null) {
            SecureLanLogger.logTransfer("Outgoing file send unavailable: file-transfer client service is not configured.")
            completion.complete(null)
            return completion
        }
        if (!chatClientService.isConnected()) {
            SecureLanLogger.logTransfer("Outgoing file send blocked: connect to chat before sending files.")
            completion.complete(null)
            return completion
        }

        SecureLanLogger.logTransfer("Outgoing file send queued for ${recipient.nickname}; file checks and transfer will run on the IO dispatcher.")
        fileTransferIoScope.launch {
            val normalizedFile = filePath.toAbsolutePath().normalize()
            if (!Files.isRegularFile(normalizedFile)) {
                dispatchUiStateUpdate { SecureLanLogger.logTransfer("Outgoing file send blocked: file does not exist: $normalizedFile.") }
                completion.complete(null)
                return@launch
            }
            try {
                val transferId = client.sendFile(
                    FileTransferClientRequest(
                        recipient.host,
                        recipient.filePort,
                        senderId.trim(),
                        recipient.nickname,
                        sessionPassword,
                        normalizedFile,
                    ),
                )
                dispatchUiStateUpdate { SecureLanLogger.logTransfer("Outgoing file send finished: ${normalizedFile.fileName} to ${recipient.nickname}.") }
                completion.complete(transferId)
            } catch (e: Exception) {
                val message = "Outgoing file send failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
                dispatchUiStateUpdate {
                    SecureLanLogger.logTransfer(message)
                    publish(ComposeConnectionEventKind.ERROR, message)
                }
                completion.complete(null)
            }
        }
        return completion
    }

    fun startQuickShare(port: Int) {
        if (shuttingDown.get()) return
        try {
            quickShareService.start(QuickShareServerConfig(port))
            quickShareError = null
            refreshQuickShareState()
            appendChatTranscript(DesktopQuickShareFormatters.formatServerStartedMessage())
            SecureLanLogger.logQuickShare(DesktopQuickShareFormatters.formatLandingUrlsDiagnostics(quickShareService.landingUrls()))
        } catch (e: Exception) {
            val message = "Quick-share start failed: ${e.message ?: "unknown error"}"
            quickShareError = message
            publish(ComposeConnectionEventKind.ERROR, message)
            SecureLanLogger.logQuickShare(message)
        }
    }

    fun stopQuickShare() {
        if (shuttingDown.get()) return
        quickShareService.stop()
        refreshQuickShareState()
        appendChatTranscript(DesktopQuickShareFormatters.formatServerStoppedMessage())
        SecureLanLogger.logQuickShare("Quick-share server stopped from Compose.")
    }

    fun createTextQuickShare(text: String, expirationMinutes: Long?, accessLimit: Int?) {
        if (shuttingDown.get()) return
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            SecureLanLogger.logQuickShare("Text quick-share rejected: enter text to share first.")
            return
        }
        if ((expirationMinutes != null && expirationMinutes < 1) || (accessLimit != null && accessLimit < 1)) {
            SecureLanLogger.logQuickShare("Text quick-share rejected: expiration and access limit must be at least 1.")
            return
        }
        try {
            if (!quickShareService.isRunning()) {
                quickShareService.start(QuickShareServerConfig())
            }
            val snapshot = quickShareService.share(
                QuickShareCreateRequest.text(
                    normalizedText,
                    DesktopQuickShareFormatters.formatTextDisplayName(normalizedText),
                    expirationMinutes?.let(Duration::ofMinutes),
                    accessLimit,
                ),
            )
            refreshQuickShareState()
            val shareUrl = DesktopQuickShareFormatters.preferQuickShareUrl(snapshot)
            appendChatTranscript(DesktopQuickShareFormatters.formatTextLinkCopiedMessage(shareUrl))
            SecureLanLogger.logQuickShare("Text quick-share created: ${snapshot.displayName()}.")
        } catch (e: Exception) {
            val message = "Text quick-share failed: ${e.message ?: "unknown error"}"
            publish(ComposeConnectionEventKind.ERROR, message)
            SecureLanLogger.logQuickShare(message)
        }
    }

    fun stopQuickShareEntry(id: String) {
        if (shuttingDown.get()) return
        if (quickShareService.stopShare(id)) {
            refreshQuickShareState()
            SecureLanLogger.logQuickShare("Quick-share stopped: $id.")
        }
    }

    fun createFileQuickShare(filePath: Path, expirationMinutes: Long?, accessLimit: Int?) {
        if (shuttingDown.get()) return
        val normalizedFile = filePath.toAbsolutePath().normalize()
        if ((expirationMinutes != null && expirationMinutes < 1) || (accessLimit != null && accessLimit < 1)) {
            SecureLanLogger.logQuickShare("File quick-share rejected: expiration and access limit must be at least 1.")
            return
        }
        try {
            if (!quickShareService.isRunning()) {
                quickShareService.start(QuickShareServerConfig())
            }
            val snapshot = quickShareService.share(
                QuickShareCreateRequest.file(
                    normalizedFile,
                    normalizedFile.fileName.toString(),
                    expirationMinutes?.let(Duration::ofMinutes),
                    accessLimit,
                ),
            )
            refreshQuickShareState()
            val shareUrl = DesktopQuickShareFormatters.preferQuickShareUrl(snapshot)
            appendChatTranscript(DesktopQuickShareFormatters.formatFileLinkCopiedMessage(shareUrl))
            SecureLanLogger.logQuickShare("File quick-share created: ${snapshot.displayName()}.")
        } catch (e: Exception) {
            val message = "File quick-share failed: ${e.message ?: "unknown error"}"
            publish(ComposeConnectionEventKind.ERROR, message)
            SecureLanLogger.logQuickShare(message)
        }
    }

    fun inspectStegoCover(coverPath: Path): BmpCapacity? {
        if (shuttingDown.get()) return null
        return try {
            val normalized = coverPath.toAbsolutePath().normalize()
            val capacity = steganographyService.inspect(DesktopMainViewHelpers.readImageAsBmpBytes(normalized))
            stegoState = stegoState.copy(
                coverPathText = normalized.toString(),
                outputPathText = DesktopMainViewHelpers.suggestedStegoOutputPath(normalized).toString(),
                capacity = capacity,
                statusText = "Cover BMP ready: ${normalized.fileName}",
            )
            appendChatTranscript("[stego] inspected cover BMP: ${normalized.fileName}")
            capacity
        } catch (e: Exception) {
            val message = "Steganography inspect failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
            stegoState = stegoState.copy(statusText = message)
            publish(ComposeConnectionEventKind.ERROR, message)
            null
        }
    }

    fun hideStegoMessage(coverPath: Path, outputPath: Path, message: String, password: String? = null): Path? {
        if (shuttingDown.get()) return null
        val normalizedMessage = message.trim()
        if (normalizedMessage.isBlank()) {
            val status = "Steganography hide rejected: message is empty."
            stegoState = stegoState.copy(statusText = status)
            return null
        }
        val passwordChars = password?.toCharArray()
        return try {
            val cover = coverPath.toAbsolutePath().normalize()
            val output = DesktopMainViewHelpers.ensureBmpExtension(outputPath.toAbsolutePath().normalize())
            val bmpBytes = DesktopMainViewHelpers.readImageAsBmpBytes(cover)
            val hiddenBytes = if (passwordChars != null && passwordChars.isNotEmpty()) {
                steganographyService.hideEncryptedText(bmpBytes, normalizedMessage, passwordChars)
            } else {
                steganographyService.hideText(bmpBytes, normalizedMessage)
            }
            Files.write(output, hiddenBytes)
            val capacity = steganographyService.inspect(hiddenBytes)
            stegoState = stegoState.copy(
                coverPathText = cover.toString(),
                inputPathText = output.toString(),
                outputPathText = output.toString(),
                messageDraft = normalizedMessage,
                passwordDraft = password.orEmpty(),
                encryptPayload = !password.isNullOrEmpty(),
                capacity = capacity,
                statusText = "Hidden message saved to ${output.fileName}",
            )
            appendChatTranscript("[stego] hidden message saved: $output")
            output
        } catch (e: Exception) {
            val status = "Steganography hide failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
            stegoState = stegoState.copy(statusText = status)
            publish(ComposeConnectionEventKind.ERROR, status)
            null
        } finally {
            passwordChars?.fill('\u0000')
        }
    }

    fun extractStegoMessage(inputPath: Path, password: String? = null): String? {
        if (shuttingDown.get()) return null
        val passwordChars = password?.toCharArray()
        return try {
            val input = inputPath.toAbsolutePath().normalize()
            val bmpBytes = DesktopMainViewHelpers.readImageAsBmpBytes(input)
            val extracted = if (passwordChars != null && passwordChars.isNotEmpty()) {
                steganographyService.extractEncryptedText(bmpBytes, passwordChars)
            } else {
                steganographyService.extractText(bmpBytes)
            }
            stegoState = stegoState.copy(
                inputPathText = input.toString(),
                passwordDraft = password.orEmpty(),
                encryptedExtract = !password.isNullOrEmpty(),
                extractedMessage = extracted,
                statusText = "Extracted message from ${input.fileName}",
            )
            appendChatTranscript("[stego] extracted message from ${input.fileName}")
            extracted
        } catch (e: Exception) {
            val status = "Steganography extract failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
            stegoState = stegoState.copy(statusText = status)
            publish(ComposeConnectionEventKind.ERROR, status)
            null
        } finally {
            passwordChars?.fill('\u0000')
        }
    }

    fun clearSteganographyState() {
        stegoState = ComposeSteganographyState()
    }

    fun refreshMediaDevices() {
        if (shuttingDown.get()) return
        val mediaService = rtcMediaDeviceService
        if (mediaService == null) {
            SecureLanLogger.logRealtime("RTC media device service is not configured.")
            refreshRealtimeState()
            return
        }
        val microphones = buildMediaChoices(mediaService.audioCaptureDevices(), "System default microphone")
        val outputDevices = buildMediaChoices(mediaService.audioRenderDevices(), "System default speaker")
        val cameras = buildMediaChoices(mediaService.videoCaptureDevices(), "System default camera")
        mediaVoiceState = mediaVoiceState.copy(
            microphones = microphones,
            outputDevices = outputDevices,
            microphoneTestStatus = "Microphones refreshed: ${microphones.size}",
            speakerTestStatus = "Speakers refreshed: ${outputDevices.size}",
        )
        experimentalVideoState = experimentalVideoState.copy(cameras = cameras, cameraTestStatus = "Cameras refreshed: ${cameras.size}")
        SecureLanLogger.logRealtime("Media devices refreshed: ${microphones.size} microphones, ${outputDevices.size} speakers, ${cameras.size} cameras.")
        refreshRealtimeState()
    }

    fun testMicrophone(deviceId: String? = mediaVoiceState.selectedMicrophone.deviceId): String {
        val mediaService = rtcMediaDeviceService
        val result = mediaService?.testAudioCaptureDevice(deviceId)
            ?: "Microphone test unavailable: RTC media device service is not configured."
        mediaVoiceState = mediaVoiceState.copy(selectedMicrophoneId = deviceId.orEmpty(), microphoneTestStatus = result)
        SecureLanLogger.logRealtime(result)
        return result
    }

    fun selectMicrophone(deviceId: String?) {
        mediaVoiceState = mediaVoiceState.copy(selectedMicrophoneId = deviceId.orEmpty())
        settingsController.update { settings ->
            settings.copy(media = settings.media.copy(microphoneDeviceId = deviceId.orEmpty()))
        }
        SecureLanLogger.logRealtime("Microphone selected: ${mediaVoiceState.selectedMicrophone}.")
    }

    fun testSpeaker(deviceId: String? = mediaVoiceState.selectedOutputDevice.deviceId): String {
        val mediaService = rtcMediaDeviceService
        val result = mediaService?.testAudioRenderDevice(deviceId)
            ?: "Speaker test unavailable: RTC media device service is not configured."
        mediaVoiceState = mediaVoiceState.copy(selectedOutputDeviceId = deviceId.orEmpty(), speakerTestStatus = result)
        SecureLanLogger.logRealtime(result)
        return result
    }

    fun selectSpeaker(deviceId: String?) {
        mediaVoiceState = mediaVoiceState.copy(selectedOutputDeviceId = deviceId.orEmpty())
        settingsController.update { settings ->
            settings.copy(media = settings.media.copy(outputDeviceId = deviceId.orEmpty()))
        }
        SecureLanLogger.logRealtime("Speaker output selected: ${mediaVoiceState.selectedOutputDevice}.")
    }

    fun testCamera(deviceId: String? = experimentalVideoState.selectedCamera.deviceId): String {
        val mediaService = rtcMediaDeviceService
        val result = mediaService?.testVideoCaptureDevice(deviceId)
            ?: "Camera test unavailable: RTC media device service is not configured."
        experimentalVideoState = experimentalVideoState.copy(selectedCameraId = deviceId.orEmpty(), cameraTestStatus = result)
        SecureLanLogger.logRealtime(result)
        return result
    }

    fun selectCamera(deviceId: String?) {
        experimentalVideoState = experimentalVideoState.copy(selectedCameraId = deviceId.orEmpty())
        settingsController.update { settings ->
            settings.copy(media = settings.media.copy(cameraDeviceId = deviceId.orEmpty()))
        }
        SecureLanLogger.logRealtime("Camera selected: ${experimentalVideoState.selectedCamera}.")
    }

    fun startRealtimeSession(
        localPeer: String,
        remotePeer: String,
        mode: RtcSessionMode,
        dataChannelLabel: String = "securelan-data",
        audioDeviceId: String? = mediaVoiceState.selectedMicrophone.deviceId,
        videoDeviceId: String? = experimentalVideoState.selectedCamera.deviceId,
    ) {
        if (shuttingDown.get()) return
        val service = rtcSessionService
        if (service == null) {
            SecureLanLogger.logRealtime("RTC session service is not configured.")
            return
        }
        if (!chatClientService.isConnected()) {
            SecureLanLogger.logRealtime("RTC session blocked: connect to chat before starting realtime.")
            return
        }
        try {
            val snapshot = service.startSession(
                RtcSessionRequest(
                    localPeer.trim(),
                    remotePeer.trim(),
                    mode,
                    dataChannelLabel,
                    audioDeviceId.orEmpty(),
                    videoDeviceId.orEmpty(),
                ),
            )
            SecureLanLogger.logRealtime(
                DesktopRealtimeFormatters.rtcStateDiagnostics(
                    mode,
                    snapshot.state ?: RtcSessionState.IDLE,
                    snapshot.remotePeer,
                    snapshot.message,
                ),
            )
        } catch (e: Exception) {
            val message = "RTC session failed: ${e.message ?: "unknown error"}"
            SecureLanLogger.logRealtime(message)
            publish(ComposeConnectionEventKind.ERROR, message)
        }
        refreshRealtimeState()
    }

    fun closeRealtimeSession() {
        if (shuttingDown.get()) return
        clearRealtimeMediaState()
        rtcSessionService?.closeCurrentSession()
        SecureLanLogger.logRealtime("RTC session close requested from Compose.")
        refreshRealtimeState()
    }

    fun startCameraPreview(deviceId: String? = experimentalVideoState.selectedCamera.deviceId) {
        if (shuttingDown.get()) return
        val mediaService = rtcMediaDeviceService
        if (mediaService == null) {
            val message = "Camera preview unavailable: RTC media device service is not configured."
            experimentalVideoState = experimentalVideoState.copy(
                previewRunning = false,
                latestPreviewFrame = null,
                cameraTestStatus = message,
            )
            SecureLanLogger.logRealtime(message)
            return
        }
        closeCameraPreview()
        experimentalVideoState = experimentalVideoState.copy(
            selectedCameraId = deviceId.orEmpty(),
            previewRunning = true,
            latestPreviewFrame = null,
            latestLocalVideoFrame = null,
            cameraTestStatus = "Starting camera preview…",
        )
        val session = try {
            mediaService.startVideoPreview(deviceId) { event ->
                experimentalVideoState = experimentalVideoState.copy(
                    latestPreviewFrame = event,
                    latestLocalVideoFrame = event.takeIf { it.local() } ?: experimentalVideoState.latestLocalVideoFrame,
                    previewRunning = true,
                )
                SecureLanLogger.logRealtime(DesktopRealtimeFormatters.cameraPreviewLiveStatus(event.width(), event.height()))
            }
        } catch (error: Throwable) {
            val message = "Camera preview failed: ${error::class.java.simpleName}: ${error.message.orEmpty()}"
            experimentalVideoState = experimentalVideoState.copy(
                previewRunning = false,
                latestPreviewFrame = null,
                cameraTestStatus = message,
            )
            SecureLanLogger.logRealtime(message)
            return
        }
        val status = session.statusMessage()
        if (status.startsWith("Camera preview failed", ignoreCase = true)) {
            runCatching { session.close() }
            experimentalVideoState = experimentalVideoState.copy(
                previewRunning = false,
                latestPreviewFrame = null,
                cameraTestStatus = status,
            )
            SecureLanLogger.logRealtime(status)
            return
        }
        cameraPreviewSession = session
        experimentalVideoState = experimentalVideoState.copy(
            selectedCameraId = deviceId.orEmpty(),
            previewRunning = true,
            cameraTestStatus = status,
        )
        SecureLanLogger.logRealtime(status)
    }

    fun closeCameraPreview() {
        val session = cameraPreviewSession
        cameraPreviewSession = null
        try {
            session?.close()
        } catch (_: Exception) { /* best-effort */ }
        experimentalVideoState = experimentalVideoState.copy(
            previewRunning = false,
            latestPreviewFrame = if (experimentalVideoState.latestPreviewFrame?.local() == true) null else experimentalVideoState.latestPreviewFrame,
            latestLocalVideoFrame = null,
            cameraTestStatus = "Camera preview stopped.",
        )
    }

    // ---- lifecycle ----

    /** Shut down the adapter and all underlying services. */
    fun shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) return
        publishEventKind(ComposeAdapterEventKind.CLEANUP_STARTED, "Adapter shutdown initiated.")
        pendingIncomingTransferDecisions.values.forEach { it.complete(false) }
        pendingIncomingTransferDecisions.clear()
        fileTransferIoScope.cancel()

        try {
            if (chatClientService.isConnected()) {
                chatClientService.disconnect()
            }
        } catch (_: Exception) { /* best-effort */ }

        try {
            if (fileTransferServerService.isRunning()) {
                fileTransferServerService.stop()
            }
        } catch (_: Exception) { /* best-effort */ }

        try {
            quickShareService.stop()
            refreshQuickShareState()
        } catch (_: Exception) { /* best-effort */ }

        try {
            closeCameraPreview()
        } catch (_: Exception) { /* best-effort */ }

        try {
            rtcSessionService?.close()
        } catch (_: Exception) { /* best-effort */ }

        try {
            rtcMediaDeviceService?.close()
        } catch (_: Exception) { /* best-effort */ }

        try {
            discoveryService.stop()
            currentConfig = null
            discoveredPeers = emptyList()
            SecureLanLogger.logConnection("Discovery stopped during adapter shutdown; peer snapshot cleared.")
        } catch (_: Exception) { /* best-effort */ }

        try {
            if (chatServerService.isRunning()) {
                chatServerService.stop()
            }
        } catch (_: Exception) { /* best-effort */ }

        publishEventKind(ComposeAdapterEventKind.CLEANUP_COMPLETED, "Adapter shutdown complete.")
    }

    override fun close() {
        shutdown()
    }

    // ---- internal helpers ----

    private fun refreshState() {
        val serverRunning = chatServerService.isRunning()
        val clientConnected = chatClientService.isConnected()
        val discoveryActive = discoveryService.isRunning()
        val config = currentConfig

        statusState = statusState.copy(
            localServerRunning = serverRunning,
            clientConnected = clientConnected,
            serverStatus = if (serverRunning) "Room running" else "Room closed",
            connectionStatus = if (clientConnected) "Connected" else "Connection idle",
            discoveryStatus = when {
                discoveryActive && config?.announceEnabled == true -> "Room visible nearby"
                discoveryActive -> "Room hidden nearby"
                else -> "Discovery not started"
            },
            discoverable = config?.announceEnabled ?: statusState.discoverable,
        )
        refreshRealtimeState()
    }

    private fun publish(kind: ComposeConnectionEventKind, message: String) {
        adapterEvents = adapterEvents + ComposeConnectionEvent(kind, message)
        when (kind) {
            ComposeConnectionEventKind.ERROR -> SecureLanLogger.logError(message)
            ComposeConnectionEventKind.WARNING -> SecureLanLogger.logWarning(message)
            else -> SecureLanLogger.logConnection(message)
        }
    }

    private fun publishMicrointeraction(kind: ComposeConnectionEventKind, message: String) {
        microinteractionEvents = (microinteractionEvents + ComposeConnectionEvent(kind, message)).takeLast(8)
    }

    private fun dispatchUiStateUpdate(action: () -> Unit) {
        if (shuttingDown.get()) return
        val customDispatcher = uiStateDispatcher
        if (customDispatcher != null) {
            customDispatcher(action)
            return
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeLater {
                if (!shuttingDown.get()) {
                    action()
                }
            }
        }
    }

    private fun runUiStateUpdateAndWait(action: () -> Unit) {
        val customDispatcher = uiStateDispatcher
        if (customDispatcher != null) {
            customDispatcher(action)
            return
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait {
                if (!shuttingDown.get()) {
                    action()
                }
            }
        }
    }

    private fun appendChatTranscript(line: String) {
        val normalized = normalizeTranscriptLine(line)
        if (normalized.isBlank()) {
            return
        }
        val timestamp = Instant.now()
        chatTranscript = (chatTranscript + normalized).takeLast(200)
        chatMessages = (chatMessages + ComposeChatMessage.fromTranscriptLine(normalized, timestamp)).takeLast(200)
        when {
            normalized.startsWith("[disconnected]", ignoreCase = true) -> publishMicrointeraction(ComposeConnectionEventKind.INFO, "Connection ended")
            normalized.contains("completed", ignoreCase = true) && (normalized.startsWith("[file-send]", ignoreCase = true) || normalized.startsWith("[file-recv]", ignoreCase = true)) -> publishMicrointeraction(ComposeConnectionEventKind.SUCCESS, "Transfer completed")
            normalized.startsWith("[error]", ignoreCase = true) || normalized.contains("failed", ignoreCase = true) -> publishMicrointeraction(ComposeConnectionEventKind.ERROR, normalized.removePrefix("[error]").trim())
        }
    }

    private fun formatChatMessage(sender: String, text: String): String {
        val normalizedSender = sender.trim().ifEmpty { "unknown" }
        val normalizedText = normalizeChatText(text, normalizedSender)
        return if (isSystemSender(normalizedSender)) {
            "[system] $normalizedText"
        } else {
            "$normalizedSender: $normalizedText"
        }
    }

    private fun normalizeTranscriptLine(line: String): String = line.trim()
        .replace(Regex("^system:\\s*\\[system]\\s*", RegexOption.IGNORE_CASE), "[system] ")
        .replace(Regex("^\\[system]\\s*system:\\s*", RegexOption.IGNORE_CASE), "[system] ")

    private fun disconnectedTranscriptLine(event: ChatDisconnectedEvent): String = when (event.reason) {
        ChatDisconnectReasons.REMOTE_HOST_CLOSED -> "[system] Room closed by host"
        ChatDisconnectReasons.CLIENT_REQUEST -> "[system] Disconnected from room"
        ChatDisconnectReasons.CONNECTION_LOST -> "[disconnected] Connection lost"
        else -> "[disconnected] ${event.reason ?: "Chat disconnected"}"
    }

    private fun normalizeChatText(text: String?, sender: String): String {
        var normalized = text?.trim().orEmpty()
        val normalizedSender = Regex.escape(sender.trim())
        if (normalizedSender.isNotEmpty()) {
            normalized = normalized.replace(Regex("^$normalizedSender:\\s*", RegexOption.IGNORE_CASE), "")
        }
        normalized = normalized
            .replace(Regex("^system:\\s*\\[system]\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\[system]\\s*system:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\[system]\\s*", RegexOption.IGNORE_CASE), "")
        return normalized
    }

    private fun publishLocalNetworkInfo() {
        localNetworkInfo = try {
            DesktopMainViewHelpers.localNetworkInfoMessage(DesktopMainViewHelpers.resolveLocalLanIps())
        } catch (e: Exception) {
            DesktopMainViewHelpers.localNetworkInfoErrorMessage(e.message)
        }
        SecureLanLogger.logConnection(localNetworkInfo)
    }

    private fun startFileTransferListener(filePort: Int, password: String) {
        if (fileTransferServerService.isRunning()) {
            return
        }
        fileTransferServerService.start(
            FileTransferServerConfig(
                filePort,
                downloadsPath,
                password,
                this::acceptIncomingFileTransfer,
            ),
        )
        publish(ComposeConnectionEventKind.SUCCESS, "File transfer listener started on port $filePort.")
        SecureLanLogger.logTransfer("File transfer listener started on port $filePort.")
    }

    private fun handleFileTransferEvent(event: FileTransferEvent) {
        when (event) {
            is FileTransferStartedEvent -> {
                val entry = TransferEntry(
                    event.transferId ?: "transfer-${transferEntryMap.size + 1}",
                    event.fileName ?: "unknown file",
                    event.outgoing,
                    if (event.outgoing) "Sending" else "Receiving",
                    0,
                    event.totalBytes,
                )
                transferEntryMap[entry.transferId] = entry
                appendChatTranscript((if (event.outgoing) "[file-send] " else "[file-recv] ") + "started: ${entry.fileName}")
                SecureLanLogger.logTransfer("Transfer started: ${entry.fileName}.")
            }

            is FileTransferProgressEvent -> {
                val transferId = event.transferId ?: return
                val existing = transferEntryMap[transferId] ?: return
                val progress = event.progress ?: return
                if (existing.active()) {
                    existing.status = if (event.outgoing) "Sending" else "Receiving"
                    existing.updateProgress(progress.transferredBytes, progress.percent(), progress.totalBytes)
                    SecureLanLogger.logTransfer("Transfer progress: ${existing.fileName} ${existing.percent}%.")
                }
            }

            is FileTransferCompletedEvent -> {
                val transferId = event.transferId ?: "transfer-${transferEntryMap.size + 1}"
                val entry = transferEntryMap.getOrPut(transferId) {
                    TransferEntry(transferId, event.fileName ?: "unknown file", event.outgoing, "Completed", 100, event.totalBytes)
                }
                entry.status = "Completed"
                entry.percent = 100
                entry.totalBytes = event.totalBytes
                entry.stopSpeedTracking()
                appendChatTranscript((if (event.outgoing) "[file-send] " else "[file-recv] ") + "completed: ${event.path}")
                SecureLanLogger.logTransfer("Transfer completed: ${entry.fileName}.")
                if (settingsController.settings.transfers.notifyOnCompletion) {
                    publishMicrointeraction(ComposeConnectionEventKind.SUCCESS, "Transfer completed: ${entry.fileName}")
                    playTransferNotificationSound(completion = true)
                }
            }

            is FileTransferFailedEvent -> {
                val transferId = event.transferId ?: "transfer-${transferEntryMap.size + 1}"
                val entry = transferEntryMap.getOrPut(transferId) {
                    TransferEntry(transferId, event.fileName ?: "unknown file", event.outgoing, "Failed", 0, 0)
                }
                entry.status = "Failed"
                entry.stopSpeedTracking()
                val message = event.message?.takeIf { it.isNotBlank() } ?: "unknown error"
                appendChatTranscript((if (event.outgoing) "[file-send] " else "[file-recv] ") + "failed: $message")
                SecureLanLogger.logTransfer("Transfer failed: ${entry.fileName}: $message.")
            }
        }
        transferEntries = ArrayList(transferEntryMap.values)
    }

    private fun playTransferNotificationSound(completion: Boolean) {
        val settings = settingsController.settings
        val notificationEnabled = settings.notifications.enabled &&
            settings.notifications.transferNotificationsEnabled &&
            settings.notifications.soundsEnabled &&
            (!completion || settings.transfers.notifyOnCompletion)
        if (notificationEnabled) {
            DesktopNotificationSound.play(settings.media.volumePercent)
        }
    }

    private fun handleQuickShareEvent(event: QuickShareEvent) {
        if (event.message().isNotBlank()) {
            SecureLanLogger.logQuickShare(
                DesktopQuickShareFormatters.formatEventDiagnostics(event.message(), event.remoteAddress()),
            )
        }
        refreshQuickShareState()
    }

    private fun refreshQuickShareState() {
        quickShareEntries = quickShareService.shares().map(::QuickShareEntry)
        quickShareRunning = quickShareService.isRunning()
        quickShareLandingUrls = quickShareService.landingUrls()
        if (quickShareRunning) {
            quickShareStatus = DesktopQuickShareFormatters.formatServerStatus()
            quickShareLanding = DesktopQuickShareFormatters.formatLandingValue(quickShareLandingUrls)
        } else {
            quickShareStatus = "Quick share idle"
            quickShareLanding = "Start quick share to get LAN browser links."
        }
    }

    private fun refreshRealtimeState() {
        val peerState = ComposePeerListState(peers = visiblePeerItems.map { ComposePeerListItem.fromPeer(it, chatClientService.isConnected()) }, selectedPeerIndex = if (visiblePeerItems.isEmpty()) -1 else 0)
        val runtimeStatus = rtcSessionService?.runtimeStatus() ?: RtcRuntimeStatus.unavailable("Voice and video service is not configured.")
        val currentSession = rtcSessionService?.currentSession()?.orElse(null)?.takeUnless { it.isTerminal() }
        mediaVoiceState = mediaVoiceState.copy(statusState = statusState, peerListState = peerState, runtimeStatus = runtimeStatus, currentSession = currentSession)
        experimentalVideoState = experimentalVideoState.copy(statusState = statusState, peerListState = peerState, runtimeStatus = runtimeStatus, currentSession = currentSession)
    }

    private fun clearRealtimeMediaState() {
        closeCameraPreview()
        mediaVoiceState = mediaVoiceState.copy(localAudioLevel = 0.0, remoteAudioLevel = 0.0, currentSession = null)
        experimentalVideoState = experimentalVideoState.copy(
            latestPreviewFrame = null,
            latestLocalVideoFrame = null,
            latestRemoteVideoFrame = null,
            currentSession = null,
        )
    }

    private fun RtcSessionSnapshot.isTerminal(): Boolean = when (state) {
        RtcSessionState.CLOSED,
        RtcSessionState.FAILED,
        RtcSessionState.UNAVAILABLE,
        -> true
        else -> false
    }

    private fun handleRtcEvent(event: RtcEvent) {
        when (event) {
            is RtcStateChangedEvent -> {
                SecureLanLogger.logRealtime(
                    DesktopRealtimeFormatters.rtcStateDiagnostics(
                        event.mode ?: RtcSessionMode.DATA,
                        event.state ?: RtcSessionState.IDLE,
                        event.remotePeer,
                        event.message,
                    ),
                )
                when (event.state ?: RtcSessionState.IDLE) {
                    RtcSessionState.CONNECTED -> publishMicrointeraction(ComposeConnectionEventKind.SUCCESS, "Call connected")
                    RtcSessionState.CLOSED -> {
                        clearRealtimeMediaState()
                        publishMicrointeraction(ComposeConnectionEventKind.INFO, "Call disconnected")
                    }
                    RtcSessionState.FAILED,
                    RtcSessionState.UNAVAILABLE,
                    -> {
                        clearRealtimeMediaState()
                        publishMicrointeraction(ComposeConnectionEventKind.ERROR, event.message ?: "Call failed")
                    }
                    else -> Unit
                }
            }
            is RtcRuntimeWarningEvent -> {
                SecureLanLogger.logRealtime(DesktopRealtimeFormatters.rtcWarningDiagnostics(event.message.orEmpty()))
            }
            is RtcAudioLevelEvent -> {
                mediaVoiceState = if (event.local) {
                    mediaVoiceState.copy(localAudioLevel = event.level)
                } else {
                    mediaVoiceState.copy(remoteAudioLevel = event.level)
                }
            }
            is RtcVideoFrameEvent -> {
                experimentalVideoState = if (event.local()) {
                    experimentalVideoState.copy(latestPreviewFrame = event, latestLocalVideoFrame = event)
                } else {
                    experimentalVideoState.copy(latestPreviewFrame = event, latestRemoteVideoFrame = event)
                }
            }
            else -> {
                SecureLanLogger.logRealtime("RTC event: ${event.javaClass.simpleName}.")
            }
        }
        refreshRealtimeState()
    }

    private fun buildMediaChoices(devices: List<com.shterneregen.securelan.webrtc.service.RtcMediaDevice>, defaultLabel: String): List<MediaDeviceChoice> =
        buildList {
            add(MediaDeviceChoice.systemDefault(defaultLabel))
            devices.mapTo(this, MediaDeviceChoice::of)
        }

    private fun connectLocalHostedChat(chatPort: Int, nickname: String, password: String) {
        if (chatClientService.isConnected()) {
            return
        }
        val connected = chatClientService.connect(
            ChatClientConnectRequest(
                "127.0.0.1",
                chatPort,
                nickname,
                password,
                desktopCapabilities(statusState.serverFilePort ?: NetworkConstants.DEFAULT_FILE_TRANSFER_PORT),
            ),
        )
        if (connected) {
            publish(ComposeConnectionEventKind.SUCCESS, "Joined local hosted room as $nickname.")
            publishEventKind(ComposeAdapterEventKind.CONNECTED, "Connected locally: $nickname")
        } else {
            publish(ComposeConnectionEventKind.ERROR, "Local hosting connection failed.")
            publishEventKind(ComposeAdapterEventKind.CONNECT_FAILED, "Local hosting connection failed.")
        }
    }

    private fun startPeerDiscoveryListener() {
        if (chatServerService.isRunning()) {
            val chatPort = statusState.serverChatPort ?: NetworkConstants.DEFAULT_CHAT_PORT
            val filePort = statusState.serverFilePort ?: NetworkConstants.DEFAULT_FILE_TRANSFER_PORT
            startPeerDiscovery(PeerDiscoveryConfig.defaults(localPeerId, statusState.nickname.trim(), chatPort, filePort, statusState.discoverable), hosting = true)
        } else {
            startPeerDiscovery(PeerDiscoveryConfig.listenOnly(localPeerId, statusState.nickname.trim()), hosting = false)
        }
    }

    private fun startPeerDiscoveryListenOnly(nickname: String) {
        val trimmedNick = nickname.trim().ifBlank { statusState.nickname.trim() }
        if (trimmedNick.isBlank()) return
        startPeerDiscovery(PeerDiscoveryConfig.listenOnly(localPeerId, trimmedNick), hosting = false)
    }

    private fun startPeerDiscovery(discoveryConfig: PeerDiscoveryConfig, hosting: Boolean) {
        if (discoveryConfig.nickname.isBlank()) return
        if (discoveryService.isRunning() && DesktopMainViewHelpers.canReuseDiscoverySession(currentConfig, discoveryConfig)) {
            currentConfig = discoveryConfig
            discoveredPeers = discoveryService.snapshot()
            SecureLanLogger.logConnection(
                "Discovery listener reused on port ${discoveryConfig.discoveryPort}; snapshot=${discoveredPeers.size}.",
            )
            return
        }
        currentConfig = discoveryConfig
        discoveryService.start(discoveryConfig, discoveryListener)
        discoveredPeers = discoveryService.snapshot()
        if (discoveryService.isRunning()) {
            val message = if (hosting) {
                DesktopMainViewHelpers.discoveryStartedMessage(discoveryConfig)
            } else {
                DesktopMainViewHelpers.discoveryListeningMessage(discoveryConfig.discoveryPort)
            }
            publish(ComposeConnectionEventKind.INFO, message)
            SecureLanLogger.logConnection("$message; snapshot=${discoveredPeers.size}.")
        }
    }

    private fun upsertJoinedPeer(event: ChatUserJoinedEvent): PeerPresence? {
        val nickname = event.nickname ?: return null
        if (chatServerService.isRunning() && !event.remoteAddress.isNullOrBlank()) {
            val host = DesktopMainViewHelpers.hostFromRemoteAddress(event.remoteAddress)
            if (host.isNotBlank()) {
                return upsertChatPeer(
                    nickname = nickname,
                    online = true,
                    peerId = null,
                    host = host,
                    chatPort = statusState.serverChatPort ?: NetworkConstants.DEFAULT_CHAT_PORT,
                    filePort = filePortFromCapabilities(event.capabilities, inferredClientFilePort()),
                    lastSeen = Instant.now(),
                    capabilities = event.capabilities,
                )
            }
        }
        return upsertChatPeer(nickname, online = true, filePort = filePortFromCapabilities(event.capabilities, 0), capabilities = event.capabilities)
    }

    private fun desktopCapabilities(filePort: Int): PeerCapabilities = PeerCapabilities.desktop(APP_VERSION, filePort)

    private fun filePortFromCapabilities(capabilities: PeerCapabilities, fallback: Int): Int =
        if (capabilities.supportsFileReceive() && capabilities.fileReceivePort() > 0) capabilities.fileReceivePort() else fallback

    private fun updatePeerPresenceFromSystemMessage(text: String): Boolean {
        val match = PRESENCE_MESSAGE_REGEX.matchEntire(text.trim()) ?: return false
        val nickname = match.groupValues[1].trim()
        when (match.groupValues[2].lowercase()) {
            "joined" -> upsertChatPeer(nickname, online = true)
            "left" -> markChatPeerOffline(nickname)
        }
        return true
    }

    private fun upsertChatPeer(
        nickname: String?,
        online: Boolean,
        peerId: String? = null,
        host: String? = null,
        chatPort: Int = 0,
        filePort: Int = 0,
        lastSeen: Instant? = null,
        capabilities: PeerCapabilities = PeerCapabilities.unknown(),
    ): PeerPresence? {
        if (nickname.isNullOrBlank() || isSystemSender(nickname) || isLocalNickname(nickname)) {
            return null
        }
        val existing = chatPeers.firstOrNull { DesktopMainViewHelpers.samePeer(it, nickname, peerId) }
        if (existing != null) {
            val changed = existing.apply(online, peerId, host, chatPort, filePort, lastSeen, capabilities)
            if (!changed) {
                return existing
            }
            // Replace the mutated peer with a fresh instance so Compose observes the change.
            val updated = PeerPresence(
                existing.nickname(),
                existing.online(),
                existing.peerId(),
                existing.host(),
                existing.chatPort(),
                existing.filePort(),
                existing.lastSeen(),
                existing.capabilities(),
            )
            chatPeers = chatPeers.map { if (it === existing) updated else it }.sortedWith(
                Comparator.comparing(PeerPresence::online).reversed()
                    .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
            )
            return updated
        }
        val created = PeerPresence(nickname, online, peerId, host, chatPort, filePort, lastSeen, capabilities)
        chatPeers = (chatPeers + created).sortedWith(
            Comparator.comparing(PeerPresence::online).reversed()
                .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
        )
        return created
    }

    private fun markChatPeerOffline(nickname: String?): Boolean {
        if (nickname.isNullOrBlank() || isSystemSender(nickname) || isLocalNickname(nickname)) {
            return false
        }
        val peer = chatPeers.firstOrNull { it.nickname().equals(nickname, ignoreCase = true) }
        if (peer != null) {
            if (!peer.online()) {
                return false
            }
            // PeerPresence is a mutable class; mutating the same instance and re-assigning the list
            // does not notify Compose because the new list is structurally equal to the old one.
            // Replace the peer with a new immutable copy so the state change is observed.
            val offlinePeer = PeerPresence(
                peer.nickname(),
                false,
                peer.peerId(),
                peer.host(),
                peer.chatPort(),
                peer.filePort(),
                peer.lastSeen(),
                peer.capabilities(),
            )
            chatPeers = chatPeers.map { if (it === peer) offlinePeer else it }.sortedWith(
                Comparator.comparing(PeerPresence::online).reversed()
                    .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
            )
            return true
        }
        val discovered = visiblePeers.firstOrNull { it.nickname.equals(nickname, ignoreCase = true) }
        val offlinePeer = if (discovered != null) {
            PeerPresence(discovered.nickname, false, discovered.peerId, discovered.host, discovered.chatPort, discovered.filePort, discovered.lastSeen)
        } else {
            PeerPresence(nickname, false, null, null, 0, 0, null)
        }
        chatPeers = (chatPeers + offlinePeer).sortedWith(
            Comparator.comparing(PeerPresence::online).reversed()
                .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
        )
        return true
    }

    private fun clearChatPeers() {
        if (chatPeers.isNotEmpty()) {
            chatPeers = emptyList()
        }
    }

    private fun inferredClientFilePort(): Int = DesktopMainViewHelpers.resolveInferredClientFilePort(
        statusState.serverFilePortText.trim(),
        NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
        COMPOSE_CLIENT_FILE_PORT_OFFSET,
    )

    private fun isLocalPeer(peer: DiscoveredPeer): Boolean = peer.peerId == localPeerId || isLocalNickname(peer.nickname)

    private fun isLocalPeer(peer: PeerPresence): Boolean = peer.peerId() == localPeerId || isLocalNickname(peer.nickname())

    private fun isLocalNickname(nickname: String?): Boolean = !nickname.isNullOrBlank() && nickname.equals(statusState.nickname.trim(), ignoreCase = true)

    private fun isSystemSender(nickname: String?): Boolean = nickname != null && nickname.equals("system", ignoreCase = true)

    private fun isSystemPrefixedChatText(text: String?): Boolean {
        val trimmed = text?.trim().orEmpty()
        return trimmed.startsWith("[system]", ignoreCase = true) ||
            trimmed.startsWith("system:", ignoreCase = true)
    }

    private fun peerKey(peer: PeerPresence): String = peer.peerId()?.takeIf { it.isNotBlank() } ?: peerKeyForNickname(peer.nickname())

    private fun peerKeyForDiscovered(peer: DiscoveredPeer): String = peer.peerId.takeIf { it.isNotBlank() } ?: peerKeyForNickname(peer.nickname)

    private fun peerKeyForNickname(nickname: String): String = "nick:${nickname.lowercase()}"

    private fun publishEventKind(kind: ComposeAdapterEventKind, detail: String) {
        val prefix = kind.name.lowercase().replace('_', ' ')
        publish(
            ComposeConnectionEventKind.INFO,
            "[$prefix] $detail",
        )
    }

    private companion object {
        private const val APP_VERSION = "0.5.0"
        const val COMPOSE_CLIENT_FILE_PORT_OFFSET: Int = 1000
        private val PRESENCE_MESSAGE_REGEX = Regex(
            "^(?:\\[system]\\s*)?(?:system:\\s*)?(?:\\[(?:join|left)]\\s*)?(.+?)\\s+(joined|left)\\s+the\\s+chat[\\s.!?]*$",
            RegexOption.IGNORE_CASE,
        )
    }
}
