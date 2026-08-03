package com.shterneregen.securelan.androidclient

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.OpenableColumns
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shterneregen.securelan.chat.service.impl.DefaultRandomNicknameService
import com.shterneregen.securelan.androidclient.model.AppLogEntry
import com.shterneregen.securelan.androidclient.model.AppLanguage
import com.shterneregen.securelan.androidclient.model.ChatLine
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.FileSendProgress
import com.shterneregen.securelan.androidclient.model.IncomingFileProgress
import com.shterneregen.securelan.androidclient.model.MainUiState
import com.shterneregen.securelan.androidclient.model.NearbyPermissionState
import com.shterneregen.securelan.androidclient.model.PeerRole
import com.shterneregen.securelan.androidclient.model.SelectedFile
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.model.ThemeMode
import com.shterneregen.securelan.androidclient.model.TransferDirection
import com.shterneregen.securelan.androidclient.model.TransferRecord
import com.shterneregen.securelan.androidclient.model.TransferResult
import com.shterneregen.securelan.androidclient.network.PeerDiscoveryRepository
import com.shterneregen.securelan.androidclient.network.AndroidChatHostService
import com.shterneregen.securelan.androidclient.network.AndroidHostServiceState
import com.shterneregen.securelan.androidclient.network.SecureChatClient
import com.shterneregen.securelan.androidclient.network.SecureFileReceiver
import com.shterneregen.securelan.androidclient.network.SecureFileSender
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val randomNicknameService = DefaultRandomNicknameService()
    private val discoveryRepository = PeerDiscoveryRepository()
    private val chatClient = SecureChatClient()
    private val fileSender = SecureFileSender(application.contentResolver)
    private val fileReceiver = SecureFileReceiver(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            nickname = preferences.getString(KEY_NICKNAME, null)?.takeIf { it.isNotBlank() }
                ?: randomNicknameService.generate(),
            manualHost = preferences.getString(KEY_MANUAL_HOST, "").orEmpty(),
            manualChatPort = preferences.getInt(KEY_MANUAL_CHAT_PORT, SecureLanPorts.DEFAULT_CHAT_PORT).toString(),
            manualFilePort = preferences.getInt(KEY_MANUAL_FILE_PORT, SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT).toString(),
            themeMode = preferences.getString(KEY_THEME_MODE, null)
                ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
                ?: ThemeMode.SYSTEM,
            appLanguage = preferences.getString(KEY_APP_LANGUAGE, null)
                ?.let { saved -> AppLanguage.entries.firstOrNull { it.name == saved } }
                ?: AppLanguage.SYSTEM,
            notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS, true),
            autoReceiveFiles = preferences.getBoolean(KEY_AUTO_RECEIVE, true),
        ),
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var discoveryJob: Job? = null
    private var discoveryTimeoutJob: Job? = null
    private var receiveJob: Job? = null
    private var fileReceiverJob: Job? = null
    private var disconnectRequested: Boolean = false
    private var hostConnectionAttemptedForPeerId: String? = null
    private val hostPeerId: String = preferences.getString(KEY_HOST_PEER_ID, null)
        ?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString().also { generated ->
            preferences.edit { putString(KEY_HOST_PEER_ID, generated) }
        }

    init {
        createNotificationChannel()
        observeHostService()
    }

    fun updateNickname(value: String) {
        val sanitized = value.filterNot(Char::isWhitespace)
        _uiState.update { it.copy(nickname = sanitized) }
        preferences.edit { putString(KEY_NICKNAME, sanitized) }
    }

    fun updateSessionPassword(value: String) = _uiState.update {
        it.copy(sessionPassword = value.filterNot(Char::isWhitespace))
    }

    fun updateThemeMode(value: ThemeMode) {
        _uiState.update { it.copy(themeMode = value) }
        preferences.edit { putString(KEY_THEME_MODE, value.name) }
    }

    fun updateAppLanguage(value: AppLanguage) {
        _uiState.update { it.copy(appLanguage = value) }
        preferences.edit { putString(KEY_APP_LANGUAGE, value.name) }
        createNotificationChannel()
    }

    fun updateDarkThemeEnabled(value: Boolean) = updateThemeMode(if (value) ThemeMode.DARK else ThemeMode.LIGHT)

    fun updateNotificationsEnabled(value: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = value) }
        preferences.edit { putBoolean(KEY_NOTIFICATIONS, value) }
    }

    fun updateAutoReceiveFiles(value: Boolean) {
        _uiState.update { it.copy(autoReceiveFiles = value) }
        preferences.edit { putBoolean(KEY_AUTO_RECEIVE, value) }
        if (value && _uiState.value.connected) {
            startFileReceiver()
        } else if (!value) {
            stopFileReceiver()
        }
    }

    fun updateNearbyPermission(state: NearbyPermissionState) {
        _uiState.update { it.copy(nearbyPermissionState = state) }
        if (state == NearbyPermissionState.GRANTED || state == NearbyPermissionState.NOT_REQUIRED) {
            startDiscovery()
        }
    }

    fun updateNetworkAvailable(available: Boolean) {
        _uiState.update { it.copy(networkAvailable = available) }
        if (!available && discoveryJob != null) {
            stopDiscovery()
        } else if (available && discoveryJob == null) {
            startDiscovery()
        }
    }

    fun updateInputMessage(value: String) = _uiState.update { it.copy(inputMessage = value) }

    fun selectPeer(peer: DiscoveredPeer) {
        _uiState.update {
            val connectionPeer = if (peer.role == PeerRole.SERVER) peer else it.connectionPeer
            it.copy(selectedPeer = peer, connectionPeer = connectionPeer, status = "Selected ${peer.nickname}")
        }
        addLog("Selected peer ${peer.nickname} at ${peer.host}:${peer.chatPort}")
    }

    fun startDiscovery() {
        if (discoveryJob != null) return
        if (_uiState.value.nearbyPermissionState == NearbyPermissionState.REQUIRED ||
            _uiState.value.nearbyPermissionState == NearbyPermissionState.DENIED
        ) return
        _uiState.update { it.copy(discoveryRunning = true, discoveryTimedOut = false, status = "Listening for SecureLan peers") }
        addLog("Started peer discovery")
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = viewModelScope.launch {
            delay(DISCOVERY_EMPTY_STATE_DELAY_MS)
            if (_uiState.value.peers.none { it.role == PeerRole.SERVER }) {
                _uiState.update { it.copy(discoveryTimedOut = true) }
            }
        }
        discoveryJob = viewModelScope.launch {
            discoveryRepository.discoverPeers().collect { peer ->
                if (peer.peerId == hostPeerId) return@collect
                _uiState.update { state ->
                    val serverPeer = peer.copy(role = PeerRole.SERVER)
                    val peers = upsertPeer(state.peers, serverPeer)
                        .sortedBy { it.nickname.lowercase() }
                    val selectedPeer = state.selectedPeer?.let { selected ->
                        if (samePeer(selected, serverPeer)) serverPeer else selected
                    }
                    val connectionPeer = state.connectionPeer?.let { current ->
                        if (samePeer(current, serverPeer)) serverPeer else current
                    } ?: serverPeer
                    state.copy(
                        peers = peers,
                        selectedPeer = selectedPeer,
                        connectionPeer = connectionPeer,
                        discoveryTimedOut = false,
                        status = "Found ${peers.size} peer(s)",
                    )
                }
            }
        }
    }

    fun connectManualPeer(hostValue: String, chatPortValue: String, filePortValue: String) {
        val host = hostValue.trim().removeSurrounding("[", "]")
        val chatPort = chatPortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        val filePort = filePortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        if (host.isBlank() || host.any { it.isWhitespace() } || "://" in host || '/' in host) {
            setError("Enter a valid IP address or host name")
            return
        }
        if (chatPort == null || filePort == null) {
            setError("Ports must be between 1 and 65535")
            return
        }
        val peer = DiscoveredPeer(
            peerId = "manual:$host:$chatPort",
            nickname = host,
            host = host,
            chatPort = chatPort,
            filePort = filePort,
            role = PeerRole.SERVER,
            fileTargetHost = host,
            fileTargetPort = filePort,
        )
        preferences.edit {
            putString(KEY_MANUAL_HOST, host)
            putInt(KEY_MANUAL_CHAT_PORT, chatPort)
            putInt(KEY_MANUAL_FILE_PORT, filePort)
        }
        _uiState.update { state ->
            state.copy(
                manualHost = host,
                manualChatPort = chatPort.toString(),
                manualFilePort = filePort.toString(),
                peers = upsertPeer(state.peers, peer).sortedBy { it.nickname.lowercase() },
                selectedPeer = peer,
                connectionPeer = peer,
                error = null,
                status = "Connecting to $host",
            )
        }
        addLog("Added manual desktop $host:$chatPort")
        connectSelectedPeer()
    }

    fun restartDiscovery() {
        stopDiscovery()
        startDiscovery()
    }

    fun startFileReceiver() {
        if (fileReceiverJob != null) return
        val state = _uiState.value
        val password = state.sessionPassword
        if (password.isBlank()) {
            setError("Session password must not be blank to receive files")
            return
        }
        val listenPort = localFileReceiverPort(state)
        _uiState.update { it.copy(fileReceiverRunning = true, status = "File receiver listening on port $listenPort") }
        addLog("File receiver listening on port $listenPort")
        fileReceiverJob = viewModelScope.launch {
            runCatching {
                fileReceiver.serve(
                    port = listenPort,
                    sessionPassword = password,
                    onStarted = { metadata ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = IncomingFileProgress(
                                    fileName = metadata.fileName,
                                    totalBytes = metadata.fileSize,
                                    active = true,
                                ),
                                status = "Receiving ${metadata.fileName}",
                            )
                        }
                        addLog("Receiving file ${metadata.fileName} (${metadata.fileSize} bytes) from ${metadata.senderId}")
                    },
                    onProgress = { metadata, received ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = it.incomingFileProgress.copy(
                                    fileName = metadata.fileName,
                                    bytesReceived = received,
                                    totalBytes = metadata.fileSize,
                                    active = true,
                                ),
                            )
                        }
                    },
                    onCompleted = { metadata, savedPath ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = it.incomingFileProgress.copy(
                                    fileName = metadata.fileName,
                                    bytesReceived = metadata.fileSize,
                                    totalBytes = metadata.fileSize,
                                    active = false,
                                    completedPath = savedPath,
                                    error = null,
                                ),
                                recentTransfers = listOf(
                                    TransferRecord(
                                        fileName = metadata.fileName,
                                        bytes = metadata.fileSize,
                                        direction = TransferDirection.RECEIVED,
                                        result = TransferResult.COMPLETED,
                                        peerName = metadata.senderId,
                                        savedPath = savedPath,
                                    ),
                                ) + it.recentTransfers.take(MAX_RECENT_TRANSFERS - 1),
                                status = "Received ${metadata.fileName}",
                            )
                        }
                        addLog("Received file ${metadata.fileName}; saved to $savedPath")
                        postFileReceivedNotification(metadata.fileName, savedPath)
                    },
                    onError = { message, _ ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = it.incomingFileProgress.copy(active = false, error = message),
                                recentTransfers = listOf(
                                    TransferRecord(
                                        fileName = it.incomingFileProgress.fileName.ifBlank { "Incoming file" },
                                        bytes = it.incomingFileProgress.totalBytes,
                                        direction = TransferDirection.RECEIVED,
                                        result = TransferResult.FAILED,
                                    ),
                                ) + it.recentTransfers.take(MAX_RECENT_TRANSFERS - 1),
                                status = message,
                            )
                        }
                        addLog(message, level = "ERROR")
                    },
                )
            }.onFailure { error ->
                fileReceiverJob = null
                if (error !is kotlinx.coroutines.CancellationException) {
                    _uiState.update {
                        it.copy(fileReceiverRunning = false, error = error.message, status = "File receiver stopped")
                    }
                    addLog(error.message ?: "File receiver stopped unexpectedly", level = "ERROR")
                }
            }
        }
    }

    fun stopFileReceiver() {
        fileReceiverJob?.cancel()
        fileReceiverJob = null
        _uiState.update { it.copy(fileReceiverRunning = false) }
        addLog("File receiver stopped")
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = null
        _uiState.update { it.copy(discoveryRunning = false, discoveryTimedOut = false, status = "Discovery stopped") }
        addLog("Peer discovery stopped")
    }

    fun connectSelectedPeer() {
        val state = _uiState.value
        val peer = state.selectedPeer?.takeIf { it.role == PeerRole.SERVER }
            ?: state.connectionPeer
            ?: state.peers.firstOrNull { it.role == PeerRole.SERVER }
            ?: return setError("Select a server peer first")
        if (state.nickname.isBlank()) return setError("Nickname must not be blank")
        connectToPeer(peer, state.sessionPassword, localFileReceiverPortFor(peer))
    }

    fun startHosting(chatPortValue: String, filePortValue: String) {
        val state = _uiState.value
        val chatPort = chatPortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        val filePort = filePortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        if (state.nickname.isBlank()) return setError("Nickname must not be blank")
        if (state.sessionPassword.isBlank()) return setError("Room password must not be blank")
        if (chatPort == null || filePort == null) return setError("Ports must be between 1 and 65535")
        if (chatPort == filePort) return setError("Chat and file ports must be different")
        if (state.connected || state.connecting) return setError("Disconnect from the current room before hosting")
        if (state.hosting || state.hostingStarting) return

        hostConnectionAttemptedForPeerId = null
        _uiState.update {
            it.copy(
                hostingStarting = true,
                hostChatPort = chatPort.toString(),
                hostFilePort = filePort.toString(),
                error = null,
                status = "Starting hosted room",
            )
        }
        addLog("Starting Android-hosted room on chat port $chatPort and file port $filePort")
        runCatching {
            AndroidChatHostService.start(
                context = getApplication(),
                peerId = hostPeerId,
                nickname = state.nickname.trim(),
                password = state.sessionPassword,
                chatPort = chatPort,
                filePort = filePort,
            )
        }.onFailure { error ->
            _uiState.update { it.copy(hostingStarting = false) }
            setError(error.message ?: "Unable to start hosted room")
        }
    }

    fun stopHosting() {
        if (!_uiState.value.hosting && !_uiState.value.hostingStarting) return
        hostConnectionAttemptedForPeerId = null
        disconnectRequested = true
        receiveJob?.cancel()
        receiveJob = null
        fileReceiverJob?.cancel()
        fileReceiverJob = null
        _uiState.update { it.copy(hostingStarting = false, status = "Stopping hosted room") }
        viewModelScope.launch {
            chatClient.disconnect()
            AndroidChatHostService.stop(getApplication())
            _uiState.update {
                it.copy(
                    connected = false,
                    connecting = false,
                    fileReceiverRunning = false,
                    hostedParticipantCount = 0,
                    status = "Hosted room stopped",
                )
            }
            addLog("Android-hosted room stopped")
        }
    }

    private fun connectToPeer(peer: DiscoveredPeer, password: String, localFilePort: Int) {
        val state = _uiState.value
        _uiState.update { it.copy(connecting = true, error = null, status = "Connecting to ${peer.nickname}") }
        disconnectRequested = false
        addLog("Connecting to ${peer.nickname} at ${peer.host}:${peer.chatPort}")
        viewModelScope.launch {
            runCatching {
                chatClient.connect(
                    peer.host,
                    peer.chatPort,
                    state.nickname.trim(),
                    password,
                    androidCapabilities(localFilePort),
                )
            }.onSuccess { acceptedNickname ->
                _uiState.update {
                    it.copy(
                        nickname = acceptedNickname,
                        selectedPeer = peer,
                        connectionPeer = peer,
                        connected = true,
                        connecting = false,
                        status = "Connected as $acceptedNickname",
                        error = null,
                    )
                }
                preferences.edit { putString(KEY_NICKNAME, acceptedNickname) }
                addLog("Connected as $acceptedNickname")
                startReceiving()
                if (_uiState.value.autoReceiveFiles) startFileReceiver()
            }.onFailure { error ->
                _uiState.update { it.copy(connecting = false, connected = false, error = error.message, status = "Connection failed") }
                addLog(error.message ?: "Connection failed", level = "ERROR")
            }
        }
    }

    fun disconnect() {
        if (_uiState.value.hosting || _uiState.value.hostingStarting) {
            stopHosting()
            return
        }
        disconnectRequested = true
        receiveJob?.cancel()
        receiveJob = null
        fileReceiverJob?.cancel()
        fileReceiverJob = null
        viewModelScope.launch {
            chatClient.disconnect()
            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = null, status = "Disconnected") }
            addLog("Disconnected")
        }
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputMessage.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { chatClient.sendMessage(text) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            inputMessage = "",
                            messages = state.messages + ChatLine(state.nickname, text, outbound = true),
                            status = "Message sent",
                        )
                    }
                    addLog("Sent message (${text.length} chars)")
                }
                .onFailure { setError(it.message ?: "Unable to send message") }
        }
    }

    fun selectFile(uri: Uri) {
        val file = resolveSelectedFile(uri)
        _uiState.update { it.copy(selectedFile = file, status = "Selected ${file.name}", error = null) }
        addLog("Selected file ${file.name} (${file.size} bytes)")
    }

    fun sendSelectedFile() {
        val state = _uiState.value
        if (!state.connected) return setError("Connect to a desktop first")
        val peer = state.fileRecipient ?: return setError("Select a recipient first")
        val file = state.selectedFile ?: return setError("Select a file first")
        _uiState.update { it.copy(fileProgress = FileSendProgress(file.name, 0, file.size, active = true), error = null) }
        addLog("Sending file ${file.name} (${file.size} bytes) to ${peer.nickname} via ${peer.fileTargetHost}:${peer.fileTargetPort}")
        viewModelScope.launch {
            runCatching {
                fileSender.sendFile(
                    host = peer.fileTargetHost,
                    port = peer.fileTargetPort,
                    senderId = state.nickname,
                    recipientId = peer.nickname,
                    sessionPassword = state.sessionPassword,
                    selectedFile = file,
                ) { sent, total ->
                    _uiState.update { current ->
                        current.copy(fileProgress = current.fileProgress.copy(bytesSent = sent, totalBytes = total, active = true))
                    }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        fileProgress = it.fileProgress.copy(active = false),
                        recentTransfers = listOf(
                            TransferRecord(
                                fileName = file.name,
                                bytes = file.size,
                                direction = TransferDirection.SENT,
                                result = TransferResult.COMPLETED,
                                peerName = peer.nickname,
                            ),
                        ) + it.recentTransfers.take(MAX_RECENT_TRANSFERS - 1),
                        status = "File sent",
                    )
                }
                addLog("File sent: ${file.name}")
            }.onFailure { error ->
                val message = userFacingError(error, "Unable to send file")
                _uiState.update {
                    it.copy(
                        fileProgress = it.fileProgress.copy(active = false, error = message),
                        recentTransfers = listOf(
                            TransferRecord(
                                fileName = file.name,
                                bytes = file.size,
                                direction = TransferDirection.SENT,
                                result = TransferResult.FAILED,
                                peerName = peer.nickname,
                            ),
                        ) + it.recentTransfers.take(MAX_RECENT_TRANSFERS - 1),
                        error = message,
                        status = "File send failed",
                    )
                }
                addLog(message, level = "ERROR")
            }
        }
    }

    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = viewModelScope.launch {
            while (_uiState.value.connected) {
                runCatching { chatClient.readMessage() }
                    .onSuccess { message ->
                        if (message == null) {
                            val status = if (disconnectRequested) "Disconnected" else "Connection closed"
                            fileReceiverJob?.cancel()
                            fileReceiverJob = null
                            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = null, status = status) }
                            addLog(status)
                            return@launch
                        }
                        when (message.type) {
                            WireMessageType.CHAT -> handleIncomingChatMessage(message.sender, message.payload)
                            WireMessageType.USER_JOINED -> handleUserJoined(message.sender, message.payload)
                            WireMessageType.USER_LEFT -> handleUserLeft(message.sender)
                            WireMessageType.SYSTEM,
                            -> _uiState.update { state ->
                                state.copy(status = message.payload.ifBlank { message.type.name })
                            }
                            else -> Unit
                        }
                    }
                    .onFailure { error ->
                        fileReceiverJob?.cancel()
                        fileReceiverJob = null
                        if (disconnectRequested || error.isExpectedDisconnect()) {
                            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = null, status = "Disconnected") }
                            addLog("Disconnected")
                        } else {
                            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = error.message, status = "Receive failed") }
                            addLog(error.message ?: "Receive failed", level = "ERROR")
                        }
                        return@launch
                    }
            }
        }
    }

    private fun handleIncomingChatMessage(sender: String, text: String) {
        if (sender.equals(_uiState.value.nickname, ignoreCase = true)) {
            return
        }
        _uiState.update { state ->
            state.copy(messages = state.messages + ChatLine(sender, text, outbound = false))
        }
    }

    private fun handleUserJoined(nickname: String, encodedCapabilities: String) {
        val peerNickname = nickname.trim()
        if (peerNickname.isBlank() || peerNickname.equals(_uiState.value.nickname, ignoreCase = true)) {
            return
        }
        _uiState.update { state ->
            val serverPeer = state.selectedPeer
            val existingServerPeer = state.peers.firstOrNull { it.role == PeerRole.SERVER && it.nickname.equals(peerNickname, ignoreCase = true) }
            val hostedParticipant = AndroidChatHostService.state.value.participants
                .firstOrNull { it.nickname.equals(peerNickname, ignoreCase = true) }
            val capabilities = PeerCapabilities.decode(encodedCapabilities)
            val peer = DiscoveredPeer(
                peerId = existingServerPeer?.peerId ?: "chat:$peerNickname",
                nickname = peerNickname,
                host = hostedParticipant?.host ?: existingServerPeer?.host ?: serverPeer?.host ?: "chat-room",
                chatPort = existingServerPeer?.chatPort ?: serverPeer?.chatPort ?: SecureLanPorts.DEFAULT_CHAT_PORT,
                filePort = existingServerPeer?.filePort ?: serverPeer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
                role = existingServerPeer?.role ?: PeerRole.CHAT_CLIENT,
                fileTargetHost = hostedParticipant?.host ?: existingServerPeer?.fileTargetHost ?: serverPeer?.host ?: "chat-room",
                fileTargetPort = hostedParticipant?.filePort
                    ?: capabilities.fileReceivePort().takeIf { it in 1..65535 }
                    ?: existingServerPeer?.fileTargetPort
                    ?: clientFilePortForServer(serverPeer),
            )
            val peers = upsertPeer(state.peers, peer)
                .sortedBy { it.nickname.lowercase() }
            state.copy(peers = peers, status = "$peerNickname joined the chat")
        }
        addLog("Peer joined chat: $peerNickname")
    }

    private fun handleUserLeft(nickname: String) {
        val peerNickname = nickname.trim()
        if (peerNickname.isBlank()) return
        _uiState.update { state ->
            state.copy(
                peers = state.peers.filterNot { it.role == PeerRole.CHAT_CLIENT && it.nickname.equals(peerNickname, ignoreCase = true) },
                status = "$peerNickname left the chat",
            )
        }
        addLog("Peer left chat: $peerNickname")
    }

    private fun upsertPeer(peers: List<DiscoveredPeer>, peer: DiscoveredPeer): List<DiscoveredPeer> {
        val filtered = peers.filterNot { existing ->
            samePeer(existing, peer)
        }
        return filtered + peer
    }

    private fun samePeer(left: DiscoveredPeer, right: DiscoveredPeer): Boolean {
        if (left.peerId.isNotBlank() && right.peerId.isNotBlank() && left.peerId == right.peerId) {
            return true
        }
        return left.host == right.host && left.chatPort == right.chatPort && left.role == right.role
    }

    private fun clientFilePortForServer(serverPeer: DiscoveredPeer?): Int {
        val serverFilePort = serverPeer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        val candidate = serverFilePort + CLIENT_FILE_PORT_OFFSET
        return if (candidate > 65535) {
            SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT + CLIENT_FILE_PORT_OFFSET
        } else {
            candidate
        }
    }

    private fun androidCapabilities(fileReceivePort: Int): PeerCapabilities = PeerCapabilities.android(
        APP_VERSION,
        fileReceivePort,
        android.os.Build.MODEL ?: "Android device",
    )

    private fun resolveSelectedFile(uri: Uri): SelectedFile {
        val resolver = getApplication<Application>().contentResolver
        var name = uri.lastPathSegment ?: "selected-file"
        var size = -1L
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = it.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = it.getLong(sizeIndex)
            }
        }
        return SelectedFile(uri, name, size.coerceAtLeast(0L))
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(error = message, status = message) }
        addLog(message, level = "ERROR")
    }

    private fun addLog(message: String, level: String = "INFO") {
        _uiState.update { state ->
            val updatedLogs = (state.logs + AppLogEntry(level = level, message = message)).takeLast(MAX_LOG_ENTRIES)
            state.copy(logs = updatedLogs)
        }
    }

    private fun userFacingError(error: Throwable, fallback: String): String {
        val message = error.message?.takeIf { it.isNotBlank() }
        if (message != null) return message
        return error::class.simpleName?.let { "$fallback: $it" } ?: fallback
    }

    private fun localFileReceiverPort(state: MainUiState): Int {
        return localFileReceiverPortFor(state.connectionPeer ?: state.selectedPeer)
    }

    private fun localFileReceiverPortFor(peer: DiscoveredPeer?): Int {
        if (_uiState.value.hosting) {
            return _uiState.value.hostFilePort.toIntOrNull()?.takeIf { it in 1..65535 }
                ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        }
        val remoteFilePort = peer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        val candidate = remoteFilePort + CLIENT_FILE_PORT_OFFSET
        return if (candidate > 65535) {
            SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT + CLIENT_FILE_PORT_OFFSET
        } else {
            candidate
        }
    }

    private fun createNotificationChannel() {
        val application = getApplication<Application>()
        val manager = application.getSystemService(NotificationManager::class.java)
        val localizedContext = localizedApplicationContext()
        manager.createNotificationChannel(
            NotificationChannel(
                FILE_NOTIFICATION_CHANNEL,
                localizedContext.getString(R.string.notification_channel_files),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun observeHostService() {
        viewModelScope.launch {
            AndroidChatHostService.state.collect { hostState -> handleHostServiceState(hostState) }
        }
    }

    private fun handleHostServiceState(hostState: AndroidHostServiceState) {
        val previous = _uiState.value
        if (hostState.running) {
            val localPeer = DiscoveredPeer(
                peerId = hostState.peerId,
                nickname = hostState.nickname,
                host = LOOPBACK_HOST,
                chatPort = hostState.chatPort,
                filePort = hostState.filePort,
                role = PeerRole.SERVER,
                fileTargetHost = LOOPBACK_HOST,
                fileTargetPort = hostState.filePort,
            )
            val hostedPeers = hostState.participants.map { participant ->
                DiscoveredPeer(
                    peerId = "host-client:${participant.nickname.lowercase()}",
                    nickname = participant.nickname,
                    host = participant.host,
                    chatPort = hostState.chatPort,
                    filePort = participant.filePort,
                    role = PeerRole.CHAT_CLIENT,
                    fileTargetHost = participant.host,
                    fileTargetPort = participant.filePort,
                )
            }
            _uiState.update { state ->
                val selectedHostedPeer = state.selectedPeer?.takeIf { it.role == PeerRole.CHAT_CLIENT }?.let { selected ->
                    hostedPeers.firstOrNull { it.nickname.equals(selected.nickname, ignoreCase = true) }
                }
                state.copy(
                    sessionPassword = AndroidChatHostService.currentSessionPassword(),
                    hosting = true,
                    hostingStarting = false,
                    hostChatPort = hostState.chatPort.toString(),
                    hostFilePort = hostState.filePort.toString(),
                    hostedParticipantCount = hostedPeers.size,
                    connectionPeer = localPeer,
                    selectedPeer = selectedHostedPeer ?: localPeer,
                    peers = state.peers.filter { it.role == PeerRole.SERVER && it.peerId != hostState.peerId } + hostedPeers,
                    error = null,
                    status = if (state.connected) "Hosting room" else "Hosted room is ready",
                )
            }
            if (!previous.connected && !previous.connecting && hostConnectionAttemptedForPeerId != hostState.peerId) {
                hostConnectionAttemptedForPeerId = hostState.peerId
                connectToPeer(localPeer, AndroidChatHostService.currentSessionPassword(), hostState.filePort)
            }
            return
        }

        if (hostState.starting) {
            _uiState.update { it.copy(hostingStarting = true, error = null, status = "Starting hosted room") }
            return
        }

        if (hostState.error != null) {
            hostConnectionAttemptedForPeerId = null
            _uiState.update {
                it.copy(hosting = false, hostingStarting = false, connected = false, connecting = false, error = hostState.error, status = "Unable to host room")
            }
            addLog(hostState.error, level = "ERROR")
            return
        }

        if (previous.hosting || previous.hostingStarting) {
            hostConnectionAttemptedForPeerId = null
            receiveJob?.cancel()
            receiveJob = null
            fileReceiverJob?.cancel()
            fileReceiverJob = null
            disconnectRequested = true
            viewModelScope.launch { chatClient.disconnect() }
            _uiState.update {
                it.copy(
                    hosting = false,
                    hostingStarting = false,
                    connected = false,
                    connecting = false,
                    fileReceiverRunning = false,
                    hostedParticipantCount = 0,
                    peers = it.peers.filterNot { peer -> peer.role == PeerRole.CHAT_CLIENT },
                    selectedPeer = null,
                    connectionPeer = null,
                    status = "Hosted room stopped",
                )
            }
        }
    }

    private fun postFileReceivedNotification(fileName: String, savedPath: String) {
        if (!_uiState.value.notificationsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            getApplication<Application>().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val application = getApplication<Application>()
        val localizedContext = localizedApplicationContext()
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            application,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = android.app.Notification.Builder(application, FILE_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(localizedContext.getString(R.string.notification_file_received))
            .setContentText(localizedContext.getString(R.string.notification_file_saved, fileName, savedPath))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        application.getSystemService(NotificationManager::class.java)
            .notify(FILE_NOTIFICATION_ID, notification)
    }

    @SuppressLint("AppBundleLocaleChanges") // Language splitting is disabled in android.bundle.language.
    private fun localizedApplicationContext(): Context {
        val application = getApplication<Application>()
        val locale = when (_uiState.value.appLanguage) {
            AppLanguage.SYSTEM -> return application
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.RUSSIAN -> Locale("ru")
        }
        val configuration = Configuration(application.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return application.createConfigurationContext(configuration)
    }

    override fun onCleared() {
        discoveryJob?.cancel()
        discoveryTimeoutJob?.cancel()
        receiveJob?.cancel()
        fileReceiverJob?.cancel()
        viewModelScope.launch { chatClient.disconnect() }
        super.onCleared()
    }

    private companion object {
        private const val APP_VERSION = "0.5.0"
        private const val CLIENT_FILE_PORT_OFFSET = 1000
        private const val MAX_LOG_ENTRIES = 300
        private const val MAX_RECENT_TRANSFERS = 20
        private const val DISCOVERY_EMPTY_STATE_DELAY_MS = 8_000L
        private const val PREFERENCES_NAME = "secure_lan_preferences"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_AUTO_RECEIVE = "auto_receive"
        private const val KEY_MANUAL_HOST = "manual_host"
        private const val KEY_MANUAL_CHAT_PORT = "manual_chat_port"
        private const val KEY_MANUAL_FILE_PORT = "manual_file_port"
        private const val KEY_HOST_PEER_ID = "host_peer_id"
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val FILE_NOTIFICATION_CHANNEL = "secure_lan_files"
        private const val FILE_NOTIFICATION_ID = 5051
    }
}

private fun Throwable.isExpectedDisconnect(): Boolean {
    val message = message?.lowercase() ?: return false
    return "socket closed" in message || "connection reset" in message || "connection abort" in message
}
