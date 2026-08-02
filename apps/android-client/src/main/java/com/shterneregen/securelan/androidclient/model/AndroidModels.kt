package com.shterneregen.securelan.androidclient.model

import android.net.Uri
import com.shterneregen.securelan.common.net.NetworkConstants
import java.time.Instant

object SecureLanPorts {
    val DEFAULT_CHAT_PORT: Int = NetworkConstants.DEFAULT_CHAT_PORT
    val DEFAULT_FILE_TRANSFER_PORT: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT
    val DEFAULT_DISCOVERY_PORT: Int = NetworkConstants.DEFAULT_DISCOVERY_PORT
}

enum class PeerRole {
    SERVER,
    CHAT_CLIENT,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    RUSSIAN,
}

enum class NearbyPermissionState {
    NOT_REQUIRED,
    REQUIRED,
    GRANTED,
    DENIED,
}

enum class TransferDirection {
    SENT,
    RECEIVED,
}

enum class TransferResult {
    COMPLETED,
    FAILED,
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

data class TransferRecord(
    val fileName: String,
    val bytes: Long,
    val direction: TransferDirection,
    val result: TransferResult,
    val peerName: String? = null,
    val savedPath: String? = null,
    val timestamp: Instant = Instant.now(),
)

data class MainUiState(
    val nickname: String = "",
    val sessionPassword: String = "",
    val manualHost: String = "",
    val manualChatPort: String = SecureLanPorts.DEFAULT_CHAT_PORT.toString(),
    val manualFilePort: String = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT.toString(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val autoReceiveFiles: Boolean = true,
    val nearbyPermissionState: NearbyPermissionState = NearbyPermissionState.REQUIRED,
    val networkAvailable: Boolean = true,
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
    val recentTransfers: List<TransferRecord> = emptyList(),
    val fileReceiverRunning: Boolean = false,
    val discoveryRunning: Boolean = false,
    val discoveryTimedOut: Boolean = false,
    val status: String = "Ready",
    val error: String? = null,
    val logs: List<AppLogEntry> = listOf(AppLogEntry(message = "Android client started")),
) {
    /** Compatibility for legacy composables kept while the redesigned screens settle. */
    val darkThemeEnabled: Boolean
        get() = themeMode == ThemeMode.DARK
}
