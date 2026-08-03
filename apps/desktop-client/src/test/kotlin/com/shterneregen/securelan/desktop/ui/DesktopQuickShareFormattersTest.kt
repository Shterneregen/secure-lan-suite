package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class DesktopQuickShareFormattersTest {
    @Test
    fun shouldFormatActiveFileShareMetadata() {
        val snapshot = snapshot(
            type = QuickShareType.FILE,
            status = QuickShareStatus.ACTIVE,
            accessCount = 1,
            accessLimit = 3,
        )

        assertEquals(
            "file — active — 1/3 — expires 2026-05-21T10:00:00Z",
            DesktopQuickShareFormatters.formatSnapshotMeta(snapshot),
        )
    }

    @Test
    fun shouldFormatUnderscoredStatusWithSpaces() {
        val snapshot = snapshot(
            type = QuickShareType.TEXT,
            status = QuickShareStatus.LIMIT_REACHED,
            accessCount = 3,
            accessLimit = 3,
        )

        assertEquals(
            "text — limit reached — 3/3 — expires 2026-05-21T10:00:00Z",
            DesktopQuickShareFormatters.formatSnapshotMeta(snapshot),
        )
    }

    @Test
    fun shouldFormatShareWithoutExpirationOrDownloadLimit() {
        val snapshot = snapshot(
            type = QuickShareType.TEXT,
            status = QuickShareStatus.ACTIVE,
            accessCount = 2,
            accessLimit = null,
            expiresAt = null,
        )

        assertEquals(
            "text — active — 2 opens, unlimited — until stopped",
            DesktopQuickShareFormatters.formatSnapshotMeta(snapshot),
        )
    }

    @Test
    fun shouldUseFirstLineAsTextDisplayName() {
        assertEquals("Hello world", DesktopQuickShareFormatters.formatTextDisplayName("Hello world\nsecond line"))
    }

    @Test
    fun shouldFallbackToSharedTextForBlankFirstLine() {
        assertEquals("shared-text", DesktopQuickShareFormatters.formatTextDisplayName("   \nsecond line"))
    }

    @Test
    fun shouldTruncateTextDisplayNameToThirtyTwoCharacters() {
        assertEquals(
            "12345678901234567890123456789012",
            DesktopQuickShareFormatters.formatTextDisplayName("1234567890123456789012345678901234567890"),
        )
    }

    @Test
    fun shouldFormatRunningServerStatus() {
        assertEquals("Quick share is active", DesktopQuickShareFormatters.formatServerStatus())
    }

    @Test
    fun shouldFormatEmptyLandingUrls() {
        assertEquals(
            "No local address detected. Check your network or firewall.",
            DesktopQuickShareFormatters.formatLandingValue(emptyList()),
        )
    }

    @Test
    fun shouldFormatLandingUrlsWithSeparator() {
        assertEquals(
            "Index: http://192.168.1.10:8090 • http://127.0.0.1:8090",
            DesktopQuickShareFormatters.formatLandingValue(listOf("http://192.168.1.10:8090", "http://127.0.0.1:8090")),
        )
    }

    @Test
    fun shouldFormatQuickShareDiagnostics() {
        assertEquals(
            "[quick-share] landing URLs: http://192.168.1.10:8090, http://127.0.0.1:8090",
            DesktopQuickShareFormatters.formatLandingUrlsDiagnostics(listOf("http://192.168.1.10:8090", "http://127.0.0.1:8090")),
        )
    }

    @Test
    fun shouldFormatQuickShareCopyMessages() {
        val url = "http://127.0.0.1:8090/share-1"

        assertEquals("[system] Quick Share file link is ready: $url", DesktopQuickShareFormatters.formatFileLinkCopiedMessage(url))
        assertEquals("[system] Quick Share text link is ready: $url", DesktopQuickShareFormatters.formatTextLinkCopiedMessage(url))
        assertEquals("[system] Quick Share index link copied: $url", DesktopQuickShareFormatters.formatIndexLinkCopiedMessage(url))
        assertEquals("[system] Quick Share link copied: $url", DesktopQuickShareFormatters.formatLinkCopiedMessage(url))
    }

    @Test
    fun shouldPrefer192LandingUrl() {
        assertEquals(
            "http://192.168.1.10:8090",
            DesktopQuickShareFormatters.pickPrimaryLandingUrl(
                listOf("http://172.17.0.1:8090", "http://192.168.1.10:8090", "http://127.0.0.1:8090"),
            ),
        )
    }

    @Test
    fun shouldFormatQuickShareEventDiagnostics() {
        assertEquals(
            "[quick-share] downloaded demo.txt from /192.168.1.10:54545",
            DesktopQuickShareFormatters.formatEventDiagnostics("downloaded demo.txt", "/192.168.1.10:54545"),
        )
        assertEquals(
            "[quick-share] share expired",
            DesktopQuickShareFormatters.formatEventDiagnostics("share expired", " "),
        )
    }

    @Test
    fun shouldPrefer192HostForShareUrl() {
        val snapshot = snapshot(
            type = QuickShareType.FILE,
            status = QuickShareStatus.ACTIVE,
            accessCount = 0,
            accessLimit = 3,
            urls = listOf(
                "http://172.17.0.1:8090/share-1",
                "http://192.168.1.10:8090/share-1",
                "http://127.0.0.1:8090/share-1",
            ),
        )

        assertEquals(
            "http://192.168.1.10:8090/share-1",
            DesktopQuickShareFormatters.preferQuickShareUrl(snapshot),
        )
    }

    @Test
    fun shouldNotDuplicatePathWhenReplacingShareUrlBase() {
        val snapshot = snapshot(
            type = QuickShareType.FILE,
            status = QuickShareStatus.ACTIVE,
            accessCount = 0,
            accessLimit = 3,
            urls = listOf(
                "http://172.17.240.1:5053/phase-11-context-panel-extension-md",
                "http://192.168.1.77:5053/phase-11-context-panel-extension-md",
            ),
        )

        assertEquals(
            "http://192.168.1.77:5053/phase-11-context-panel-extension-md",
            DesktopQuickShareFormatters.preferQuickShareUrl(snapshot),
        )
    }

    @Test
    fun shouldReturnPrimaryUrlWhenNoAlternativeUrls() {
        val snapshot = snapshot(
            type = QuickShareType.FILE,
            status = QuickShareStatus.ACTIVE,
            accessCount = 0,
            accessLimit = 3,
            urls = listOf("http://127.0.0.1:8090/share-1"),
        )

        assertEquals(
            "http://127.0.0.1:8090/share-1",
            DesktopQuickShareFormatters.preferQuickShareUrl(snapshot),
        )
    }

    @Test
    fun shouldReturnBlankPrimaryUrlWhenUrlsEmpty() {
        val snapshot = snapshot(
            type = QuickShareType.FILE,
            status = QuickShareStatus.ACTIVE,
            accessCount = 0,
            accessLimit = 3,
            urls = emptyList(),
        )

        assertEquals(
            "",
            DesktopQuickShareFormatters.preferQuickShareUrl(snapshot),
        )
    }

    private fun snapshot(
        type: QuickShareType,
        status: QuickShareStatus,
        accessCount: Int,
        accessLimit: Int?,
        expiresAt: Instant? = Instant.parse("2026-05-21T10:00:00Z"),
        urls: List<String> = listOf("http://127.0.0.1:8090/share-1"),
    ): QuickShareSnapshot = QuickShareSnapshot(
        "share-1",
        type,
        "demo",
        "demo.txt",
        42,
        Instant.parse("2026-05-21T09:00:00Z"),
        expiresAt,
        accessLimit,
        accessCount,
        status,
        urls,
    )
}
