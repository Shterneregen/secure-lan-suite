package com.shterneregen.securelan.chat.service

import java.util.Objects

/**
 * Immutable configuration for a [ChatServerService] instance.
 *
 * @property port             The TCP port on which the server will listen for incoming connections
 *                            (1–65 535).
 * @property sessionPassword  The password that connecting clients must supply during the handshake.
 *
 * @throws IllegalArgumentException if [port] is not in the 1–65 535 range.
 * @throws NullPointerException     if [sessionPassword] is `null`.
 */
@JvmRecord
data class ChatServerConfig(val port: Int, val sessionPassword: String) {
    init {
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }
}
