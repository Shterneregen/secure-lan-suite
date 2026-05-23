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

    @JvmStatic
    fun formatServerStartedMessage(port: Int): String = "[quick-share] server started on port $port"

    @JvmStatic
    fun formatServerStoppedMessage(): String = "[quick-share] server stopped"

    @JvmStatic
    fun formatLandingUrlsDiagnostics(landingUrls: List<String>): String = "[quick-share] landing URLs: ${landingUrls.joinToString(", ")}"

    @JvmStatic
    fun formatFileLinkCopiedMessage(url: String): String = "[quick-share] file link copied: $url"

    @JvmStatic
    fun formatTextLinkCopiedMessage(url: String): String = "[quick-share] text link copied: $url"

    @JvmStatic
    fun formatIndexLinkCopiedMessage(url: String): String = "[quick-share] index link copied: $url"

    @JvmStatic
    fun formatLinkCopiedMessage(url: String): String = "[quick-share] link copied: $url"

    @JvmStatic
    fun formatEventDiagnostics(message: String, remoteAddress: String): String =
        "[quick-share] $message" + if (remoteAddress.isBlank()) "" else " from $remoteAddress"

    private fun enumLabel(value: Enum<*>): String = value.name.lowercase(Locale.ROOT).replace('_', ' ')

    private const val MAX_TEXT_DISPLAY_NAME_LENGTH = 32
}
