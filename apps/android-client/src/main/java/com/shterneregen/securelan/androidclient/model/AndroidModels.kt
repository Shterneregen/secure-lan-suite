package com.shterneregen.securelan.androidclient.model

import android.net.Uri
import java.time.Instant

object SecureLanPorts {
    const val DEFAULT_CHAT_PORT: Int = 5050
    const val DEFAULT_FILE_TRANSFER_PORT: Int = 5051
    const val DEFAULT_DISCOVERY_PORT: Int = 5052
}

enum class PeerRole {
    SERVER,
    CHAT_CLIENT,
}

data class DiscoveredPeer(
    val peerId: String,
    val nickname: String,
    val host: String,
    val chatPort: Int,
    val filePort: Int,
    val lastSeen: Instant = Instant.now(),
    val role: PeerRole = PeerRole.SERVER,
    val fileTargetHost: String = host,
    val fileTargetPort: Int = filePort,
)

data class ChatLine(
    val sender: String,
    val text: String,
    val outbound: Boolean,
    val timestamp: Instant = Instant.now(),
)

data class AppLogEntry(
    val timestamp: Instant = Instant.now(),
    val level: String = "INFO",
    val message: String,
)

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
)

data class FileSendProgress(
    val fileName: String = "",
    val bytesSent: Long = 0,
    val totalBytes: Long = 0,
    val active: Boolean = false,
    val error: String? = null,
) {
    val percent: Float
        get() = if (totalBytes <= 0) 0f else (bytesSent.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

data class IncomingFileProgress(
    val fileName: String = "",
    val bytesReceived: Long = 0,
    val totalBytes: Long = 0,
    val active: Boolean = false,
    val completedPath: String? = null,
    val error: String? = null,
) {
    val percent: Float
        get() = if (totalBytes <= 0) 0f else (bytesReceived.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

data class MainUiState(
    val nickname: String = "Android",
    val sessionPassword: String = "",
    val darkThemeEnabled: Boolean = true,
    val peers: List<DiscoveredPeer> = emptyList(),
    val selectedPeer: DiscoveredPeer? = null,
    val connectionPeer: DiscoveredPeer? = null,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val inputMessage: String = "",
    val messages: List<ChatLine> = emptyList(),
    val selectedFile: SelectedFile? = null,
    val fileProgress: FileSendProgress = FileSendProgress(),
    val incomingFileProgress: IncomingFileProgress = IncomingFileProgress(),
    val fileReceiverRunning: Boolean = false,
    val discoveryRunning: Boolean = false,
    val status: String = "Ready",
    val error: String? = null,
    val logs: List<AppLogEntry> = listOf(AppLogEntry(message = "Android client started")),
)
