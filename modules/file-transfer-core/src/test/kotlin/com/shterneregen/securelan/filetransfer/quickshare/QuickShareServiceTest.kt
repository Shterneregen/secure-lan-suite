package com.shterneregen.securelan.filetransfer.quickshare

import com.shterneregen.securelan.filetransfer.quickshare.impl.DefaultQuickShareService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

class QuickShareServiceTest {
    @Test
    fun shouldServeTextShareAndEnforceAccessLimit() {
        val events = CopyOnWriteArrayList<String>()
        val service = DefaultQuickShareService(QuickShareEventPublisher { event -> events.add(event.message()) })
        service.start(QuickShareServerConfig(findAvailablePort(), listOf("127.0.0.1")))
        try {
            val snapshot = service.share(QuickShareCreateRequest.text("hello from LAN", "Greeting", Duration.ofMinutes(5), 1))
            val client = HttpClient.newHttpClient()

            val first = client.send(
                HttpRequest.newBuilder(URI.create(snapshot.primaryUrl())).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, first.statusCode())
            assertTrue(first.body().contains("hello from LAN"))

            val second = client.send(
                HttpRequest.newBuilder(URI.create(snapshot.primaryUrl())).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(410, second.statusCode())
            assertEquals(QuickShareStatus.LIMIT_REACHED, service.findShare(snapshot.id()).orElseThrow().status())
            assertTrue(events.any { it.contains("Text share opened") })
        } finally {
            service.stop()
        }
    }

    @Test
    fun shouldServeFileDownloadAndKeepPageViewFree() {
        val sourceFile = Files.createTempFile("quick-share", ".txt")
        Files.writeString(sourceFile, "download payload")

        val service = DefaultQuickShareService(QuickShareEventPublisher.noOp())
        service.start(QuickShareServerConfig(findAvailablePort(), listOf("127.0.0.1")))
        try {
            val snapshot = service.share(QuickShareCreateRequest.file(sourceFile, "Payload", Duration.ofMinutes(5), 1))
            val client = HttpClient.newHttpClient()

            val page = client.send(
                HttpRequest.newBuilder(URI.create(snapshot.primaryUrl())).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, page.statusCode())
            assertEquals(0, service.findShare(snapshot.id()).orElseThrow().accessCount())

            val download = client.send(
                HttpRequest.newBuilder(URI.create(snapshot.primaryUrl() + "/download")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, download.statusCode())
            assertEquals("download payload", download.body())
            assertEquals(QuickShareStatus.LIMIT_REACHED, service.findShare(snapshot.id()).orElseThrow().status())
        } finally {
            service.stop()
        }
    }

    @Test
    fun shouldStopShareManually() {
        val service = DefaultQuickShareService(QuickShareEventPublisher.noOp())
        service.start(QuickShareServerConfig(findAvailablePort(), listOf("127.0.0.1")))
        try {
            val snapshot = service.share(QuickShareCreateRequest.text("manual stop", "Manual", Duration.ofMinutes(5), 2))
            assertTrue(service.stopShare(snapshot.id()))
            assertFalse(service.findShare(snapshot.id()).orElseThrow().active())
            assertEquals(QuickShareStatus.STOPPED, service.findShare(snapshot.id()).orElseThrow().status())
        } finally {
            service.stop()
        }
    }

    private fun findAvailablePort(): Int = ServerSocket(0).use { it.localPort }
}
