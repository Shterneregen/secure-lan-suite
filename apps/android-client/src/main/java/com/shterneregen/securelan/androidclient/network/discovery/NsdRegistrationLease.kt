package com.shterneregen.securelan.androidclient.network.discovery

/** Cancelling a pending platform registration still owns its eventual successful callback. */
internal class NsdRegistrationLease(private val unregister: () -> Unit, private val ready: () -> Unit) : AutoCloseable {
    private var registered = false
    private var released = false
    @Volatile var isClosed: Boolean = false
        private set
    @Synchronized fun registered() {
        if (registered) return
        registered = true
        if (isClosed) release() else ready()
    }
    @Synchronized override fun close() { isClosed = true; if (registered) release() }
    private fun release() { if (!released) { released = true; unregister() } }
}
