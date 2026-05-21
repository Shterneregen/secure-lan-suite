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
        assertEquals("Quick share running on port 8090", DesktopQuickShareFormatters.formatServerStatus(8090))
    }

    @Test
    fun shouldFormatEmptyLandingUrls() {
        assertEquals(
            "No LAN URL detected. Check network adapter/firewall.",
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

    private fun snapshot(
        type: QuickShareType,
        status: QuickShareStatus,
        accessCount: Int,
        accessLimit: Int,
    ): QuickShareSnapshot = QuickShareSnapshot(
        "share-1",
        type,
        "demo",
        "demo.txt",
        42,
        Instant.parse("2026-05-21T09:00:00Z"),
        Instant.parse("2026-05-21T10:00:00Z"),
        accessLimit,
        accessCount,
        status,
        listOf("http://127.0.0.1:8090/share-1"),
    )
}
