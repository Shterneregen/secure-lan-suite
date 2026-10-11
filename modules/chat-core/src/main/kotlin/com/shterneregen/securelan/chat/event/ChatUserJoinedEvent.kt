package com.shterneregen.securelan.chat.event

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities

/**
 * Event fired when a new peer joins the chat room.
 *
 * On the **server side** this is emitted after a client has successfully completed the handshake.
 * On the **client side** this is emitted both when a peer joins while the client is connected
 * and during the initial peer-list synchronisation that happens right after the client connects.
 *
 * @property nickname       The nickname of the peer who joined, or `null` if unknown.
 * @property remoteAddress  The remote address of the peer (e.g. `"192.168.1.10:54321"`), or
 *                          `null` if the address is not available.
 * @property capabilities   The [PeerCapabilities] advertised by the joining peer.
 *                          Defaults to [PeerCapabilities.unknown] when not provided.
 */
@JvmRecord
data class ChatUserJoinedEvent(
    val nickname: String?,
    val remoteAddress: String?,
    val capabilities: PeerCapabilities,
) : ChatCoreEvent {

    /**
     * Convenience constructor that uses an empty remote address and [PeerCapabilities.unknown].
     *
     * @param nickname The nickname of the peer who joined.
     * @param remoteAddress The remote address of the joining peer.
     */
    constructor(nickname: String?, remoteAddress: String?) : this(nickname, remoteAddress, PeerCapabilities.unknown())

    /**
     * Convenience constructor for cases where only the nickname is available.
     * Sets [remoteAddress] to an empty string and [capabilities] to [PeerCapabilities.unknown].
     *
     * @param nickname The nickname of the peer who joined.
     */
    constructor(nickname: String?) : this(nickname, "", PeerCapabilities.unknown())
}
