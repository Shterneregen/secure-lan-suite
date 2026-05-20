package com.shterneregen.securelan.common.model

import java.util.Objects

@JvmRecord
data class ConnectionInfo(
    val host: String,
    val port: Int,
    val secure: Boolean,
) {
    init {
        Objects.requireNonNull(host, "host must not be null")
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }
}
