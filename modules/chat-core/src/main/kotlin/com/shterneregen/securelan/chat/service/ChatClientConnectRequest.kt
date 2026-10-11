package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import java.util.Objects

/**
 * Immutable value object carrying all the parameters needed to connect a chat client to a server.
 *
 * @property host             The hostname or IP address of the chat server.
 * @property port             The TCP port of the chat server (1–65 535).
 * @property nickname         The nickname the client wishes to use in the room.
 * @property sessionPassword  The room password required by the server.
 * @property capabilities     The [PeerCapabilities] this client will advertise during the handshake.
 *                            Defaults to [PeerCapabilities.unknown] when not specified.
 *
 * @throws IllegalArgumentException if [port] is not in the 1–65 535 range.
 * @throws NullPointerException     if [host], [nickname], [sessionPassword], or [capabilities] is `null`.
 */
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

    // Java-friendly accessor aliases kept for binary compatibility.
    /** Returns [host]. Prefer the property in Kotlin. */
    fun host(): String = host

    /** Returns [port]. Prefer the property in Kotlin. */
    fun port(): Int = port

    /** Returns [nickname]. Prefer the property in Kotlin. */
    fun nickname(): String = nickname

    /** Returns [sessionPassword]. Prefer the property in Kotlin. */
    fun sessionPassword(): String = sessionPassword

    /** Returns [capabilities]. Prefer the property in Kotlin. */
    fun capabilities(): PeerCapabilities = capabilities

    override fun equals(other: Any?): Boolean = this === other ||
        other is ChatClientConnectRequest &&
        host == other.host &&
        port == other.port &&
        nickname == other.nickname &&
        sessionPassword == other.sessionPassword &&
        capabilities == other.capabilities

    override fun hashCode(): Int = Objects.hash(host, port, nickname, sessionPassword, capabilities)

    /** Returns a string representation that omits the session password for security reasons. */
    override fun toString(): String =
        "ChatClientConnectRequest[host=$host, port=$port, nickname=$nickname, capabilities=$capabilities]"
}
