package com.shterneregen.securelan.chat.discovery

import java.time.Instant
import java.util.Objects

/**
 * An immutable snapshot of a discovered LAN peer at a specific point in time.
 *
 * Instances are created by [com.shterneregen.securelan.chat.discovery.impl.UdpBroadcastPeerDiscoveryService]
 * whenever a valid UDP discovery broadcast is received from a remote peer.
 *
 * @property peerId      A stable, unique identifier for the peer (e.g. a UUID).  Must not be blank.
 * @property nickname    The human-readable display name advertised by the peer.  Must not be blank.
 * @property host        The IP address from which the discovery packet was received.  Must not be blank.
 * @property chatPort    The TCP port on which the peer is listening for chat connections (1–65 535).
 * @property filePort    The TCP port on which the peer is listening for file-transfer connections (1–65 535).
 * @property lastSeen    The [Instant] at which the most recent discovery packet was received.
 *
 * @throws IllegalArgumentException if any of the string fields is blank or either port is out of range.
 * @throws NullPointerException     if [peerId], [nickname], [host], or [lastSeen] is `null`.
 */
@JvmRecord
data class DiscoveredPeer(
    val peerId: String,
    val nickname: String,
    val host: String,
    val chatPort: Int,
    val filePort: Int,
    val lastSeen: Instant,
) {
    init {
        Objects.requireNonNull(peerId, "peerId must not be null")
        Objects.requireNonNull(nickname, "nickname must not be null")
        Objects.requireNonNull(host, "host must not be null")
        Objects.requireNonNull(lastSeen, "lastSeen must not be null")
        require(peerId.isNotBlank()) { "peerId must not be blank" }
        require(nickname.isNotBlank()) { "nickname must not be blank" }
        require(host.isNotBlank()) { "host must not be blank" }
        validatePort(chatPort, "chatPort")
        validatePort(filePort, "filePort")
    }

    companion object {
        private fun validatePort(port: Int, fieldName: String) {
            require(port in 1..65_535) { "$fieldName must be between 1 and 65535" }
        }
    }
}
