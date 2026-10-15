package com.shterneregen.securelan.desktop.compose

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Keeps one pending UI task and the latest frame, without queuing one task per native callback. */
internal class LatestUiValue<T>(private val dispatch: (() -> Unit) -> Unit, private val apply: (T) -> Unit) {
    private val value = AtomicReference<T?>()
    private val pending = AtomicBoolean()

    fun offer(next: T) {
        value.set(next)
        schedule()
    }

    fun clear() { value.set(null) }

    private fun schedule() {
        if (!pending.compareAndSet(false, true)) return
        dispatch {
            try { value.getAndSet(null)?.let(apply) }
            finally {
                pending.set(false)
                if (value.get() != null) schedule()
            }
        }
    }
}
