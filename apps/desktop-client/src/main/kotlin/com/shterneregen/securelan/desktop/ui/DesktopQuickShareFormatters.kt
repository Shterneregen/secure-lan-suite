package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import java.net.URI
import java.util.Locale

/** Presentation and diagnostic formatting for temporary, unauthenticated LAN HTTP shares. */
object DesktopQuickShareFormatters {
    @JvmStatic
    fun formatSnapshotMeta(snapshot: QuickShareSnapshot): String =
        enumLabel(snapshot.type()) +
            " — " + enumLabel(snapshot.status()) +
            " — " + (snapshot.accessLimit()?.let { "${snapshot.accessCount()}/$it" }
                ?: "${snapshot.accessCount()} opens, unlimited") +
            " — " + (snapshot.expiresAt()?.let { "expires $it" } ?: "until stopped")

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
    fun formatServerStatus(): String = "Quick share is active"

    @JvmStatic
    fun formatLandingValue(landingUrls: List<String>): String =
        if (landingUrls.isEmpty()) "No local address detected. Check your network or firewall." else "Index: ${landingUrls.joinToString(" • ")}"

    @JvmStatic
    fun formatLandingUrlsDiagnostics(landingUrls: List<String>): String = "[quick-share] landing URLs: ${landingUrls.joinToString(", ")}"

    @JvmStatic
    fun formatFileLinkCopiedMessage(url: String): String = "[system] Quick Share file link is ready: $url"

    @JvmStatic
    fun formatTextLinkCopiedMessage(url: String): String = "[system] Quick Share text link is ready: $url"

    @JvmStatic
    fun formatIndexLinkCopiedMessage(url: String): String = "[system] Quick Share index link copied: $url"

    @JvmStatic
    fun formatLinkCopiedMessage(url: String): String = "[system] Quick Share link copied: $url"

    @JvmStatic
    fun pickPrimaryLandingUrl(landingUrls: List<String>, preferredHost: String? = null): String {
        if (landingUrls.isEmpty()) return ""
        if (!preferredHost.isNullOrBlank()) {
            landingUrls.firstOrNull { urlHost(it) == preferredHost }?.let { return it }
        }
        return landingUrls.minByOrNull { url ->
            val host = urlHost(url).orEmpty()
            when {
                host.startsWith("192.168.") -> 0
                host.startsWith("10.") -> 1
                host.startsWith("172.") -> 2
                host == "127.0.0.1" -> 4
                else -> 3
            }
        } ?: landingUrls.first()
    }

    @JvmStatic
    fun preferQuickShareUrl(snapshot: QuickShareSnapshot, preferredHost: String? = null): String {
        val primary = snapshot.primaryUrl()
        if (primary.isBlank()) return primary
        val preferredBase = pickPrimaryLandingUrl(snapshot.urls().mapNotNull(::extractUrlBase), preferredHost)
        if (preferredBase.isBlank()) return primary
        return replaceUrlBase(primary, preferredBase)
    }

    private fun urlHost(url: String): String? = runCatching { URI(url).host }.getOrNull()

    private fun extractUrlBase(url: String): String? =
        try {
            val uri = URI(url)
            val scheme = uri.scheme
            val authority = uri.authority
            if (scheme.isNullOrBlank() || authority.isNullOrBlank()) null else "$scheme://$authority/"
        } catch (_: Exception) {
            null
        }

    private fun replaceUrlBase(originalUrl: String, newBase: String): String =
        try {
            val uri = URI(originalUrl)
            val path = uri.rawPath ?: ""
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val fragment = uri.rawFragment?.let { "#$it" } ?: ""
            newBase.removeSuffix("/") + path + query + fragment
        } catch (_: Exception) {
            originalUrl
        }

    /**
     * Formats lifecycle metadata without logging share names, IDs, or URLs derived from shared text.
     * Server events without a snapshot retain the service's operational message.
     */
    @JvmStatic
    fun formatEventDiagnostics(event: QuickShareEvent): String {
        val detail = event.snapshot()?.let(::formatSnapshotMeta) ?: event.message()
        val remoteAddress = event.remoteAddress()
        return "[quick-share] $detail" + if (remoteAddress.isBlank()) "" else " from $remoteAddress"
    }

    private fun enumLabel(value: Enum<*>): String = value.name.lowercase(Locale.ROOT).replace('_', ' ')

    private const val MAX_TEXT_DISPLAY_NAME_LENGTH = 32
}
