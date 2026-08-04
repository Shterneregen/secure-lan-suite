package com.shterneregen.securelan.desktop.compose.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

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
    val Diagnostics: ImageVector = Icons.Outlined.MonitorHeart
    val QuickShare: ImageVector = Icons.Outlined.Share
    val QrCode: ImageVector = secureLanQrCodeIcon
    val Copy: ImageVector = secureLanCopyIcon
    val Open: ImageVector = secureLanOpenIcon
    val Stop: ImageVector = secureLanStopIcon
    val FolderOpen: ImageVector = secureLanFolderOpenIcon
    val Tools: ImageVector = Icons.Outlined.Build
    val Commands: ImageVector = Icons.Outlined.MoreVert
    val CallEnd: ImageVector = Icons.Outlined.CallEnd
    val Settings: ImageVector = Icons.Outlined.Settings
    val Close: ImageVector = Icons.Outlined.Close
    val Notifications: ImageVector = Icons.Outlined.NotificationsNone
    val Network: ImageVector = Icons.Outlined.Lan
    val LightTheme: ImageVector = Icons.Outlined.LightMode
    val IntermediateTheme: ImageVector = Icons.Outlined.Brightness6
    val DarkTheme: ImageVector = Icons.Outlined.DarkMode

    // Context Assistant product concepts.
    val Person: ImageVector = Icons.Outlined.Person
    val History: ImageVector = Icons.Outlined.History
    val Room: ImageVector = Icons.Outlined.MeetingRoom

    val PresenceOnline: ImageVector = Icons.Filled.Circle
    val PresenceOffline: ImageVector = Icons.Outlined.Circle
    val Devices: ImageVector = Icons.Outlined.SettingsInputHdmi

    // Privacy tool.
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

private val secureLanStopIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Stop",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 6f)
            lineTo(18f, 6f)
            lineTo(18f, 18f)
            lineTo(6f, 18f)
            close()
        }
    }.build()
}

private val secureLanFolderOpenIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "FolderOpen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
        ) {
            moveTo(3f, 6f)
            curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
            lineTo(10f, 4f)
            lineTo(12f, 6f)
            lineTo(19f, 6f)
            curveTo(20.1f, 6f, 21f, 6.9f, 21f, 8f)
            lineTo(21f, 10f)
            lineTo(7f, 10f)
            lineTo(3f, 19f)
            close()
            moveTo(7f, 10f)
            lineTo(22f, 10f)
            lineTo(18f, 20f)
            lineTo(3f, 20f)
            close()
        }
    }.build()
}

private val secureLanCopyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Copy",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(16f, 1f)
            lineTo(4f, 1f)
            curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
            lineTo(2f, 17f)
            lineTo(4f, 17f)
            lineTo(4f, 3f)
            lineTo(16f, 3f)
            close()
            moveTo(19f, 5f)
            lineTo(8f, 5f)
            curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
            lineTo(6f, 21f)
            curveTo(6f, 22.1f, 6.9f, 23f, 8f, 23f)
            lineTo(19f, 23f)
            curveTo(20.1f, 23f, 21f, 22.1f, 21f, 21f)
            lineTo(21f, 7f)
            curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
            close()
            moveTo(19f, 21f)
            lineTo(8f, 21f)
            lineTo(8f, 7f)
            lineTo(19f, 7f)
            close()
        }
    }.build()
}

private val secureLanOpenIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Open",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 19f)
            lineTo(5f, 19f)
            lineTo(5f, 5f)
            lineTo(12f, 5f)
            lineTo(12f, 3f)
            lineTo(5f, 3f)
            curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
            lineTo(3f, 19f)
            curveTo(3f, 20.1f, 3.89f, 21f, 5f, 21f)
            lineTo(19f, 21f)
            curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
            lineTo(21f, 12f)
            lineTo(19f, 12f)
            close()
            moveTo(14f, 3f)
            lineTo(14f, 5f)
            lineTo(17.59f, 5f)
            lineTo(7.76f, 14.83f)
            lineTo(9.17f, 16.24f)
            lineTo(19f, 6.41f)
            lineTo(19f, 10f)
            lineTo(21f, 10f)
            lineTo(21f, 3f)
            close()
        }
    }.build()
}

private val secureLanQrCodeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "QrCode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
        ) {
            moveTo(3f, 9f); lineTo(3f, 3f); lineTo(9f, 3f); lineTo(9f, 9f); close()
            moveTo(15f, 9f); lineTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 9f); close()
            moveTo(3f, 21f); lineTo(3f, 15f); lineTo(9f, 15f); lineTo(9f, 21f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 5f); lineTo(7f, 5f); lineTo(7f, 7f); lineTo(5f, 7f); close()
            moveTo(17f, 5f); lineTo(19f, 5f); lineTo(19f, 7f); lineTo(17f, 7f); close()
            moveTo(5f, 17f); lineTo(7f, 17f); lineTo(7f, 19f); lineTo(5f, 19f); close()
            moveTo(11f, 11f); lineTo(14f, 11f); lineTo(14f, 14f); lineTo(11f, 14f); close()
            moveTo(16f, 11f); lineTo(18f, 11f); lineTo(18f, 13f); lineTo(16f, 13f); close()
            moveTo(19f, 14f); lineTo(21f, 14f); lineTo(21f, 17f); lineTo(19f, 17f); close()
            moveTo(11f, 16f); lineTo(13f, 16f); lineTo(13f, 18f); lineTo(11f, 18f); close()
            moveTo(15f, 18f); lineTo(18f, 18f); lineTo(18f, 21f); lineTo(15f, 21f); close()
            moveTo(20f, 19f); lineTo(22f, 19f); lineTo(22f, 21f); lineTo(20f, 21f); close()
        }
    }.build()
}
