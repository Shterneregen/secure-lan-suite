package com.shterneregen.securelan.common.net.transport

import java.net.Socket
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Shared absolute IO deadlines, including writes and trickling clients; close unblocks socket IO. */
object SocketDeadlines {
    private val scheduler = ScheduledThreadPoolExecutor(1) { task ->
        Thread(task, "securelan-socket-deadlines").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }

    fun <T> within(socket: Socket, millis: Long, action: () -> T): T {
        val timeout = scheduler.schedule({ runCatching { socket.close() } }, millis, TimeUnit.MILLISECONDS)
        return try { action() } finally { timeout.cancel(false) }
    }
}
