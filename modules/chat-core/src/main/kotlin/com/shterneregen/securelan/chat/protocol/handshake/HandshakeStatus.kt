package com.shterneregen.securelan.chat.protocol.handshake

/**
 * The outcome of a handshake exchange between a chat client and server.
 *
 * @see HandshakeResponse.accepted
 */
enum class HandshakeStatus {
    /** The handshake was accepted; the client may proceed with the session. */
    ACCEPTED,

    /** The handshake was rejected; the client should read [HandshakeResponse.reason] for details. */
    REJECTED,
}
