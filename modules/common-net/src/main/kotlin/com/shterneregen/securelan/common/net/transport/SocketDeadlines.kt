package com.shterneregen.securelan.common.net.transport

import java.net.Socket
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Shared absolute IO deadlines, including writes and trickling clients; close unblocks socket IO. */
object SocketDeadlines {
    private val scheduler = ScheduledThreadPoolExecutor(1) { task ->
        Thread(task, "securelan-socket-deadlines").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }

    /** Streaming inactivity budget is renewed after each completed read/write chunk. */
    fun copyWithProgress(socket: Socket, input: java.io.InputStream, output: java.io.OutputStream, millis: Long = 60_000) {
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val count = within(socket, millis) {
                var read: Int
                do { read = input.read(buffer) } while (read == 0)
                if (read > 0) { output.write(buffer, 0, read); output.flush() }
                read
            }
            if (count < 0) return
        }
    }

    fun <T> within(socket: Socket, millis: Long, action: () -> T): T {
        val timeout = scheduler.schedule({ runCatching { socket.close() } }, millis, TimeUnit.MILLISECONDS)
        return try { action() } finally { timeout.cancel(false) }
    }
}
