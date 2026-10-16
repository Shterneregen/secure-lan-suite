package com.shterneregen.securelan.androidclient.network

import android.content.ContentResolver
import android.net.Uri
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareHtml
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** UI snapshot of a temporary HTTP share; [accessCount] counts content requests, not landing pages. */
data class AndroidQuickShareItem(
    val id: String, val name: String, val url: String, val isText: Boolean,
    val expiresAt: Long, val accessLimit: Int?, val accessCount: Int,
)

/**
 * Serves desktop-compatible Quick Share pages over unauthenticated HTTP on a trusted LAN.
 * File creation copies the provider stream to private cache and must run off the main thread.
 * Share creation and access accounting are synchronized; [close] permanently stops the listener,
 * clients and staged files.
 */
class AndroidQuickShareServer internal constructor(
    private val cacheDir: File,
    private val openInput: (Uri) -> InputStream?,
) : Closeable {
    constructor(resolver: ContentResolver, cacheDir: File) : this(cacheDir, resolver::openInputStream)

    init {
        synchronized(stagingLock) {
            cacheDir.listFiles()?.filter { it.isFile && it.name.startsWith("quick-share-") && it.name.endsWith(".tmp") && it !in ownedStaging }
                ?.forEach { it.delete() }
        }
    }

    private fun deleteStaging(file: File) = synchronized(stagingLock) {
        file.delete()
        ownedStaging.remove(file)
        Unit
    }

    private data class Share(val id: String, val name: String, val text: String?, val file: File?, val expiresAt: Long, val limit: Int?, @Volatile var accesses: Int = 0, val createdAt: Long = System.currentTimeMillis(), var readers: Int = 0, var revoked: Boolean = false)
    private val running = AtomicBoolean(false)
    private val shares = ConcurrentHashMap<String, Share>()
    private val clients = java.util.concurrent.ThreadPoolExecutor(0, 8, 30, java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.SynchronousQueue(),
        { runnable -> Thread(runnable, "securelan-quick-share-client").apply { isDaemon = true } })
    private val activeClients = ConcurrentHashMap.newKeySet<Socket>()
    private var closed = false
    @Volatile private var socket: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile var urls: List<String> = emptyList(); private set
    @Volatile var port: Int = 5053; private set

    @Synchronized fun start() {
        check(!closed) { "Quick Share server is closed" }
        if (running.get()) return
        var server: ServerSocket? = null
        for (p in 5053..5063) try { server = ServerSocket(p, 32, InetAddress.getByName("0.0.0.0")); break } catch (_: Exception) { }
        val listener = server ?: ServerSocket(0, 32, InetAddress.getByName("0.0.0.0"))
        socket = listener
        port = listener.localPort
        val hosts = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().flatMap { ni -> if (!ni.isUp || ni.isLoopback) emptyList() else ni.inetAddresses.toList() }
                .filter { it is java.net.Inet4Address && it.isSiteLocalAddress }.map { it.hostAddress }.distinct()
        }.getOrDefault(emptyList()).ifEmpty { listOf("127.0.0.1") }
        urls = hosts.map { "http://$it:$port/" }
        running.set(true)
        acceptThread = Thread {
            while (running.get()) {
                try {
                    val client = listener.accept()
                    synchronized(this@AndroidQuickShareServer) {
                        if (closed) client.close() else {
                            activeClients.add(client)
                            try { clients.execute { try { handle(client) } finally { activeClients.remove(client) } } }
                            catch (_: java.util.concurrent.RejectedExecutionException) { activeClients.remove(client); client.close() }
                        }
                    }
                }
                catch (_: Exception) { if (!running.get()) break }
            }
        }.also { it.name = "securelan-quick-share"; it.isDaemon = true; it.start() }
    }

    fun createText(text: String, minutes: Long, limit: Int?): AndroidQuickShareItem = add("shared-text", text, null, minutes, limit)
    fun createFile(uri: Uri, name: String, minutes: Long, limit: Int?): AndroidQuickShareItem {
        return createFile(name, minutes, limit) { openInput(uri) ?: error("Unable to read selected file") }
    }

    /** The stream supplier keeps provider IO outside lifecycle locks and allows bounded local verification. */
    internal fun createFile(name: String, minutes: Long, limit: Int?, input: () -> InputStream): AndroidQuickShareItem {
        if (!running.get()) start()
        check(shares.size < 100) { "Quick Share publication limit reached" }
        val target = synchronized(stagingLock) {
            File.createTempFile("quick-share-", ".tmp", cacheDir).also { ownedStaging.add(it) }
        }
        try {
            input().use { source -> target.outputStream().use { output ->
                com.shterneregen.securelan.androidclient.transfer.stageStream(source, output) {
                    check(running.get() && cacheDir.usableSpace > 128L * 1024 * 1024) { "Quick Share storage unavailable" }
                }
            } }
            return add(name.ifBlank { "shared-file" }, null, target, minutes, limit)
        } catch (error: Exception) {
            deleteStaging(target)
            throw error
        }
    }
    @Synchronized
    private fun add(base: String, text: String?, file: File?, minutes: Long, limit: Int?): AndroidQuickShareItem {
        require(minutes > 0 && (limit == null || limit > 0))
        items()
        check(shares.size < 100) { "Quick Share publication limit reached" }
        val expiresAt = Math.addExact(System.currentTimeMillis(), Math.multiplyExact(minutes, 60_000L))
        if (!running.get()) start()
        val slug = base.lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48).ifBlank { "share" }
        val id = "$slug-${java.util.UUID.randomUUID()}"
        val item = Share(id, base, text, file, expiresAt, limit)
        shares[id] = item
        return snapshot(item)
    }
    fun items(): List<AndroidQuickShareItem> {
        val now = System.currentTimeMillis()
        shares.values.filter { it.expiresAt <= now || (it.limit != null && it.accesses >= it.limit) }
            .forEach(::retire)
        return shares.values.map(::snapshot).sortedBy { it.name.lowercase() }
    }
    fun stopShare(id: String) { shares[id]?.let(::retire) }
    private fun retire(share: Share) = synchronized(share) {
        share.revoked = true
        shares.remove(share.id, share)
        if (share.readers == 0) share.file?.let(::deleteStaging)
        Unit
    }
    private fun snapshot(s: Share) = AndroidQuickShareItem(s.id, s.name, "${urls.firstOrNull()?.removeSuffix("/") ?: "http://127.0.0.1:$port"}/s/${enc(s.id)}", s.text != null, s.expiresAt, s.limit, s.accesses)
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun pageSnapshot(s: Share): QuickShareSnapshot = QuickShareSnapshot(
        s.id, if (s.text != null) QuickShareType.TEXT else QuickShareType.FILE,
        s.name, if (s.file != null) s.name else "",
        s.file?.length() ?: s.text.orEmpty().toByteArray(StandardCharsets.UTF_8).size.toLong(),
        Instant.ofEpochMilli(s.createdAt), Instant.ofEpochMilli(s.expiresAt), s.limit, s.accesses,
        when {
            s.expiresAt <= System.currentTimeMillis() -> QuickShareStatus.EXPIRED
            s.limit != null && s.accesses >= s.limit -> QuickShareStatus.LIMIT_REACHED
            else -> QuickShareStatus.ACTIVE
        }, listOf(snapshot(s).url),
    )
    private fun available(share: Share): Boolean = !share.revoked && shares[share.id] === share &&
        share.expiresAt > System.currentTimeMillis() && (share.limit == null || share.accesses < share.limit)

    private fun handle(client: Socket) = client.use { c -> runCatching {
        c.soTimeout = 10_000
        val channel = com.shterneregen.securelan.common.net.transport.LineTextChannel(c, 4096)
        val request = com.shterneregen.securelan.common.net.transport.SocketDeadlines.within(c, 10_000) {
            val line = channel.readLine() ?: return@within emptyList<String>()
            var total = line.length
            var count = 0
            while (true) {
                val header = channel.readLine() ?: error("Incomplete HTTP headers")
                total += header.length
                check(total <= 16_384 && ++count <= 32) { "HTTP headers too large" }
                if (header.isEmpty()) break
                check(header.contains(':') && header.all { it.code in 32..126 || it == '\t' }) { "Invalid HTTP header" }
                check(!header.substringBefore(':').equals("Transfer-Encoding", true)) { "Request bodies unsupported" }
                if (header.substringBefore(':').equals("Content-Length", true)) check(header.substringAfter(':').trim() == "0")
            }
            line.split(' ')
        }
        if (request.size != 3 || request[2] !in setOf("HTTP/1.0", "HTTP/1.1") || !request[1].startsWith('/')) {
            respond(c, "400 Bad Request", "Malformed HTTP request".toByteArray()); return@runCatching
        }
        if (request[0] != "GET") { respond(c, "405 Method Not Allowed", "Method not allowed".toByteArray()); return@runCatching }
        val path = URLDecoder.decode(request[1].substringBefore('?'), "UTF-8")
        val parts = path.removePrefix("/").split('/')
        val target = if (parts.firstOrNull() == "s") parts.drop(1) else parts
        if (path == "/") {
            respond(c, "200 OK", QuickShareHtml.indexHtml(shares.values.map(::pageSnapshot)).toByteArray(StandardCharsets.UTF_8)); return@runCatching
        }
        if (target.size !in 1..2 || target.any { it.isBlank() || it.any { ch -> ch.isISOControl() || ch == '\\' } }) {
            respond(c, "404 Not Found", QuickShareHtml.notFoundHtml().toByteArray(StandardCharsets.UTF_8)); return@runCatching
        }
        val share = shares[target[0]]
        val action = target.getOrNull(1).orEmpty()
        if (share == null) { respond(c, "410 Gone", "Share is no longer available".toByteArray()); return@runCatching }
        if (action == "download" && share.file != null) {
            val input = synchronized(share) {
                if (!available(share)) null else share.file.inputStream().also { share.readers++; share.accesses++ }
            }
            if (input == null) { respond(c, "410 Gone", "Share is no longer available".toByteArray()); return@runCatching }
            try {
                input.use { stream ->
                    com.shterneregen.securelan.common.net.transport.SocketDeadlines.within(c, 10_000) {
                        val out = c.getOutputStream()
                        val name = share.name.map { if (it.code !in 32..126 || it == '"' || it == '\\') '_' else it }.joinToString("")
                        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + share.file.length() +
                            "\r\nContent-Disposition: attachment; filename=\"" + name + "\"; filename*=UTF-8''" + enc(share.name).replace("+", "%20") + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray())
                    }
                    com.shterneregen.securelan.common.net.transport.SocketDeadlines.copyWithProgress(c, stream, c.getOutputStream())
                }
            } finally {
                synchronized(share) {
                    share.readers--
                    if (share.readers == 0 && (share.revoked || !available(share))) deleteStaging(share.file)
                }
            }
            return@runCatching
        }
        val body = synchronized(share) {
            if (!available(share)) null
            else if (share.text != null && action in setOf("", "text")) {
                share.accesses++
                if (action == "text") share.text.toByteArray(StandardCharsets.UTF_8)
                else QuickShareHtml.textItemHtml(pageSnapshot(share), share.text).toByteArray(StandardCharsets.UTF_8)
            } else if (share.file != null && action.isBlank()) QuickShareHtml.fileItemHtml(pageSnapshot(share)).toByteArray(StandardCharsets.UTF_8)
            else ByteArray(0)
        }
        when {
            body == null -> respond(c, "410 Gone", "Share is no longer available".toByteArray())
            body.isEmpty() && !(action == "text" && share.text != null) -> respond(c, "404 Not Found", QuickShareHtml.notFoundHtml().toByteArray())
            else -> respond(c, "200 OK", body, if (action == "text") "text/plain; charset=utf-8" else "text/html; charset=utf-8")
        }
    } }

    private fun respond(client: Socket, status: String, body: ByteArray, type: String = "text/html; charset=utf-8") {
        com.shterneregen.securelan.common.net.transport.SocketDeadlines.within(client, 60_000) {
            val out = client.getOutputStream()
            out.write(("HTTP/1.1 " + status + "\r\nContent-Type: " + type + "\r\nContent-Length: " + body.size +
                "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray())
            out.write(body); out.flush()
        }
    }
    @Synchronized override fun close() {
        closed = true
        running.set(false)
        runCatching { socket?.close() }
        socket = null
        activeClients.forEach { runCatching { it.close() } }
        activeClients.clear()
        clients.shutdownNow()
        shares.values.forEach(::retire)
        shares.clear()
        urls = emptyList()
    }
    companion object {
        private val stagingLock = Any()
        private val ownedStaging = mutableSetOf<File>()
    }

}
