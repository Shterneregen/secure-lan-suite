package com.shterneregen.securelan.chat.protocol.handshake

import java.util.Objects

@JvmRecord
data class HandshakeRequest(val nickname: String, val sessionPassword: String) {
    init {
        Objects.requireNonNull(nickname, "nickname must not be null")
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
    }
}
