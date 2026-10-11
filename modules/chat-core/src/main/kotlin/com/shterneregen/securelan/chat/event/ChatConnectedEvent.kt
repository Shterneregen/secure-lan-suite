package com.shterneregen.securelan.chat.event

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities

/**
 * Event fired on the **client side** when a TCP connection to the chat server is successfully
 * established and the secure handshake has been accepted.
 *
 * @property nickname       The nickname assigned by the server for this session (may be `null`
 *                          if the server did not echo it back).
 * @property remoteAddress  The remote address of the server (e.g. `"192.168.1.5:7777"`), or
 *                          `null` if the address could not be determined.
 * @property capabilities   The [PeerCapabilities] advertised by this client during the handshake.
 *                          Defaults to [PeerCapabilities.unknown] when not explicitly provided.
 */
@JvmRecord
data class ChatConnectedEvent(
    val nickname: String?,
    val remoteAddress: String?,
    val capabilities: PeerCapabilities,
) : ChatCoreEvent {

    /**
     * Convenience constructor that uses [PeerCapabilities.unknown] as the default capabilities.
     *
     * @param nickname      The nickname assigned by the server.
     * @param remoteAddress The remote address of the server.
     */
    constructor(nickname: String?, remoteAddress: String?) : this(nickname, remoteAddress, PeerCapabilities.unknown())
}
