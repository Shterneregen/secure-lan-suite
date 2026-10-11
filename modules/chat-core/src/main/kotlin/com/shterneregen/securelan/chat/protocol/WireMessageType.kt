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
 * | [SERVER_KEY]| server → client | Server's RSA public key, Base64-encoded. |
 * | [CLIENT_KEY]| client → server | RSA-encrypted payload: nickname, password, AES session key, and capabilities. |
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

    /** Server RSA public key frame (Base64-encoded DER). */
    SERVER_KEY,

    /** Client RSA-encrypted payload frame (Base64-encoded ciphertext). */
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
}
