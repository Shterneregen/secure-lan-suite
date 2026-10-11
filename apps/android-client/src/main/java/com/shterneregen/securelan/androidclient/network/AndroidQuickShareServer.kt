package com.shterneregen.securelan.androidclient.network

import android.content.ContentResolver
import android.net.Uri
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareHtml
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import java.io.Closeable
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class AndroidQuickShareItem(
    val id: String, val name: String, val url: String, val isText: Boolean,
    val expiresAt: Long, val accessLimit: Int?, val accessCount: Int,
)

/** Small Android-compatible HTTP server implementing the desktop Quick Share URL contract. */
class AndroidQuickShareServer(private val resolver: ContentResolver, private val cacheDir: File) : Closeable {
    private data class Share(val id: String, val name: String, val text: String?, val file: File?, val expiresAt: Long, val limit: Int?, @Volatile var accesses: Int = 0, val createdAt: Long = System.currentTimeMillis())
    private val running = AtomicBoolean(false)
    private val shares = ConcurrentHashMap<String, Share>()
    private val clients = Executors.newCachedThreadPool()
    @Volatile private var socket: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile var urls: List<String> = emptyList(); private set
    @Volatile var port: Int = 5053; private set

    @Synchronized fun start() {
        if (running.get()) return
        var server: ServerSocket? = null
        for (p in 5053..5063) try { server = ServerSocket(p, 32, InetAddress.getByName("0.0.0.0")); break } catch (_: Exception) { }
        socket = server ?: ServerSocket(0, 32, InetAddress.getByName("0.0.0.0"))
        port = socket!!.localPort
        val hosts = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().flatMap { ni -> if (!ni.isUp || ni.isLoopback) emptyList() else ni.inetAddresses.toList() }
                .filter { it is java.net.Inet4Address && it.isSiteLocalAddress }.map { it.hostAddress }.distinct()
        }.getOrDefault(emptyList()).ifEmpty { listOf("127.0.0.1") }
        urls = hosts.map { "http://$it:$port/" }
        running.set(true)
        acceptThread = Thread {
            while (running.get()) {
                try { val client = socket!!.accept(); clients.execute { handle(client) } }
                catch (_: Exception) { if (!running.get()) break }
            }
        }.also { it.name = "securelan-quick-share"; it.isDaemon = true; it.start() }
    }

    fun createText(text: String, minutes: Long, limit: Int?): AndroidQuickShareItem = add("shared-text", text, null, minutes, limit)
    fun createFile(uri: Uri, name: String, minutes: Long, limit: Int?): AndroidQuickShareItem {
        if (!running.get()) start()
        val target = File(cacheDir, "quick-share-${System.nanoTime()}-${name.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) } ?: error("Unable to read selected file")
        return add(name.ifBlank { "shared-file" }, null, target, minutes, limit)
    }
    private fun add(base: String, text: String?, file: File?, minutes: Long, limit: Int?): AndroidQuickShareItem {
        if (!running.get()) start()
        require(minutes > 0 && (limit == null || limit > 0))
        val slug = base.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48).ifBlank { "share" }
        var id = slug; var n = 2; while (shares.containsKey(id)) id = "$slug-${n++}"
        val item = Share(id, base, text, file, System.currentTimeMillis() + minutes * 60_000, limit)
        shares[id] = item
        return snapshot(item)
    }
    fun items(): List<AndroidQuickShareItem> {
        val now = System.currentTimeMillis()
        shares.values.filter { it.expiresAt <= now || (it.limit != null && it.accesses >= it.limit) }
            .forEach { if (shares.remove(it.id, it)) it.file?.delete() }
        return shares.values.map(::snapshot).sortedBy { it.name.lowercase() }
    }
    fun stopShare(id: String) { shares.remove(id)?.file?.delete() }
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
    private fun handle(client: Socket) = client.use { c -> runCatching {
        c.soTimeout = 10_000
        val reader = c.getInputStream().bufferedReader(StandardCharsets.UTF_8)
        val request = reader.readLine()?.split(' ') ?: return@runCatching
        while (reader.readLine()?.isNotEmpty() == true) { }
        val path = URLDecoder.decode(request.getOrNull(1)?.substringBefore('?') ?: "/", "UTF-8").trim('/').split('/')
        val sharePath = if (path.firstOrNull() == "s") path.drop(1) else path
        val id = sharePath.firstOrNull().orEmpty()
        val action = sharePath.getOrNull(1).orEmpty()
        val share = shares[id]
        val body: ByteArray; var status = "200 OK"; var type = "text/html; charset=utf-8"; var disposition: String? = null
        if (request[0] != "GET") { status = "405 Method Not Allowed"; body = "Method not allowed".toByteArray(); type = "text/plain" }
        else if (id.isBlank()) {
            body = QuickShareHtml.indexHtml(shares.values.map(::pageSnapshot)).toByteArray(StandardCharsets.UTF_8)
        } else if (share == null || share.expiresAt <= System.currentTimeMillis() || (share.limit != null && share.accesses >= share.limit)) {
            status = "410 Gone"
            if (action.isBlank()) body = QuickShareHtml.unavailableHtml(share?.let(::pageSnapshot)).toByteArray(StandardCharsets.UTF_8)
            else { body = "Share is no longer available".toByteArray(); type = "text/plain; charset=utf-8" }
        } else if (action == "download" && share.file != null) {
            synchronized(share) { if (share.limit != null && share.accesses >= share.limit) { status = "410 Gone" } else share.accesses++ }
            if (status == "410 Gone") body = "Share is no longer available".toByteArray() else {
                val out = c.getOutputStream()
                val downloadName = share.name.replace(Regex("[\\r\\n\\\"]"), "_")
                out.write("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: ${share.file.length()}\r\nContent-Disposition: attachment; filename=\"$downloadName\"; filename*=UTF-8''${enc(share.name).replace("+", "%20")}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n".toByteArray())
                share.file.inputStream().use { it.copyTo(out) }; out.flush()
                return@runCatching
            }
        } else if ((action == "text" || action.isBlank()) && share.text != null) {
            synchronized(share) { if (share.limit != null && share.accesses >= share.limit) status = "410 Gone" else share.accesses++ }
            if (status == "410 Gone") body = if (action.isBlank()) QuickShareHtml.unavailableHtml(pageSnapshot(share)).toByteArray(StandardCharsets.UTF_8) else "Share is no longer available".toByteArray()
            else { body = if (action == "text") share.text.toByteArray(StandardCharsets.UTF_8) else QuickShareHtml.textItemHtml(pageSnapshot(share), share.text).toByteArray(StandardCharsets.UTF_8); type = if (action == "text") "text/plain; charset=utf-8" else "text/html; charset=utf-8" }
        } else if (action.isBlank() && share.file != null) body = QuickShareHtml.fileItemHtml(pageSnapshot(share)).toByteArray(StandardCharsets.UTF_8)
        else { status = "404 Not Found"; body = QuickShareHtml.notFoundHtml().toByteArray(StandardCharsets.UTF_8) }
        val out = c.getOutputStream(); out.write("HTTP/1.1 $status\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\n${disposition?.let { "Content-Disposition: $it\r\n" } ?: ""}Connection: close\r\n\r\n".toByteArray()); out.write(body); out.flush()
    } }
    @Synchronized override fun close() { running.set(false); runCatching { socket?.close() }; socket = null; shares.values.forEach { it.file?.delete() }; shares.clear(); urls = emptyList() }
}
