package com.shterneregen.securelan.filetransfer.quickshare.impl

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher
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

class DefaultQuickShareService @JvmOverloads constructor(
    eventPublisher: QuickShareEventPublisher?,
    private val clock: Clock = Clock.systemUTC(),
) : QuickShareService {
    private val eventPublisher: QuickShareEventPublisher = eventPublisher ?: QuickShareEventPublisher.noOp()
    private val lifecycleLock = Object()
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
            val createdExecutor = Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "securelan-quick-share-http-${threadSequence.incrementAndGet()}").apply { isDaemon = true }
            }
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
            createdExpiryExecutor.scheduleAtFixedRate(this::expireInactiveShares, 1, 1, TimeUnit.SECONDS)
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
        var candidate = base
        var index = 2
        while (items.containsKey(candidate)) {
            candidate = "$base-$index"
            index++
        }
        return candidate
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
        try {
            if (!"GET".equals(exchange.requestMethod, ignoreCase = true)) {
                sendText(exchange, 405, "Method Not Allowed", "text/plain; charset=utf-8")
                return
            }
            val path = exchange.requestURI.path
            if ("/" == path || path.isBlank()) {
                sendText(exchange, 200, indexHtml(), "text/html; charset=utf-8")
                return
            }
            val sharePath = parseSharePath(path.split("/").toTypedArray())
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
        } finally {
            exchange.close()
        }
    }

    private fun parseSharePath(parts: Array<String>): ParsedSharePath? = when {
        parts.size == 2 && parts[1].isNotBlank() ->
            ParsedSharePath(URLDecoder.decode(parts[1], StandardCharsets.UTF_8), "")
        parts.size == 3 && "s" == parts[1] ->
            ParsedSharePath(URLDecoder.decode(parts[2], StandardCharsets.UTF_8), "")
        parts.size == 3 && parts[1].isNotBlank() ->
            ParsedSharePath(URLDecoder.decode(parts[1], StandardCharsets.UTF_8), parts[2])
        parts.size == 4 && "s" == parts[1] ->
            ParsedSharePath(URLDecoder.decode(parts[2], StandardCharsets.UTF_8), parts[3])
        else -> null
    }

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
        exchange.responseBody.use { outputStream -> Files.copy(item.file!!, outputStream) }
    }

    private fun indexHtml(): String {
        val activeShares = shares().filter { it.active() }
        val body = StringBuilder(baseHtmlStart("SecureLanSuite quick shares"))
        body.append("<h1>SecureLanSuite quick shares</h1>")
        body.append("<p class='warn'>No authorization is required. Anyone on this LAN who opens this page can access active shares.</p>")
        if (activeShares.isEmpty()) {
            body.append("<p>No active shares.</p>")
        } else {
            body.append("<ul>")
            for (snapshot in activeShares) {
                body.append("<li><a href='/").append(escapeHtml(snapshot.id())).append("'>")
                    .append(escapeHtml(snapshot.displayName()))
                    .append("</a> <span>")
                    .append(escapeHtml(snapshot.type().name.lowercase(Locale.ROOT)))
                    .append(" — ").append(snapshot.accessCount()).append("/").append(snapshot.accessLimit())
                    .append(" accesses</span></li>")
            }
            body.append("</ul>")
        }
        body.append(baseHtmlEnd())
        return body.toString()
    }

    private fun fileItemHtml(snapshot: QuickShareSnapshot): String = baseHtmlStart(snapshot.displayName()) +
        "<h1>${escapeHtml(snapshot.displayName())}</h1>" +
        "<p>File: <strong>${escapeHtml(snapshot.fileName())}</strong></p>" +
        "<p>Size: ${snapshot.fileSize()} bytes</p>" +
        "<p>Accesses: ${snapshot.accessCount()} / ${snapshot.accessLimit()}</p>" +
        "<a class='button' href='/${escapeHtml(snapshot.id())}/download'>Download file</a>" +
        "<p><a href='/'>Back to active shares</a></p>" +
        baseHtmlEnd()

    private fun textItemHtml(snapshot: QuickShareSnapshot, text: String): String = baseHtmlStart(snapshot.displayName()) +
        "<h1>${escapeHtml(snapshot.displayName())}</h1>" +
        "<p>Accesses: ${snapshot.accessCount()} / ${snapshot.accessLimit()}</p>" +
        "<textarea id='sharedText' readonly>${escapeHtml(text)}</textarea>" +
        "<p><button class='button' onclick='navigator.clipboard.writeText(document.getElementById(\"sharedText\").value)'>Copy text</button></p>" +
        "<p><a href='/'>Back to active shares</a></p>" +
        baseHtmlEnd()

    private fun unavailableHtml(snapshot: QuickShareSnapshot?): String {
        val status = snapshot?.status()?.name?.lowercase(Locale.ROOT)?.replace('_', ' ') ?: "unavailable"
        return baseHtmlStart("Share unavailable") +
            "<h1>Share unavailable</h1>" +
            "<p>This share is ${escapeHtml(status)}.</p>" +
            "<p><a href='/'>Back to active shares</a></p>" +
            baseHtmlEnd()
    }

    private fun notFoundHtml(): String = baseHtmlStart("Not found") +
        "<h1>Not found</h1><p>The requested share does not exist.</p><p><a href='/'>Back to active shares</a></p>" +
        baseHtmlEnd()

    private fun baseHtmlStart(title: String): String =
        "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>${escapeHtml(title)}</title>" +
            "<style>body{font-family:system-ui,sans-serif;max-width:820px;margin:32px auto;padding:0 16px;background:#0f172a;color:#e2e8f0}" +
            "a{color:#93c5fd}.button,button{display:inline-block;background:#2563eb;color:white;border:0;border-radius:8px;padding:10px 14px;text-decoration:none;cursor:pointer}" +
            "textarea{width:100%;min-height:220px;border-radius:8px;padding:12px;background:#020617;color:#e2e8f0;border:1px solid #334155}" +
            ".warn{background:#451a03;color:#fed7aa;padding:10px 12px;border-radius:8px}li{margin:10px 0}</style></head><body>"

    private fun baseHtmlEnd(): String = "</body></html>"

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
        val fileName = if (request.type() == QuickShareType.FILE) file!!.fileName.toString() else ""
        val fileSize = if (request.type() == QuickShareType.FILE) {
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
            now.plus(request.expiresAfter()),
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
        val result = if (siteLocalAddresses.isNotEmpty()) siteLocalAddresses else otherNonLoopbackAddresses
        result.sortWith(Comparator.naturalOrder())
        return result.distinct()
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun contentDispositionFileName(value: String): String = value
        .replace("\\", "_")
        .replace("\"", "_")
        .replace("\r", "_")
        .replace("\n", "_")

    private data class ParsedSharePath(val id: String, val action: String)

    private data class AccessResult(val allowed: Boolean, val snapshot: QuickShareSnapshot)

    private inner class SharedItem(
        private val id: String,
        val type: QuickShareType,
        private val displayName: String,
        val file: Path?,
        val fileName: String,
        val fileSize: Long,
        val text: String,
        private val createdAt: Instant,
        private val expiresAt: Instant,
        private val accessLimit: Int,
        private val urls: List<String>,
    ) {
        private var accessCount = 0
        private var status = QuickShareStatus.ACTIVE

        @Synchronized
        fun registerAccess(now: Instant): AccessResult {
            expireIfNeeded(now)
            if (status != QuickShareStatus.ACTIVE) return AccessResult(false, snapshot())
            if (accessCount >= accessLimit) {
                status = QuickShareStatus.LIMIT_REACHED
                return AccessResult(false, snapshot())
            }
            accessCount++
            if (accessCount >= accessLimit) status = QuickShareStatus.LIMIT_REACHED
            return AccessResult(true, snapshot())
        }

        @Synchronized
        fun snapshotAfterExpiry(now: Instant): QuickShareSnapshot {
            expireIfNeeded(now)
            return snapshot()
        }

        @Synchronized
        fun expireIfNeeded(now: Instant): QuickShareSnapshot? {
            if (status == QuickShareStatus.ACTIVE && !now.isBefore(expiresAt)) {
                status = QuickShareStatus.EXPIRED
                return snapshot()
            }
            return null
        }

        @Synchronized
        fun stop(): QuickShareSnapshot {
            status = QuickShareStatus.STOPPED
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
