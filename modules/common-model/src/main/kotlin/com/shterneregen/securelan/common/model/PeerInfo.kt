package com.shterneregen.securelan.common.model

/**
 * Identifies a peer and the network address at which it can be reached.
 *
 * @property id peer identifier.
 * @property username peer's display name.
 * @property host hostname or IP address.
 * @property port port number in the range `1..65535`.
 * @throws IllegalArgumentException if [port] is outside the valid range.
 */
@JvmRecord
data class PeerInfo(
    val id: String,
    val username: String,
    val host: String,
    val port: Int,
) {
    init {
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }
}
