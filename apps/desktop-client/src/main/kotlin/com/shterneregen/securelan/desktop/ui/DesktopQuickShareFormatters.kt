package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import java.util.Locale

object DesktopQuickShareFormatters {
    @JvmStatic
    fun formatSnapshotMeta(snapshot: QuickShareSnapshot): String =
        enumLabel(snapshot.type()) +
            " — " + enumLabel(snapshot.status()) +
            " — " + snapshot.accessCount() + "/" + snapshot.accessLimit() +
            " — expires " + snapshot.expiresAt()

    @JvmStatic
    fun formatTextDisplayName(text: String): String {
        val firstLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
        return when {
            firstLine.isBlank() -> "shared-text"
            firstLine.length > MAX_TEXT_DISPLAY_NAME_LENGTH -> firstLine.substring(0, MAX_TEXT_DISPLAY_NAME_LENGTH)
            else -> firstLine
        }
    }

    @JvmStatic
    fun formatServerStatus(port: Int): String = "Quick share running on port $port"

    @JvmStatic
    fun formatLandingValue(landingUrls: List<String>): String =
        if (landingUrls.isEmpty()) "No LAN URL detected. Check network adapter/firewall." else "Index: ${landingUrls.joinToString(" • ")}"

    private fun enumLabel(value: Enum<*>): String = value.name.lowercase(Locale.ROOT).replace('_', ' ')

    private const val MAX_TEXT_DISPLAY_NAME_LENGTH = 32
}
