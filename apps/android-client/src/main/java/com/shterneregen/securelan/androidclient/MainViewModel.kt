package com.shterneregen.securelan.androidclient

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shterneregen.securelan.androidclient.model.AppLogEntry
import com.shterneregen.securelan.androidclient.model.ChatLine
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.FileSendProgress
import com.shterneregen.securelan.androidclient.model.IncomingFileProgress
import com.shterneregen.securelan.androidclient.model.MainUiState
import com.shterneregen.securelan.androidclient.model.PeerRole
import com.shterneregen.securelan.androidclient.model.SelectedFile
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.network.PeerDiscoveryRepository
import com.shterneregen.securelan.androidclient.network.SecureChatClient
import com.shterneregen.securelan.androidclient.network.SecureFileReceiver
import com.shterneregen.securelan.androidclient.network.SecureFileSender
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val discoveryRepository = PeerDiscoveryRepository()
    private val chatClient = SecureChatClient()
    private val fileSender = SecureFileSender(application.contentResolver)
    private val fileReceiver = SecureFileReceiver(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var discoveryJob: Job? = null
    private var receiveJob: Job? = null
    private var fileReceiverJob: Job? = null

    fun updateNickname(value: String) = _uiState.update { it.copy(nickname = value) }

    fun updateSessionPassword(value: String) = _uiState.update { it.copy(sessionPassword = value) }

    fun updateDarkThemeEnabled(value: Boolean) = _uiState.update { it.copy(darkThemeEnabled = value) }

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
        _uiState.update { it.copy(discoveryRunning = true, status = "Listening for SecureLan peers") }
        addLog("Started peer discovery")
        discoveryJob = viewModelScope.launch {
            discoveryRepository.discoverPeers().collect { peer ->
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
                    state.copy(peers = peers, selectedPeer = selectedPeer, connectionPeer = connectionPeer, status = "Found ${peers.size} peer(s)")
                }
            }
        }
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
                                status = "Received ${metadata.fileName}",
                            )
                        }
                        addLog("Received file ${metadata.fileName}; saved to $savedPath")
                    },
                    onError = { message, _ ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = it.incomingFileProgress.copy(active = false, error = message),
                                status = message,
                            )
                        }
                        addLog(message, level = "ERROR")
                    },
                )
            }.onFailure { error ->
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
        _uiState.update { it.copy(fileReceiverRunning = false, status = "File receiver stopped") }
        addLog("File receiver stopped")
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _uiState.update { it.copy(discoveryRunning = false, status = "Discovery stopped") }
        addLog("Peer discovery stopped")
    }

    fun connectSelectedPeer() {
        val state = _uiState.value
        val peer = state.selectedPeer?.takeIf { it.role == PeerRole.SERVER }
            ?: state.connectionPeer
            ?: state.peers.firstOrNull { it.role == PeerRole.SERVER }
            ?: return setError("Select a server peer first")
        if (state.nickname.isBlank()) return setError("Nickname must not be blank")
        _uiState.update { it.copy(connecting = true, error = null, status = "Connecting to ${peer.nickname}") }
        addLog("Connecting to ${peer.nickname} at ${peer.host}:${peer.chatPort}")
        viewModelScope.launch {
            runCatching {
                chatClient.connect(peer.host, peer.chatPort, state.nickname.trim(), state.sessionPassword)
            }.onSuccess { acceptedNickname ->
                _uiState.update {
                    it.copy(
                        nickname = acceptedNickname,
                        connected = true,
                        connecting = false,
                        status = "Connected as $acceptedNickname",
                        error = null,
                    )
                }
                addLog("Connected as $acceptedNickname")
                startReceiving()
            }.onFailure { error ->
                _uiState.update { it.copy(connecting = false, connected = false, error = error.message, status = "Connection failed") }
                addLog(error.message ?: "Connection failed", level = "ERROR")
            }
        }
    }

    fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        viewModelScope.launch {
            chatClient.disconnect()
            _uiState.update { it.copy(connected = false, status = "Disconnected") }
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
        val peer = state.selectedPeer ?: return setError("Select a peer first")
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
                _uiState.update { it.copy(fileProgress = it.fileProgress.copy(active = false), status = "File sent") }
                addLog("File sent: ${file.name}")
            }.onFailure { error ->
                val message = userFacingError(error, "Unable to send file")
                _uiState.update {
                    it.copy(
                        fileProgress = it.fileProgress.copy(active = false, error = message),
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
                            _uiState.update { it.copy(connected = false, status = "Connection closed") }
                            addLog("Connection closed")
                            return@launch
                        }
                        when (message.type) {
                            WireMessageType.CHAT -> handleIncomingChatMessage(message.sender, message.payload)
                            WireMessageType.USER_JOINED -> handleUserJoined(message.sender)
                            WireMessageType.USER_LEFT -> handleUserLeft(message.sender)
                            WireMessageType.SYSTEM,
                            -> _uiState.update { state ->
                                state.copy(status = message.payload.ifBlank { message.type.name })
                            }
                            else -> Unit
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(connected = false, error = error.message, status = "Receive failed") }
                        addLog(error.message ?: "Receive failed", level = "ERROR")
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

    private fun handleUserJoined(nickname: String) {
        val peerNickname = nickname.trim()
        if (peerNickname.isBlank() || peerNickname.equals(_uiState.value.nickname, ignoreCase = true)) {
            return
        }
        _uiState.update { state ->
            val serverPeer = state.selectedPeer
            val existingServerPeer = state.peers.firstOrNull { it.role == PeerRole.SERVER && it.nickname.equals(peerNickname, ignoreCase = true) }
            val peer = DiscoveredPeer(
                peerId = existingServerPeer?.peerId ?: "chat:$peerNickname",
                nickname = peerNickname,
                host = existingServerPeer?.host ?: serverPeer?.host ?: "chat-room",
                chatPort = existingServerPeer?.chatPort ?: serverPeer?.chatPort ?: SecureLanPorts.DEFAULT_CHAT_PORT,
                filePort = existingServerPeer?.filePort ?: serverPeer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
                role = existingServerPeer?.role ?: PeerRole.CHAT_CLIENT,
                fileTargetHost = existingServerPeer?.fileTargetHost ?: serverPeer?.host ?: "chat-room",
                fileTargetPort = existingServerPeer?.fileTargetPort ?: clientFilePortForServer(serverPeer),
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
        if (!state.connected) return SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        val remoteFilePort = state.selectedPeer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        val candidate = remoteFilePort + CLIENT_FILE_PORT_OFFSET
        return if (candidate > 65535) {
            SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT + CLIENT_FILE_PORT_OFFSET
        } else {
            candidate
        }
    }

    override fun onCleared() {
        discoveryJob?.cancel()
        receiveJob?.cancel()
        fileReceiverJob?.cancel()
        viewModelScope.launch { chatClient.disconnect() }
        super.onCleared()
    }

    private companion object {
        private const val CLIENT_FILE_PORT_OFFSET = 1000
        private const val MAX_LOG_ENTRIES = 300
    }
}
