package com.shterneregen.securelan.filetransfer.quickshare

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class QuickShareHtmlTest {
    @Test
    fun shouldEscapeDangerousHtmlCharactersInIndex() {
        val snapshot = QuickShareSnapshot(
            id = "share-xss",
            type = QuickShareType.TEXT,
            displayName = "<script>alert('xss')</script>",
            fileName = "",
            fileSize = 100L,
            createdAt = Instant.now(),
            expiresAt = null,
            accessLimit = null,
            accessCount = 0,
            status = QuickShareStatus.ACTIVE,
            urls = listOf("http://127.0.0.1:9090/share-xss"),
        )

        val html = QuickShareHtml.indexHtml(listOf(snapshot))

        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"))
    }

    @Test
    fun shouldRenderFileDownloadPage() {
        val snapshot = QuickShareSnapshot(
            id = "my-file",
            type = QuickShareType.FILE,
            displayName = "My Presentation",
            fileName = "slides.pdf",
            fileSize = 2048L,
            createdAt = Instant.now(),
            expiresAt = null,
            accessLimit = 5,
            accessCount = 1,
            status = QuickShareStatus.ACTIVE,
            urls = listOf("http://127.0.0.1:9090/my-file"),
        )

        val html = QuickShareHtml.fileItemHtml(snapshot)

        assertTrue(html.contains("My Presentation"))
        assertTrue(html.contains("slides.pdf"))
        assertTrue(html.contains("2048 bytes"))
        assertTrue(html.contains("1 / 5 accesses"))
        assertTrue(html.contains("/my-file/download"))
    }

    @Test
    fun shouldRenderTextSnippetPage() {
        val snapshot = QuickShareSnapshot(
            id = "my-text",
            type = QuickShareType.TEXT,
            displayName = "API Secret",
            fileName = "",
            fileSize = 16L,
            createdAt = Instant.now(),
            expiresAt = null,
            accessLimit = 1,
            accessCount = 1,
            status = QuickShareStatus.LIMIT_REACHED,
            urls = listOf("http://127.0.0.1:9090/my-text"),
        )

        val html = QuickShareHtml.textItemHtml(snapshot, "secret-token-xyz")

        assertTrue(html.contains("API Secret"))
        assertTrue(html.contains("secret-token-xyz"))
        assertTrue(html.contains("navigator.clipboard.writeText"))
    }

    @Test
    fun shouldRenderUnavailablePage() {
        val snapshot = QuickShareSnapshot(
            id = "expired-share",
            type = QuickShareType.FILE,
            displayName = "Expired File",
            fileName = "data.csv",
            fileSize = 100L,
            createdAt = Instant.now(),
            expiresAt = Instant.now(),
            accessLimit = null,
            accessCount = 0,
            status = QuickShareStatus.EXPIRED,
            urls = listOf(),
        )

        val html = QuickShareHtml.unavailableHtml(snapshot)

        assertTrue(html.contains("Share unavailable"))
        assertTrue(html.contains("expired"))
    }
}
