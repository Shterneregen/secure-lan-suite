package com.shterneregen.securelan.chat.protocol.handshake

import java.util.Objects

/**
 * The server's response to a [HandshakeRequest].
 *
 * Contains the outcome of handshake validation ([status]), the canonicalized nickname assigned
 * by the server ([nickname]), a human-readable rejection reason ([reason]), and the server's own
 * [PeerCapabilities] advertisement.
 *
 * @property status       Whether the handshake was [HandshakeStatus.ACCEPTED] or [HandshakeStatus.REJECTED].
 * @property nickname     The server-assigned (or echoed) nickname; empty when rejected.
 * @property reason       A human-readable reason for rejection; empty when accepted.
 * @property capabilities The server's [PeerCapabilities]; defaults to [PeerCapabilities.unknown].
 * @throws NullPointerException if [status] or [capabilities] is `null`.
 */
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

    /** Returns the handshake outcome. */
    fun status(): HandshakeStatus = status

    /** Returns the server-assigned (or echoed) nickname. */
    fun nickname(): String = nickname

    /** Returns the rejection reason, or an empty string if the handshake was accepted. */
    fun reason(): String = reason

    /** Returns the server's [PeerCapabilities]. */
    fun capabilities(): PeerCapabilities = capabilities

    /**
     * Returns `true` if [status] is [HandshakeStatus.ACCEPTED], `false` otherwise.
     *
     * This is the primary guard used by client and server code after a handshake:
     * ```kotlin
     * if (!response.accepted()) {
     *     // handle rejection
     * }
     * ```
     */
    fun accepted(): Boolean = status == HandshakeStatus.ACCEPTED

    override fun equals(other: Any?): Boolean =
        this === other || (other is HandshakeResponse &&
            status == other.status &&
            nickname == other.nickname &&
            reason == other.reason &&
            capabilities == other.capabilities)

    override fun hashCode(): Int = Objects.hash(status, nickname, reason, capabilities)

    override fun toString(): String =
        "HandshakeResponse[status=$status, nickname=$nickname, reason=$reason, capabilities=$capabilities]"
}
