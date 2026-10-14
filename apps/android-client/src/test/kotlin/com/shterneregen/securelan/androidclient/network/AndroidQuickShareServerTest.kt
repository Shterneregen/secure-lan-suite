package com.shterneregen.securelan.androidclient.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.Socket
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AndroidQuickShareServerTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test(timeout = 10_000)
    fun unicodeTextIsDeliveredOnceAndThenUnavailable() {
        server().use { server ->
            val item = server.createText("Привет 🌍", 1, 1)
            val response = request(server, "/s/${item.id}/text")
            assertTrue(response.startsWith("HTTP/1.1 200 OK"))
            assertEquals("Привет 🌍", response.substringAfter("\r\n\r\n"))
            assertTrue(request(server, "/s/${item.id}/text").startsWith("HTTP/1.1 410 Gone"))
            assertTrue(server.items().isEmpty())
        }
    }

    @Test(timeout = 10_000)
    fun concurrentCreationKeepsEveryShare() {
        val workers = Executors.newFixedThreadPool(4)
        try {
            server().use { server ->
                val results = workers.invokeAll(List(20) { Callable { server.createText("value", 1, null).id } })
                assertEquals(20, results.map { it.get(3, TimeUnit.SECONDS) }.distinct().size)
                assertEquals(20, server.items().size)
            }
        } finally {
            workers.shutdownNow()
        }
    }

    @Test(timeout = 10_000)
    fun concurrentRequestsCannotExceedAccessLimit() {
        val workers = Executors.newFixedThreadPool(4)
        try {
            server().use { server ->
                val share = server.createText("value", 1, 1)
                val results = workers.invokeAll(List(8) { Callable { request(server, "/s/${share.id}/text") } })
                assertEquals(1, results.count { it.get(3, TimeUnit.SECONDS).startsWith("HTTP/1.1 200 OK") })
            }
        } finally {
            workers.shutdownNow()
        }
    }

    @Test(timeout = 10_000)
    fun closeStopsListenerAndPreventsNewShares() {
        val server = server()
        server.createText("value", 1, null)
        val port = server.port
        server.close()
        server.close()
        assertTrue(server.urls.isEmpty())
        assertTrue(server.items().isEmpty())
        assertTrue(runCatching { server.createText("late", 1, null) }.isFailure)
        assertTrue(runCatching { Socket("127.0.0.1", port).use { } }.isFailure)
    }

    @Test
    fun invalidOrOverflowingExpirationDoesNotPublishShare() {
        server().use { server ->
            assertTrue(runCatching { server.createText("value", 0, null) }.isFailure)
            assertTrue(runCatching { server.createText("value", Long.MAX_VALUE, null) }.isFailure)
            assertTrue(runCatching { server.createText("value", 1, 0) }.isFailure)
            assertTrue(server.items().isEmpty())
            assertFalse(server.urls.isNotEmpty())
        }
    }

    @Test(timeout = 15_000)
    fun cleanupDoesNotDeleteTheFileUntilTheAdmittedDownloadCloses() {
        server().use { server ->
            val payload = ByteArray(4 * 1024 * 1024) { (it % 251).toByte() }
            val item = server.createFile("payload.bin", 1, 1) { payload.inputStream() }
            Socket().use { socket ->
                socket.receiveBufferSize = 16 * 1024
                socket.connect(java.net.InetSocketAddress("127.0.0.1", server.port))
                socket.soTimeout = 5000
                socket.getOutputStream().write("GET /s/${item.id}/download HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray())
                val input = socket.getInputStream()
                val header = StringBuilder()
                while (!header.endsWith("\r\n\r\n")) { val next = input.read(); check(next >= 0); header.append(next.toChar()) }
                assertTrue(header.startsWith("HTTP/1.1 200 OK"))
                assertTrue(server.items().isEmpty())
                assertTrue(temporary.root.listFiles().orEmpty().any { it.name.startsWith("quick-share-") })
                org.junit.Assert.assertArrayEquals(payload, input.readBytes())
            }
            val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            while (System.nanoTime() < end && temporary.root.listFiles().orEmpty().any { it.name.startsWith("quick-share-") }) Thread.sleep(10)
            assertFalse(temporary.root.listFiles().orEmpty().any { it.name.startsWith("quick-share-") })
        }
    }

    @Test(timeout = 10_000)
    fun malformedAndRevokedPathsNeverReturnPayload() {
        server().use { server ->
            val share = server.createText("secret", 1, null)
            assertTrue(request(server, "/s/${share.id}/text/extra").startsWith("HTTP/1.1 404"))
            server.stopShare(share.id)
            assertFalse(request(server, "/s/${share.id}/text").substringAfter("\r\n\r\n").contains("secret"))
            val replacement = server.createText("new", 1, null)
            assertFalse(share.id == replacement.id)
        }
    }

    private fun server() = AndroidQuickShareServer(temporary.root) { error("Text shares do not read documents") }

    private fun request(server: AndroidQuickShareServer, path: String): String = Socket("127.0.0.1", server.port).use { socket ->
        socket.soTimeout = 3_000
        socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray(Charsets.US_ASCII))
        socket.getOutputStream().flush()
        socket.getInputStream().bufferedReader(Charsets.UTF_8).readText()
    }
}
