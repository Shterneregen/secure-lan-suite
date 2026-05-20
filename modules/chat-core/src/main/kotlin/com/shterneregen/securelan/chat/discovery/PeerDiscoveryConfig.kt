package com.shterneregen.securelan.chat.discovery

import com.shterneregen.securelan.common.net.NetworkConstants
import java.time.Duration
import java.util.Objects

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
        @JvmStatic
        fun defaults(peerId: String, nickname: String, chatPort: Int, filePort: Int): PeerDiscoveryConfig =
            defaults(peerId, nickname, chatPort, filePort, true)

        @JvmStatic
        fun defaults(peerId: String, nickname: String, chatPort: Int, filePort: Int, announceEnabled: Boolean): PeerDiscoveryConfig =
            PeerDiscoveryConfig(
                peerId,
                nickname,
                NetworkConstants.DEFAULT_DISCOVERY_PORT,
                chatPort,
                filePort,
                announceEnabled,
                Duration.ofSeconds(10),
                Duration.ofSeconds(45),
            )

        @JvmStatic
        fun listenOnly(peerId: String, nickname: String): PeerDiscoveryConfig =
            PeerDiscoveryConfig(
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
