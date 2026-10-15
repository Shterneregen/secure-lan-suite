package com.shterneregen.securelan.desktop.compose

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Owns bounded tool execution, cancellation and stale-completion suppression. */
internal class ToolOperations(private val dispatch: (() -> Unit) -> Unit) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor { work -> Thread(work, "desktop-tools").apply { isDaemon = true } }
    private val busy = AtomicBoolean()
    private val generation = AtomicLong()
    private val closed = AtomicBoolean()

    fun <T> execute(work: () -> T, complete: (Result<T>) -> Unit): Boolean {
        if (closed.get() || !busy.compareAndSet(false, true)) return false
        val token = generation.incrementAndGet()
        worker.execute {
            val result = runCatching(work)
            busy.set(false)
            dispatch { if (!closed.get() && generation.get() == token) complete(result) }
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
