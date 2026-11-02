package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Delayed
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SocketDeadlineProgressTest {
    @Test fun progressingStreamMayOutliveOneInactivityBudget() {
        val scheduler = ManualDeadlineScheduler()
        try {
            Socket().use { socket ->
                var chunks = 0
                val stream = object : InputStream() {
                    override fun read(): Int = error("bulk only")
                    override fun read(bytes: ByteArray): Int {
                        if (chunks++ == 4) return -1
                        scheduler.advanceBy(35)
                        assertFalse(socket.isClosed, "Progress must renew the inactivity deadline")
                        bytes[0] = 42
                        return 1
                    }
                }
                val output = ByteArrayOutputStream()
                SocketDeadlines.copyWithProgress(socket, stream, output, 100, scheduler)
                assertEquals(140L, scheduler.nowMillis)
                scheduler.advanceBy(100)
                assertFalse(socket.isClosed)
                assertArrayEquals(byteArrayOf(42, 42, 42, 42), output.toByteArray())
            }
        } finally { scheduler.shutdownNow() }
    }

    @Test fun stalledSourceIsClosedByWatchdog() {
        val closed = CountDownLatch(1)
        val socket = object : Socket() { override fun close() { super.close(); closed.countDown() } }
        socket.use {
            val stream = object : InputStream() {
                override fun read(): Int = error("bulk only")
                override fun read(bytes: ByteArray): Int {
                    check(closed.await(2, TimeUnit.SECONDS))
                    throw java.io.IOException("closed")
                }
            }
            assertThrows(java.io.IOException::class.java) { SocketDeadlines.copyWithProgress(socket, stream, ByteArrayOutputStream(), 50) }
            assertTrue(socket.isClosed)
        }
    }

    /** Executes deadline callbacks only when the test advances time. No worker threads are started. */
    private class ManualDeadlineScheduler : ScheduledThreadPoolExecutor(1) {
        var nowMillis = 0L
            private set
        private val deadlines = mutableListOf<ScheduledFuture<*>>()

        override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
            val dueMillis = nowMillis + unit.toMillis(delay)
            val task = object : FutureTask<Unit>(command, Unit), ScheduledFuture<Unit> {
                override fun getDelay(unit: TimeUnit): Long = unit.convert(dueMillis - nowMillis, TimeUnit.MILLISECONDS)
                override fun compareTo(other: Delayed): Int = getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))
            }
            deadlines.add(task)
            return task
        }

        fun advanceBy(millis: Long) {
            nowMillis += millis
            deadlines.filter { !it.isDone && it.getDelay(TimeUnit.MILLISECONDS) <= 0 }.forEach { (it as Runnable).run() }
            deadlines.removeAll { it.isDone }
        }
    }
}
