package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import java.util.Objects

class ChatClientConnectRequest @JvmOverloads constructor(
    val host: String,
    val port: Int,
    val nickname: String,
    val sessionPassword: String,
    val capabilities: PeerCapabilities = PeerCapabilities.unknown(),
) {
    init {
        Objects.requireNonNull(host, "host must not be null")
        Objects.requireNonNull(nickname, "nickname must not be null")
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
        Objects.requireNonNull(capabilities, "capabilities must not be null")
        require(port in 1..65_535) { "port must be between 1 and 65535" }
    }

    fun host(): String = host
    fun port(): Int = port
    fun nickname(): String = nickname
    fun sessionPassword(): String = sessionPassword
    fun capabilities(): PeerCapabilities = capabilities

    override fun equals(other: Any?): Boolean = this === other ||
        other is ChatClientConnectRequest &&
        host == other.host &&
        port == other.port &&
        nickname == other.nickname &&
        sessionPassword == other.sessionPassword &&
        capabilities == other.capabilities

    override fun hashCode(): Int = Objects.hash(host, port, nickname, sessionPassword, capabilities)

    override fun toString(): String = "ChatClientConnectRequest[host=$host, port=$port, nickname=$nickname, capabilities=$capabilities]"
}
