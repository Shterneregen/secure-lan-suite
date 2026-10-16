package com.shterneregen.securelan.chat.transport

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.common.net.transport.LineTextChannel
import com.shterneregen.securelan.common.net.transport.SocketDeadlines
import com.shterneregen.securelan.crypto.protocol.SecureRecordCipher
import com.shterneregen.securelan.crypto.protocol.SecureSessionKeys
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.util.Base64

/** Bounded line transport with a latching, ordered, mutually authenticated V2 record channel. */
class ChatSocketSession(private val socket: Socket) : Closeable {
    private val channel = LineTextChannel(socket)
    @Volatile private var cipher: SecureRecordCipher? = null

    private val writeLock = java.util.concurrent.locks.ReentrantLock()
    private val closed = java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile private var lastReceived = System.nanoTime()
    private var heartbeat: Thread? = null
    private var healthDeadline: java.util.concurrent.ScheduledFuture<*>? = null

    /** Negotiated only with peers advertising health:v1. Any authenticated frame renews liveness. */
    @Synchronized
    fun startHealth(intervalMillis: Long = 10_000, timeoutMillis: Long = 30_000) {
        check(intervalMillis > 0 && timeoutMillis > intervalMillis)
        if (heartbeat != null || closed.get()) return
        lastReceived = System.nanoTime()
        healthDeadline = healthScheduler.scheduleAtFixedRate({
            if (System.nanoTime() - lastReceived >= java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis)) close()
        }, intervalMillis, intervalMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
        heartbeat = Thread({
            try {
                while (!closed.get()) {
                    Thread.sleep(intervalMillis)
                    if (!closed.get()) writeMessage(WireMessage(WireMessageType.PING, "", ""))
                }
            } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            catch (_: Exception) { close() }
        }, "chat-heartbeat").apply { isDaemon = true; start() }
    }

    fun readMessage(): WireMessage? {
        while (true) {
            val message = readFrame() ?: return null
            lastReceived = System.nanoTime()
            when (message.type()) {
                WireMessageType.PING -> writeMessage(WireMessage(WireMessageType.PONG, "", ""))
                WireMessageType.PONG -> Unit
                else -> return message
            }
        }
    }

    private fun readFrame(): WireMessage? {
        val line = channel.readLine(if (cipher == null) 4096 else 90_000) ?: return null
        if (cipher == null && line.length > 4096) throw IOException("Handshake exceeds maximum size")
        return try {
            val text = cipher?.decrypt(Base64.getDecoder().decode(line))?.toString(Charsets.UTF_8) ?: line
            WireMessage.deserialize(text)
        } catch (error: Exception) {
            close()
            throw IOException("Invalid chat frame", error)
        }
    }

    fun writeMessage(message: WireMessage) = SocketDeadlines.within(socket, 10_000) {
        // Deadline includes lock wait and encryption ordering, not just the physical write.
        writeLock.lockInterruptibly()
        try {
            if (closed.get()) throw IOException("Chat session closed")
            val text = message.serialize()
            if (cipher == null && text.length > 4096) throw IOException("Handshake exceeds maximum size")
            val line = cipher?.let { Base64.getEncoder().encodeToString(it.encrypt(text.toByteArray(Charsets.UTF_8))) } ?: text
            channel.writeLine(line)
        } finally { writeLock.unlock() }
    }

    fun enableTransportEncryption(keys: SecureSessionKeys, client: Boolean = true) {
        check(cipher == null) { "Secure transport already enabled" }
        cipher = SecureRecordCipher(keys, client)
    }

    fun <T> handshake(action: () -> T): T = SocketDeadlines.within(socket, 10_000, action)
    fun isSecure(): Boolean = cipher != null
    fun remoteAddress(): String = channel.remoteAddress()
    fun setReadTimeout(timeoutMillis: Int) = channel.setReadTimeout(timeoutMillis)
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try { channel.close() }
            finally {
                synchronized(this) {
                    healthDeadline?.cancel(false)
                    heartbeat?.interrupt()
                }
            }
        }
    }

    companion object {
        private val healthScheduler = java.util.concurrent.ScheduledThreadPoolExecutor(1) { task ->
            Thread(task, "chat-health-deadline").apply { isDaemon = true }
        }.apply { removeOnCancelPolicy = true }
    }
}
