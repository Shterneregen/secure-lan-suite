package com.shterneregen.securelan.common.model

/**
 * Network address and security preference for a connection.
 *
 * @property host hostname or IP address of the remote endpoint.
 * @property port port number in the range `1..65535`.
 * @property secure whether a secure connection is requested.
 * @throws IllegalArgumentException if [port] is outside the valid range.
 */
@JvmRecord
data class ConnectionInfo(
    val host: String,
    val port: Int,
    val secure: Boolean,
) {
    init {
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }
}
