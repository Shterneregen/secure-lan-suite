package com.shterneregen.securelan.chat.discovery

import com.shterneregen.securelan.common.net.NetworkConstants
import java.time.Duration
import java.util.Objects

/**
 * Immutable configuration for a [PeerDiscoveryService] instance.
 *
 * Use the companion-object factory methods for common setups rather than the primary constructor:
 * - [defaults] — announcing peer with chat and file-transfer ports.
 * - [listenOnly] — passive peer that does not broadcast its own presence.
 *
 * @property peerId            A stable unique identifier for this peer (e.g. a UUID).
 * @property nickname          The human-readable display name broadcast to other peers.
 * @property discoveryPort     The UDP port used for both sending and receiving discovery broadcasts.
 * @property chatPort          The TCP port on which this peer accepts chat connections.
 *                             Only validated when [announceEnabled] is `true`.
 * @property filePort          The TCP port on which this peer accepts file-transfer connections.
 *                             Only validated when [announceEnabled] is `true`.
 * @property announceEnabled   Whether this peer should actively broadcast its own presence.
 *                             Set to `false` for listen-only mode.
 * @property announceInterval  How often the peer broadcasts its presence.  Must be positive.
 * @property staleTimeout      How long to wait without hearing from a peer before considering it
 *                             gone.  Must be greater than [announceInterval].
 *
 * @throws IllegalArgumentException if any constraint is violated.
 * @throws NullPointerException     if a non-nullable reference parameter is `null`.
 */
@JvmRecord
data class PeerDiscoveryConfig(
    val peerId: String,
    val nickname: String,
    val discoveryPort: Int,
    val chatPort: Int,
    val filePort: Int,
    val announceEnabled: Boolean,
    val announceInterval: Duration,
    val staleTimeout: Duration,
) {
    init {
        Objects.requireNonNull(peerId, "peerId must not be null")
        Objects.requireNonNull(nickname, "nickname must not be null")
        Objects.requireNonNull(announceInterval, "announceInterval must not be null")
        Objects.requireNonNull(staleTimeout, "staleTimeout must not be null")
        require(peerId.isNotBlank()) { "peerId must not be blank" }
        require(nickname.isNotBlank()) { "nickname must not be blank" }
        validatePort(discoveryPort, "discoveryPort")
        if (announceEnabled) {
            validatePort(chatPort, "chatPort")
            validatePort(filePort, "filePort")
        }
        require(!(announceInterval.isZero || announceInterval.isNegative)) { "announceInterval must be positive" }
        require(staleTimeout > announceInterval) { "staleTimeout must be greater than announceInterval" }
    }

    companion object {
        /**
         * Creates a default announcing configuration with a 10-second announce interval and a
         * 45-second stale timeout.
         *
         * @param peerId    Unique peer identifier.
         * @param nickname  Display name for this peer.
         * @param chatPort  TCP port for chat connections.
         * @param filePort  TCP port for file-transfer connections.
         * @return A fully configured [PeerDiscoveryConfig] with announcing enabled.
         */
        @JvmStatic
        fun defaults(peerId: String, nickname: String, chatPort: Int, filePort: Int): PeerDiscoveryConfig =
            defaults(peerId, nickname, chatPort, filePort, true)

        /**
         * Creates a default configuration where announcing can be toggled via [announceEnabled].
         *
         * @param peerId          Unique peer identifier.
         * @param nickname        Display name for this peer.
         * @param chatPort        TCP port for chat connections.
         * @param filePort        TCP port for file-transfer connections.
         * @param announceEnabled Whether to broadcast this peer's presence.
         * @return A [PeerDiscoveryConfig] with a 10-second interval and 45-second stale timeout.
         */
        @JvmStatic
        fun defaults(
            peerId: String,
            nickname: String,
            chatPort: Int,
            filePort: Int,
            announceEnabled: Boolean,
        ): PeerDiscoveryConfig = PeerDiscoveryConfig(
            peerId,
            nickname,
            NetworkConstants.DEFAULT_DISCOVERY_PORT,
            chatPort,
            filePort,
            announceEnabled,
            Duration.ofSeconds(10),
            Duration.ofSeconds(45),
        )

        /**
         * Creates a listen-only configuration; the peer will receive discovery broadcasts but
         * will not send any of its own.
         *
         * Chat and file-transfer ports are set to `0` because they are not validated in
         * listen-only mode.
         *
         * @param peerId   Unique peer identifier.
         * @param nickname Display name (used only locally; not broadcast).
         * @return A [PeerDiscoveryConfig] with [announceEnabled] set to `false`.
         */
        @JvmStatic
        fun listenOnly(peerId: String, nickname: String): PeerDiscoveryConfig = PeerDiscoveryConfig(
            peerId,
            nickname,
            NetworkConstants.DEFAULT_DISCOVERY_PORT,
            0,
            0,
            false,
            Duration.ofSeconds(10),
            Duration.ofSeconds(45),
        )

        private fun validatePort(port: Int, fieldName: String) {
            require(port in 1..65_535) { "$fieldName must be between 1 and 65535" }
        }
    }
}
