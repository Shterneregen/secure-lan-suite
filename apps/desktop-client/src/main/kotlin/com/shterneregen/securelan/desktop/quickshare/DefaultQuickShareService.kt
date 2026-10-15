package com.shterneregen.securelan.desktop.quickshare

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareHtml
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.Comparator
import java.util.Locale
import java.util.Objects
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Default implementation of [QuickShareService] backed by the JDK's built-in [HttpServer].
 *
 * Hosts temporary files and text snippets accessible over standard HTTP GET requests
 * within the local network. Handles time-to-live expiration via a background scheduled task
 * and enforces maximum download limits.
 *
 * @param eventPublisher Optional publisher for lifecycle events; defaults to [QuickShareEventPublisher.noOp].
 * @param clock Time source for measuring share creation, access, and expiration (defaults to UTC system clock).
 */
class DefaultQuickShareService @JvmOverloads constructor(
    eventPublisher: QuickShareEventPublisher?,
    private val clock: Clock = Clock.systemUTC(),
) : QuickShareService {
    private val eventPublisher: QuickShareEventPublisher = eventPublisher ?: QuickShareEventPublisher.noOp()
    private val lifecycleLock = Any()
    private val running = AtomicBoolean(false)
    private val threadSequence = AtomicLong(0)
    private val items: MutableMap<String, SharedItem> = ConcurrentHashMap()

    @Volatile
    private var server: HttpServer? = null

    @Volatile
    private var executor: ExecutorService? = null

    @Volatile
    private var expiryExecutor: ScheduledExecutorService? = null

    @Volatile
    private var activePort = 0

    @Volatile
    private var advertisedHosts: List<String> = listOf("127.0.0.1")

    init {
        Objects.requireNonNull(clock, "clock must not be null")

    }

    @Throws(IOException::class)
    override fun start(config: QuickShareServerConfig) {
        Objects.requireNonNull(config, "config must not be null")
        synchronized(lifecycleLock) {
            if (running.get()) return
            val createdServer = createHttpServer(config.port())
            val createdExecutor = java.util.concurrent.ThreadPoolExecutor(
                8, 8, 0, TimeUnit.SECONDS, java.util.concurrent.ArrayBlockingQueue(32),
                { runnable -> Thread(runnable, "securelan-quick-share-http-${threadSequence.incrementAndGet()}").apply { isDaemon = true } },
            )
            val createdExpiryExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "securelan-quick-share-expiry").apply { isDaemon = true }
            }
            createdServer.createContext("/", this::handleHttpExchange)
            createdServer.executor = createdExecutor

            server = createdServer
            executor = createdExecutor
            expiryExecutor = createdExpiryExecutor
            activePort = createdServer.address.port
            advertisedHosts = resolveAdvertisedHosts(config.advertisedHosts())
            running.set(true)
            createdServer.start()
            createdExpiryExecutor.scheduleWithFixedDelay(this::expireInactiveShares, 1, 1, TimeUnit.SECONDS)
            eventPublisher.publish(QuickShareEvent("", null, "Quick-share server started on port $activePort", ""))
        }
    }

    @Throws(IOException::class)
    private fun createHttpServer(requestedPort: Int): HttpServer {
        if (requestedPort == 0) {
            return HttpServer.create(InetSocketAddress("0.0.0.0", 0), 32)
        }
        var lastError: IOException? = null
        for (offset in 0..PORT_RETRY_COUNT) {
            val candidate = requestedPort + offset
            if (candidate > 65_535) break
            try {
                return HttpServer.create(InetSocketAddress("0.0.0.0", candidate), 32)
            } catch (ex: IOException) {
                lastError = ex
            }
        }
        throw lastError ?: IOException("Unable to bind quick-share server")
    }

    override fun stop() {
        synchronized(lifecycleLock) {
            running.set(false)
            items.values.forEach { it.stop() }
            items.clear()
            server?.stop(0)
            executor?.shutdownNow()
            expiryExecutor?.shutdownNow()
            server = null
            executor = null
            expiryExecutor = null
            activePort = 0
            advertisedHosts = listOf("127.0.0.1")
            eventPublisher.publish(QuickShareEvent("", null, "Quick-share server stopped", ""))
        }
    }

    override fun isRunning(): Boolean = running.get()

    override fun port(): Int = activePort

    @Throws(IOException::class)
    override fun share(request: QuickShareCreateRequest): QuickShareSnapshot {
        Objects.requireNonNull(request, "request must not be null")
        check(running.get()) { "Quick-share server is not running" }
        synchronized(lifecycleLock) {
            check(running.get()) { "Quick-share server is not running" }
            items.entries.removeIf { !it.value.snapshotAfterExpiry(clock.instant()).active() }
            check(items.size < 100) { "Quick-share publication limit reached" }
            val id = uniqueId(request.displayName())
            val item = createSharedItem(id, request, clock.instant(), urlsFor(id))
            items[id] = item
            val snapshot = item.snapshot()
            eventPublisher.publish(QuickShareEvent(id, snapshot, "Quick-share created: ${snapshot.displayName()}", ""))
            return snapshot
        }
    }

    private fun uniqueId(displayName: String): String {
        val base = sanitizeId(displayName)
        // Revocation must survive expiry/stop and publishing another item with the same name.
        return "$base-${java.util.UUID.randomUUID()}"
    }

    private fun sanitizeId(value: String): String {
        var sanitized = value.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .replace(Regex("(^-+|-+$)"), "")
        if (sanitized.isBlank()) sanitized = "share"
        return if (sanitized.length > 48) sanitized.substring(0, 48).replace(Regex("-+$"), "") else sanitized
    }

    override fun findShare(id: String): Optional<QuickShareSnapshot> {
        val item = items[id] ?: return Optional.empty()
        item.expireIfNeeded(clock.instant())
        return Optional.of(item.snapshot())
    }

    override fun shares(): List<QuickShareSnapshot> {
        val now = clock.instant()
        return items.values
            .onEach { it.expireIfNeeded(now) }
            .map { it.snapshot() }
            .sortedWith(compareByDescending { it.createdAt() })
    }

    override fun stopShare(id: String): Boolean {
        val item = items[id] ?: return false
        val snapshot = item.stop()
        eventPublisher.publish(QuickShareEvent(id, snapshot, "Quick-share stopped: ${snapshot.displayName()}", ""))
        return true
    }

    override fun landingUrls(): List<String> {
        if (!running.get()) return listOf()
        return advertisedHosts.map { host -> "http://$host:$activePort/" }
    }

    private fun urlsFor(id: String): List<String> = advertisedHosts.map { host -> "http://$host:$activePort/$id" }

    private fun expireInactiveShares() {
        val now = clock.instant()
        for (item in items.values) {
            val snapshot = item.expireIfNeeded(now)
            if (snapshot != null) {
                eventPublisher.publish(
                    QuickShareEvent(snapshot.id(), snapshot, "Quick-share expired: ${snapshot.displayName()}", ""),
                )
            }
        }
    }

    @Throws(IOException::class)
    private fun handleHttpExchange(exchange: HttpExchange) {
        exchange.use {
            if (!"GET".equals(exchange.requestMethod, ignoreCase = true)) {
                sendText(exchange, 405, "Method Not Allowed", "text/plain; charset=utf-8")
                return
            }
            val path = exchange.requestURI.path
            if ("/" == path || path.isBlank()) {
                sendText(exchange, 200, indexHtml(), "text/html; charset=utf-8")
                return
            }
            val sharePath = parseSharePath(path.split("/"))
            if (sharePath == null) {
                sendText(exchange, 404, notFoundHtml(), "text/html; charset=utf-8")
                return
            }
            when {
                sharePath.action.isBlank() -> handleItemPage(exchange, sharePath.id)
                "download" == sharePath.action -> handleDownload(exchange, sharePath.id)
                "text" == sharePath.action -> handleText(exchange, sharePath.id)
                else -> sendText(exchange, 404, notFoundHtml(), "text/html; charset=utf-8")
            }
        }
    }

    private fun parseSharePath(parts: List<String>): ParsedSharePath? = when (parts.size) {
        2 -> if (parts[1].isNotBlank()) ParsedSharePath(decodeUrl(parts[1]), "") else null
        3 -> when {
            "s" == parts[1] -> ParsedSharePath(decodeUrl(parts[2]), "")
            parts[1].isNotBlank() -> ParsedSharePath(decodeUrl(parts[1]), parts[2])
            else -> null
        }
        4 -> if ("s" == parts[1]) ParsedSharePath(decodeUrl(parts[2]), parts[3]) else null
        else -> null
    }

    private fun decodeUrl(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    @Throws(IOException::class)
    private fun handleItemPage(exchange: HttpExchange, id: String) {
        val item = items[id]
        if (item == null) {
            sendText(exchange, 404, notFoundHtml(), "text/html; charset=utf-8")
            return
        }
        if (item.type == QuickShareType.TEXT) {
            val access = item.registerAccess(clock.instant())
            if (!access.allowed) {
                sendText(exchange, 410, unavailableHtml(access.snapshot), "text/html; charset=utf-8")
                return
            }
            eventPublisher.publish(
                QuickShareEvent(id, access.snapshot, "Text share opened: ${access.snapshot.displayName()}", exchange.remoteAddress.toString()),
            )
            sendText(exchange, 200, textItemHtml(access.snapshot, item.text), "text/html; charset=utf-8")
            return
        }
        val snapshot = item.snapshotAfterExpiry(clock.instant())
        if (!snapshot.active()) {
            sendText(exchange, 410, unavailableHtml(snapshot), "text/html; charset=utf-8")
            return
        }
        sendText(exchange, 200, fileItemHtml(snapshot), "text/html; charset=utf-8")
    }

    @Throws(IOException::class)
    private fun handleText(exchange: HttpExchange, id: String) {
        val item = items[id]
        if (item == null || item.type != QuickShareType.TEXT) {
            sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8")
            return
        }
        val access = item.registerAccess(clock.instant())
        if (!access.allowed) {
            sendText(exchange, 410, "Share is no longer available", "text/plain; charset=utf-8")
            return
        }
        eventPublisher.publish(
            QuickShareEvent(id, access.snapshot, "Text share opened: ${access.snapshot.displayName()}", exchange.remoteAddress.toString()),
        )
        sendText(exchange, 200, item.text, "text/plain; charset=utf-8")
    }

    @Throws(IOException::class)
    private fun handleDownload(exchange: HttpExchange, id: String) {
        val item = items[id]
        if (item == null || item.type != QuickShareType.FILE) {
            sendText(exchange, 404, "Not Found", "text/plain; charset=utf-8")
            return
        }
        val file = item.file
        if (file == null || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            sendText(exchange, 404, "File not found or unreadable", "text/plain; charset=utf-8")
            return
        }
        val access = item.registerAccess(clock.instant())
        if (!access.allowed) {
            sendText(exchange, 410, "Share is no longer available", "text/plain; charset=utf-8")
            return
        }
        eventPublisher.publish(
            QuickShareEvent(id, access.snapshot, "File downloaded: ${access.snapshot.displayName()}", exchange.remoteAddress.toString()),
        )

        val headers: Headers = exchange.responseHeaders
        headers["Content-Type"] = "application/octet-stream"
        headers["Content-Disposition"] = "attachment; filename=\"${contentDispositionFileName(item.fileName)}\""
        headers["Cache-Control"] = "no-store"
        exchange.sendResponseHeaders(200, item.fileSize)
        exchange.responseBody.use { outputStream -> Files.copy(file, outputStream) }
    }

    private fun indexHtml(): String = QuickShareHtml.indexHtml(shares())

    private fun fileItemHtml(snapshot: QuickShareSnapshot): String = QuickShareHtml.fileItemHtml(snapshot)

    private fun textItemHtml(snapshot: QuickShareSnapshot, text: String): String = QuickShareHtml.textItemHtml(snapshot, text)

    private fun unavailableHtml(snapshot: QuickShareSnapshot?): String = QuickShareHtml.unavailableHtml(snapshot)

    private fun notFoundHtml(): String = QuickShareHtml.notFoundHtml()

    @Throws(IOException::class)
    private fun sendText(exchange: HttpExchange, status: Int, value: String, contentType: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val headers: Headers = exchange.responseHeaders
        headers["Content-Type"] = contentType
        headers["Cache-Control"] = "no-store"
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { outputStream: OutputStream -> outputStream.write(bytes) }
    }

    private fun resolveAdvertisedHosts(configuredHosts: List<String>): List<String> {
        if (configuredHosts.isNotEmpty()) return configuredHosts
        try {
            val localHosts = resolveLocalLanIps()
            if (localHosts.isNotEmpty()) return localHosts
        } catch (_: Exception) {
            // Fallback below keeps localhost links available even when adapter inspection fails.
        }
        return listOf("127.0.0.1")
    }

    @Throws(IOException::class)
    private fun createSharedItem(id: String, request: QuickShareCreateRequest, now: Instant, urls: List<String>): SharedItem {
        val file = request.file()
        val fileName = if (request.type() == QuickShareType.FILE) file?.fileName?.toString() ?: "" else ""
        val fileSize = if (request.type() == QuickShareType.FILE && file != null) {
            Files.size(file)
        } else {
            request.text().toByteArray(StandardCharsets.UTF_8).size.toLong()
        }
        return SharedItem(
            id,
            request.type(),
            request.displayName(),
            file,
            fileName,
            fileSize,
            request.text(),
            now,
            request.expiresAfter()?.let(now::plus),
            request.accessLimit(),
            urls,
        )
    }

    @Throws(IOException::class)
    private fun resolveLocalLanIps(): List<String> {
        val siteLocalAddresses = ArrayList<String>()
        val otherNonLoopbackAddresses = ArrayList<String>()
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()
        while (networkInterfaces.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback || networkInterface.isVirtual) continue
            val inetAddresses = networkInterface.inetAddresses
            while (inetAddresses.hasMoreElements()) {
                val inetAddress: InetAddress = inetAddresses.nextElement()
                if (inetAddress !is Inet4Address || inetAddress.isLoopbackAddress) continue
                if (inetAddress.isSiteLocalAddress) {
                    siteLocalAddresses.add(inetAddress.hostAddress)
                } else {
                    otherNonLoopbackAddresses.add(inetAddress.hostAddress)
                }
            }
        }
        val result = siteLocalAddresses.ifEmpty { otherNonLoopbackAddresses }
        result.sortWith(Comparator.naturalOrder())
        return result.distinct()
    }

    private fun contentDispositionFileName(value: String): String = value
        .replace("\\", "_")
        .replace("\"", "_")
        .replace("\r", "_")
        .replace("\n", "_")

    private data class ParsedSharePath(val id: String, val action: String)

    private data class AccessResult(val allowed: Boolean, val snapshot: QuickShareSnapshot)

    private class SharedItem(
        private val id: String,
        val type: QuickShareType,
        private val displayName: String,
        val file: Path?,
        val fileName: String,
        val fileSize: Long,
        @Volatile var text: String,
        private val createdAt: Instant,
        private val expiresAt: Instant?,
        private val accessLimit: Int?,
        private val urls: List<String>,
    ) {
        private var accessCount = 0
        private var status = QuickShareStatus.ACTIVE

        @Synchronized
        fun registerAccess(now: Instant): AccessResult {
            expireIfNeeded(now)
            if (status != QuickShareStatus.ACTIVE) return AccessResult(false, snapshot())
            if (accessLimit != null && accessCount >= accessLimit) {
                status = QuickShareStatus.LIMIT_REACHED
                return AccessResult(false, snapshot())
            }
            accessCount++
            if (accessLimit != null && accessCount >= accessLimit) status = QuickShareStatus.LIMIT_REACHED
            return AccessResult(true, snapshot())
        }

        @Synchronized
        fun snapshotAfterExpiry(now: Instant): QuickShareSnapshot {
            expireIfNeeded(now)
            return snapshot()
        }

        @Synchronized
        fun expireIfNeeded(now: Instant): QuickShareSnapshot? {
            if (status == QuickShareStatus.ACTIVE && expiresAt != null && !now.isBefore(expiresAt)) {
                status = QuickShareStatus.EXPIRED
                text = ""
                return snapshot()
            }
            return null
        }

        @Synchronized
        fun stop(): QuickShareSnapshot {
            status = QuickShareStatus.STOPPED
            text = ""
            return snapshot()
        }

        @Synchronized
        fun snapshot(): QuickShareSnapshot = QuickShareSnapshot(
            id,
            type,
            displayName,
            fileName,
            fileSize,
            createdAt,
            expiresAt,
            accessLimit,
            accessCount,
            status,
            urls,
        )
    }

    companion object {
        private const val PORT_RETRY_COUNT = 20
    }
}
