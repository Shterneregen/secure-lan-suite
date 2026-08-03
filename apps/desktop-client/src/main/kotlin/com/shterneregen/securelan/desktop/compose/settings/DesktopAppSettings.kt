package com.shterneregen.securelan.desktop.compose.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import java.nio.file.Path

data class DesktopWindowSettings(
    val width: Int = 1360,
    val height: Int = 860,
    val x: Int? = null,
    val y: Int? = null,
    val maximized: Boolean = false,
) {
    fun normalized(): DesktopWindowSettings = copy(
        width = width.coerceIn(820, 10_000),
        height = height.coerceIn(640, 10_000),
        x = x?.coerceIn(-50_000, 50_000),
        y = y?.coerceIn(-50_000, 50_000),
    )
}

data class DesktopMediaSettings(
    val microphoneDeviceId: String = "",
    val cameraDeviceId: String = "",
    val outputDeviceId: String = "",
    val volumePercent: Int = 100,
) {
    fun normalized(): DesktopMediaSettings = copy(
        microphoneDeviceId = microphoneDeviceId.trim(),
        cameraDeviceId = cameraDeviceId.trim(),
        outputDeviceId = outputDeviceId.trim(),
        volumePercent = volumePercent.coerceIn(0, 100),
    )
}

enum class DesktopConnectionMode { HOST, JOIN }

data class DesktopRecentRoom(
    val host: String,
    val chatPort: Int = NetworkConstants.DEFAULT_CHAT_PORT,
    val filePort: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
) {
    fun normalizedOrNull(): DesktopRecentRoom? = copy(host = host.trim()).takeIf {
        it.host.isNotEmpty() && it.chatPort in 1..65_535 && it.filePort in 1..65_535
    }
}

data class DesktopNetworkSettings(
    val discoverable: Boolean = true,
    val serverChatPort: Int = NetworkConstants.DEFAULT_CHAT_PORT,
    val serverFilePort: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
    val clientChatPort: Int = NetworkConstants.DEFAULT_CHAT_PORT,
    val clientFilePort: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
    val lastConnectionMode: DesktopConnectionMode = DesktopConnectionMode.HOST,
    val recentRooms: List<DesktopRecentRoom> = emptyList(),
) {
    fun normalized(): DesktopNetworkSettings = copy(
        serverChatPort = serverChatPort.validPortOr(NetworkConstants.DEFAULT_CHAT_PORT),
        serverFilePort = serverFilePort.validPortOr(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT),
        clientChatPort = clientChatPort.validPortOr(NetworkConstants.DEFAULT_CHAT_PORT),
        clientFilePort = clientFilePort.validPortOr(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT),
        recentRooms = recentRooms.mapNotNull(DesktopRecentRoom::normalizedOrNull)
            .distinctBy { "${it.host.lowercase()}:${it.chatPort}:${it.filePort}" }
            .take(MAX_RECENT_ROOMS),
    )

    fun withRecentRoom(room: DesktopRecentRoom): DesktopNetworkSettings {
        val normalizedRoom = room.normalizedOrNull() ?: return this
        return copy(
            recentRooms = (listOf(normalizedRoom) + recentRooms.filterNot {
                it.host.equals(normalizedRoom.host, ignoreCase = true) &&
                    it.chatPort == normalizedRoom.chatPort && it.filePort == normalizedRoom.filePort
            }).take(MAX_RECENT_ROOMS),
        )
    }

    companion object {
        const val MAX_RECENT_ROOMS: Int = 10
    }
}

data class DesktopNotificationSettings(
    val enabled: Boolean = true,
    val soundsEnabled: Boolean = true,
    val transferNotificationsEnabled: Boolean = true,
)

enum class IncomingFileConfirmationMode {
    ASK,
    AUTO_ACCEPT_KNOWN_PEERS,
}

data class DesktopTransferSettings(
    val incomingFileConfirmation: IncomingFileConfirmationMode = IncomingFileConfirmationMode.ASK,
    val notifyOnCompletion: Boolean = true,
)

/** User preferences that are safe to restore between desktop-client sessions. */
data class DesktopAppSettings(
    val displayName: String? = null,
    val themeMode: SecureLanThemeMode = SecureLanThemeMode.DARK,
    val reducedMotion: Boolean = false,
    val window: DesktopWindowSettings = DesktopWindowSettings(),
    val downloadsDirectory: String = defaultDownloadsDirectory(),
    val media: DesktopMediaSettings = DesktopMediaSettings(),
    val network: DesktopNetworkSettings = DesktopNetworkSettings(),
    val notifications: DesktopNotificationSettings = DesktopNotificationSettings(),
    val transfers: DesktopTransferSettings = DesktopTransferSettings(),
) {
    fun normalized(): DesktopAppSettings = copy(
        displayName = displayName?.trim()?.takeIf(String::isNotEmpty),
        window = window.normalized(),
        downloadsDirectory = downloadsDirectory.trim().takeIf(String::isNotEmpty) ?: defaultDownloadsDirectory(),
        media = media.normalized(),
        network = network.normalized(),
    )

    fun withDisplayName(value: String?): DesktopAppSettings = copy(displayName = value).normalized()

    fun downloadsPath(): Path = runCatching {
        Path.of(downloadsDirectory).toAbsolutePath().normalize()
    }.getOrElse { Path.of(defaultDownloadsDirectory()) }
}

interface DesktopAppSettingsStore {
    fun load(): DesktopAppSettings
    fun save(settings: DesktopAppSettings): Boolean
}

/** Single source of truth shared by the window, Compose UI, and host adapter. */
class DesktopAppSettingsController(
    initialSettings: DesktopAppSettings = DesktopAppSettings(),
    private val store: DesktopAppSettingsStore? = null,
) {
    var settings: DesktopAppSettings by mutableStateOf(initialSettings.normalized())
        private set

    @Synchronized
    fun update(transform: (DesktopAppSettings) -> DesktopAppSettings): DesktopAppSettings {
        val updated = transform(settings).normalized()
        if (updated != settings) {
            settings = updated
            store?.save(updated)
        }
        return settings
    }
}

private fun Int.validPortOr(fallback: Int): Int = takeIf { it in 1..65_535 } ?: fallback

private fun defaultDownloadsDirectory(): String =
    DesktopAppPaths.downloadsDirectory().toString()
