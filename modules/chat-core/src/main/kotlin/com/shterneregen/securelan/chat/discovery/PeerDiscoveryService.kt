package com.shterneregen.securelan.chat.discovery

/**
 * Service that discovers other chat peers on the local network via UDP broadcast.
 *
 * ## Lifecycle
 * 1. Call [start] with a [PeerDiscoveryConfig] and a [PeerDiscoveryListener] to begin.
 * 2. Optionally toggle the announce behaviour at runtime with [setAnnounceEnabled].
 * 3. Call [stop] (or close via [AutoCloseable]) to release resources.
 *
 * [close] is a no-op alias for [stop] that allows the service to be used in
 * `try-with-resources` / Kotlin `use {}` blocks.
 *
 * ## Thread safety
 * Implementations must be thread-safe.  [PeerDiscoveryListener] callbacks may be delivered
 * from background threads.
 */
interface PeerDiscoveryService : AutoCloseable {

    /**
     * Starts the discovery service.
     *
     * If the service is already running it is stopped first and restarted with the new
     * configuration.
     *
     * @param config   The discovery configuration to use.
     * @param listener The callback that receives peer-lifecycle events.
     */
    fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener)

    /**
     * Stops the discovery service, releases the UDP socket, and clears the peer list.
     *
     * Safe to call even if the service is not currently running.
     */
    fun stop()

    /**
     * Enables or disables outbound UDP announcements at runtime without restarting the service.
     *
     * When disabled, the service continues to listen for broadcasts from other peers but
     * does not send its own.
     *
     * @param announceEnabled `true` to start broadcasting; `false` to stop.
     */
    fun setAnnounceEnabled(announceEnabled: Boolean)

    /**
     * Returns `true` if the service is currently running (i.e. [start] has been called
     * and [stop] has not yet been called).
     */
    fun isRunning(): Boolean

    /**
     * Returns an immutable snapshot of all currently known (non-stale) peers, sorted
     * alphabetically by nickname (case-insensitive).
     *
     * @return A new list containing the current peer snapshots.
     */
    fun snapshot(): List<DiscoveredPeer>

    /**
     * Alias for [stop]; allows use in `try-with-resources` / Kotlin `use {}` blocks.
     */
    override fun close() {
        stop()
    }
}
