package com.shterneregen.securelan.chat.protocol.handshake

import java.util.Objects

class HandshakeResponse(status: HandshakeStatus, nickname: String?, reason: String?) {
    private val status: HandshakeStatus = Objects.requireNonNull(status, "status must not be null")
    private val nickname: String = nickname ?: ""
    private val reason: String = reason ?: ""

    fun status(): HandshakeStatus = status
    fun nickname(): String = nickname
    fun reason(): String = reason
    fun accepted(): Boolean = status == HandshakeStatus.ACCEPTED

    override fun equals(other: Any?): Boolean =
        this === other || (other is HandshakeResponse && status == other.status && nickname == other.nickname && reason == other.reason)

    override fun hashCode(): Int = Objects.hash(status, nickname, reason)

    override fun toString(): String = "HandshakeResponse[status=$status, nickname=$nickname, reason=$reason]"
}
