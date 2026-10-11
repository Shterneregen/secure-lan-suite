package com.shterneregen.securelan.chat.discovery

/**
 * Callback interface for receiving events from a [PeerDiscoveryService].
 *
 * Implementations should be thread-safe: callbacks may be invoked from background threads
 * managed by the discovery service.
 */
interface PeerDiscoveryListener {

    /**
     * Invoked when a new peer is discovered or when a known peer sends a fresh broadcast.
     *
     * If the [peer] was already known, this callback is called again with an updated
     * [DiscoveredPeer.lastSeen] timestamp to signal activity.
     *
     * @param peer The discovered (or refreshed) peer snapshot.
     */
    fun onPeerDiscovered(peer: DiscoveredPeer)

    /**
     * Invoked when a previously discovered peer has not sent a broadcast within the
     * configured stale timeout and has been removed from the active-peer list.
     *
     * @param peer The last known snapshot of the expired peer.
     */
    fun onPeerExpired(peer: DiscoveredPeer)

    /**
     * Invoked when an error occurs during discovery (e.g. a socket I/O failure or an
     * unreadable packet).
     *
     * Errors reported through this callback are non-fatal: the service continues running
     * unless [PeerDiscoveryService.stop] is called explicitly.
     *
     * @param message A human-readable description of the error.
     * @param cause   The underlying exception.
     */
    fun onDiscoveryError(message: String, cause: Throwable)
}
