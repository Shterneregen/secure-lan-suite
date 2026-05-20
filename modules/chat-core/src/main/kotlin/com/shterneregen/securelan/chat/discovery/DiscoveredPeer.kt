package com.shterneregen.securelan.chat.discovery

import java.time.Instant
import java.util.Objects

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
