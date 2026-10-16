package com.shterneregen.securelan.desktop.compose

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Bounded native work with expiring results. Timeout does not attempt to interrupt JNI. */
internal class DesktopMediaOperations(
    private val dispatch: (() -> Unit) -> Unit,
    private val timeoutMillis: Long = 10_000,
) : AutoCloseable {
    private val closed = AtomicBoolean()
    private val generations = ConcurrentHashMap<String, AtomicLong>()
    private val worker = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, ArrayBlockingQueue(4),
        { task -> Thread(task, "desktop-media-operation").apply { isDaemon = true } })
    private val timer = ScheduledThreadPoolExecutor(1) { task ->
        Thread(task, "desktop-media-result-timeout").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }

    fun invalidate(key: String) { generations.computeIfAbsent(key) { AtomicLong() }.incrementAndGet() }

    fun <T> submit(key: String, work: () -> T, publish: (Result<T>) -> Unit) {
        if (closed.get()) return
        val generation = generations.computeIfAbsent(key) { AtomicLong() }
        val token = generation.incrementAndGet()
        val completed = AtomicBoolean()
        fun deliver(result: Result<T>) {
            if (!completed.compareAndSet(false, true)) return
            dispatch { if (!closed.get() && generation.get() == token) publish(result) }
        }
        val deadline = timer.schedule({ deliver(Result.failure(IllegalStateException("Media device operation timed out. Check the device and retry."))) }, timeoutMillis, TimeUnit.MILLISECONDS)
        try {
            worker.execute {
                if (closed.get() || completed.get() || generation.get() != token) { deadline.cancel(false); return@execute }
                val result = runCatching(work)
                deadline.cancel(false)
                deliver(result)
            }
        } catch (error: java.util.concurrent.RejectedExecutionException) {
            deadline.cancel(false)
            deliver(Result.failure(IllegalStateException("Media device is still busy. Retry shortly.", error)))
        }
    }

    override fun close() {
        closed.set(true)
        timer.shutdownNow()
        worker.queue.clear()
        worker.shutdown()
    }
}
