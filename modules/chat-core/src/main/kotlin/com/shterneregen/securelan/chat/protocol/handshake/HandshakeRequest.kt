package com.shterneregen.securelan.chat.protocol.handshake

import java.util.Objects

/**
 * The client-side handshake request sent to the chat server at the beginning of a session.
 *
 * The request is encrypted with the server's RSA public key and carries:
 * - The desired [nickname] for this session.
 * - The [sessionPassword] required to join the room.
 * - The client's [capabilities], which describe what features this peer supports.
 *
 * @property nickname         The nickname the client wishes to use in the chat room.
 * @property sessionPassword  The room password; compared against the server's expected password
 *                            during handshake validation.
 * @property capabilities     The [PeerCapabilities] advertised by this client.
 *                            Defaults to [PeerCapabilities.unknown] when not specified.
 * @throws NullPointerException if [nickname], [sessionPassword], or [capabilities] is `null`.
 */
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

    // Java-friendly accessor aliases kept for binary compatibility.
    /** Returns [nickname]. Prefer the property in Kotlin. */
    fun nickname(): String = nickname

    /** Returns [sessionPassword]. Prefer the property in Kotlin. */
    fun sessionPassword(): String = sessionPassword

    /** Returns [capabilities]. Prefer the property in Kotlin. */
    fun capabilities(): PeerCapabilities = capabilities

    override fun equals(other: Any?): Boolean = this === other ||
        other is HandshakeRequest &&
        nickname == other.nickname &&
        sessionPassword == other.sessionPassword &&
        capabilities == other.capabilities

    override fun hashCode(): Int = Objects.hash(nickname, sessionPassword, capabilities)

    /** Returns a string representation that omits the session password for security reasons. */
    override fun toString(): String = "HandshakeRequest[nickname=$nickname, capabilities=$capabilities]"
}
