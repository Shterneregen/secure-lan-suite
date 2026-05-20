package com.shterneregen.securelan.chat.service

import java.util.Objects

@JvmRecord
data class ChatServerConfig(val port: Int, val sessionPassword: String) {
    init {
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }
}
