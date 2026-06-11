package com.shterneregen.securelan.chat.protocol.handshake

import java.util.Objects

class HandshakeRequest @JvmOverloads constructor(
    val nickname: String,
    val sessionPassword: String,
    val capabilities: PeerCapabilities = PeerCapabilities.unknown(),
) {
    init {
        Objects.requireNonNull(nickname, "nickname must not be null")
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
        Objects.requireNonNull(capabilities, "capabilities must not be null")
    }

    fun nickname(): String = nickname
    fun sessionPassword(): String = sessionPassword
    fun capabilities(): PeerCapabilities = capabilities

    override fun equals(other: Any?): Boolean = this === other ||
        other is HandshakeRequest && nickname == other.nickname && sessionPassword == other.sessionPassword && capabilities == other.capabilities

    override fun hashCode(): Int = Objects.hash(nickname, sessionPassword, capabilities)

    override fun toString(): String = "HandshakeRequest[nickname=$nickname, capabilities=$capabilities]"
}
