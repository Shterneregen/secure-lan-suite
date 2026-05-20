package com.shterneregen.securelan.common.model

import java.util.Objects

@JvmRecord
data class PeerInfo(
    val id: String,
    val username: String,
    val host: String,
    val port: Int,
) {
    init {
        Objects.requireNonNull(id, "id must not be null")
        Objects.requireNonNull(username, "username must not be null")
        Objects.requireNonNull(host, "host must not be null")
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }
}
