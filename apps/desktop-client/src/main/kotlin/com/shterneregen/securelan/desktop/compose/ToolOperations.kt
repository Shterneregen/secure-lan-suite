package com.shterneregen.securelan.desktop.compose

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Owns bounded tool execution, cancellation and stale-completion suppression. */
internal class ToolOperations(
    private val dispatch: (() -> Unit) -> Unit,
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { work -> Thread(work, "desktop-tools").apply { isDaemon = true } },
) : AutoCloseable {
    private val busy = AtomicBoolean()
    private val generation = AtomicLong()
    private val closed = AtomicBoolean()

    fun <T> execute(work: () -> T, onStarted: () -> Unit = {}, complete: (Result<T>) -> Unit): Boolean {
        if (closed.get() || !busy.compareAndSet(false, true)) return false
        val token = generation.incrementAndGet()
        try {
            // Publish startup before even an immediately completed operation can publish its result.
            onStarted()
            worker.execute {
                val result = runCatching(work)
                busy.set(false)
                dispatch { if (!closed.get() && generation.get() == token) complete(result) }
            }
        } catch (error: RuntimeException) {
            busy.set(false)
            throw error
        }
        return true
    }

    fun invalidate() { generation.incrementAndGet() }

    override fun close() {
        closed.set(true)
        invalidate()
        worker.shutdownNow()
    }
}
