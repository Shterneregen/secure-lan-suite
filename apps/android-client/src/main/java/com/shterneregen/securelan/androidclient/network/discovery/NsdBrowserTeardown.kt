package com.shterneregen.securelan.androidclient.network.discovery

import kotlinx.coroutines.CompletableDeferred

/**
 * Tracks native ownership until discovery and every service-info registration acknowledge stopping.
 * All mutations belong to the facade's serial executor. Cancellation only stops waiting; a failed
 * stop request is not an acknowledgement and must not admit replacement native resources.
 */
internal class NsdBrowserTeardown {
    private val finished = CompletableDeferred<Unit>()
    private val subscriptions = mutableSetOf<Any>()
    private var closing = false
    private var discoveryStopped = false
    val pendingSubscriptions: Int get() = subscriptions.size

    fun registered(token: Any) { check(!closing); subscriptions += token }
    fun unregistered(token: Any) { subscriptions -= token; completeIfStopped() }
    fun discoveryStopped() { discoveryStopped = true; completeIfStopped() }
    fun closeRequested() { closing = true; completeIfStopped() }
    suspend fun awaitClosed() { finished.await() }

    private fun completeIfStopped() {
        if (closing && discoveryStopped && subscriptions.isEmpty()) finished.complete(Unit)
    }
}
