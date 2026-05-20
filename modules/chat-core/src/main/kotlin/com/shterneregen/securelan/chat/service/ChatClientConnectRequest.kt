package com.shterneregen.securelan.chat.service

import java.util.Objects

@JvmRecord
data class ChatClientConnectRequest(val host: String, val port: Int, val nickname: String, val sessionPassword: String) {
    init {
        Objects.requireNonNull(host, "host must not be null")
        Objects.requireNonNull(nickname, "nickname must not be null")
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }
}
