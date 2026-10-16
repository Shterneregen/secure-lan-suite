package com.shterneregen.securelan.chat.protocol

/**
 * Enumeration of all message types exchanged over the chat wire protocol.
 *
 * Message types are grouped by their role in the protocol lifecycle:
 *
 * ### Handshake messages
 * | Type        | Direction     | Description |
 * |-------------|---------------|-------------|
 * | [HELLO]     | client → server | First frame sent by the client; carries the nickname and protocol version token. |
 * | [SERVER_KEY]| server → client | V2 SRP challenge or server evidence proof. |
 * | [CLIENT_KEY]| client → server | V2 SRP public value and client evidence proof. |
 * | [ACCEPTED]  | server → client | Handshake accepted; payload contains the server-assigned nickname. |
 * | [REJECTED]  | server → client | Handshake rejected; payload contains a human-readable reason. |
 *
 * ### Session messages
 * | Type         | Direction          | Description |
 * |--------------|--------------------|-------------|
 * | [CHAT]       | bidirectional      | A user-typed chat message. |
 * | [SYSTEM]     | server → client    | A server-generated system announcement (e.g. "Alice joined"). |
 * | [USER_JOINED]| server → client    | A peer has joined; payload is the encoded [com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities]. |
 * | [USER_LEFT]  | server → client    | A peer has left the room. |
 * | [SIGNAL]     | bidirectional      | A WebRTC signalling envelope (SDP offer/answer or ICE candidate). |
 * | [DISCONNECT] | client → server    | Graceful disconnect notification; the client will close the socket afterwards. |
 */
enum class WireMessageType {
    /** First frame sent by the client during the handshake. */
    HELLO,

    /** V2 SRP challenge or server evidence proof; never an unauthenticated RSA key. */
    SERVER_KEY,

    /** V2 SRP public value and client evidence proof; contains no password/key envelope. */
    CLIENT_KEY,

    /** Server confirmation that the handshake was accepted. */
    ACCEPTED,

    /** Server rejection frame; payload contains the rejection reason. */
    REJECTED,

    /** A user chat message. */
    CHAT,

    /** A server-generated system notification. */
    SYSTEM,

    /** Notification that a user has joined the room; payload contains encoded peer capabilities. */
    USER_JOINED,

    /** Notification that a user has left the room. */
    USER_LEFT,

    /** A WebRTC signalling message (SDP or ICE). */
    SIGNAL,

    /** Graceful disconnect notification from a client. */
    DISCONNECT,

    /** Negotiated encrypted session health probe and reply. */
    PING,
    PONG,
}
