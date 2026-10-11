package com.shterneregen.securelan.chat.event

/**
 * Well-known human-readable reason strings used in [ChatDisconnectedEvent.reason].
 *
 * These constants cover the three canonical disconnection scenarios:
 * - The local user requested a disconnect ([CLIENT_REQUEST]).
 * - The server (room host) closed the connection ([REMOTE_HOST_CLOSED]).
 * - The underlying network connection was lost unexpectedly ([CONNECTION_LOST]).
 */
object ChatDisconnectReasons {

    /** The local user explicitly requested to leave the chat. */
    const val CLIENT_REQUEST: String = "Disconnected by user"

    /** The server closed the chat room, terminating all client connections. */
    const val REMOTE_HOST_CLOSED: String = "Room closed by host"

    /** The connection was dropped due to a network error or timeout. */
    const val CONNECTION_LOST: String = "Connection lost"
}
