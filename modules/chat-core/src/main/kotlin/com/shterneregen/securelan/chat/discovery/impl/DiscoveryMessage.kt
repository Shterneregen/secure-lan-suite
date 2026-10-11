package com.shterneregen.securelan.chat.discovery.impl

/**
 * Internal data class representing a peer discovery broadcast payload.
 *
 * This is an internal transport type used exclusively within the `impl` package.
 * It mirrors [com.shterneregen.securelan.chat.discovery.DiscoveryCodec.Message] but is kept
 * separate to maintain the internal/public API boundary.
 *
 * Serialization and deserialization are handled by [DiscoveryMessageCodec].
 *
 * @property peerId    A stable unique identifier for the peer.
 * @property nickname  The human-readable display name advertised by the peer.
 * @property chatPort  The TCP port on which the peer accepts chat connections.
 * @property filePort  The TCP port on which the peer accepts file-transfer connections.
 */
internal data class DiscoveryMessage(
    val peerId: String,
    val nickname: String,
    val chatPort: Int,
    val filePort: Int,
)
