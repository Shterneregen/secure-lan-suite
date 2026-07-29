package com.shterneregen.securelan.desktop.compose.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Canonical icon set for SecureLanSuite product concepts.
 *
 * Rules:
 * - One icon represents one concept everywhere.
 * - Icons support labels; they never replace primary-action text.
 * - All icons are Material Design Outlined/Filled vectors exposed through Compose Multiplatform.
 */
object SecureLanIcons {
    val Attach: ImageVector = Icons.Outlined.AttachFile
    val Voice: ImageVector = Icons.Outlined.Mic
    val Video: ImageVector = Icons.Outlined.Videocam
    val File: ImageVector = Icons.Outlined.Description
    val Encryption: ImageVector = Icons.Outlined.Lock
    val Diagnostics: ImageVector = Icons.Outlined.MonitorHeart
    val QuickShare: ImageVector = Icons.Outlined.Share
    val CallEnd: ImageVector = Icons.Outlined.CallEnd
    val Settings: ImageVector = Icons.Outlined.Settings
    val Close: ImageVector = Icons.Outlined.Close
    val LightTheme: ImageVector = Icons.Outlined.LightMode
    val DarkTheme: ImageVector = Icons.Outlined.DarkMode

    // Context Assistant product concepts.
    val Person: ImageVector = Icons.Outlined.Person
    val Guidance: ImageVector = Icons.Outlined.Info
    val History: ImageVector = Icons.Outlined.History
    val Room: ImageVector = Icons.Outlined.MeetingRoom

    val PresenceOnline: ImageVector = Icons.Filled.Circle
    val PresenceOffline: ImageVector = Icons.Outlined.Circle
    val Devices: ImageVector = Icons.Outlined.SettingsInputHdmi

    // Attachment-menu specific privacy tools reuse the encryption / image concepts.
    val EncryptedTextOrFile: ImageVector = Encryption
    val Steganography: ImageVector = Icons.Outlined.Image

    // Inline field actions.
    val GenerateNickname: ImageVector = Icons.Outlined.Casino
    val Visibility: ImageVector = Icons.Outlined.Visibility
    val VisibilityOff: ImageVector = Icons.Outlined.VisibilityOff

    /**
     * Resolves the canonical icon for a peer/conversation capability label.
     * Returns `null` for unknown labels so the UI can fall back to text-only.
     */
    fun forCapability(label: String): ImageVector? = when (label) {
        "Voice" -> Voice
        "Video" -> Video
        "File" -> File
        else -> null
    }
}
