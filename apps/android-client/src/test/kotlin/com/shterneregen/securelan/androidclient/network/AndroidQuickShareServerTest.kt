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

    private fun server() = AndroidQuickShareServer(temporary.root) { error("Text shares do not read documents") }

    private fun request(server: AndroidQuickShareServer, path: String): String = Socket("127.0.0.1", server.port).use { socket ->
        socket.soTimeout = 3_000
        socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray(Charsets.US_ASCII))
        socket.getOutputStream().flush()
        socket.getInputStream().bufferedReader(Charsets.UTF_8).readText()
    }
}
