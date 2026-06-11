package com.shterneregen.securelan.chat.protocol.handshake

import java.util.Objects

class HandshakeResponse @JvmOverloads constructor(
    status: HandshakeStatus,
    nickname: String?,
    reason: String?,
    capabilities: PeerCapabilities = PeerCapabilities.unknown(),
) {
    private val status: HandshakeStatus = Objects.requireNonNull(status, "status must not be null")
    private val nickname: String = nickname ?: ""
    private val reason: String = reason ?: ""
    private val capabilities: PeerCapabilities = Objects.requireNonNull(capabilities, "capabilities must not be null")

    fun status(): HandshakeStatus = status
    fun nickname(): String = nickname
    fun reason(): String = reason
    fun capabilities(): PeerCapabilities = capabilities
    fun accepted(): Boolean = status == HandshakeStatus.ACCEPTED

    override fun equals(other: Any?): Boolean =
        this === other || (other is HandshakeResponse && status == other.status && nickname == other.nickname && reason == other.reason && capabilities == other.capabilities)

    override fun hashCode(): Int = Objects.hash(status, nickname, reason, capabilities)

    override fun toString(): String = "HandshakeResponse[status=$status, nickname=$nickname, reason=$reason, capabilities=$capabilities]"
}
