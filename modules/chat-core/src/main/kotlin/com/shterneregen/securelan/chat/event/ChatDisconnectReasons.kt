package com.shterneregen.securelan.chat.event

object ChatDisconnectReasons {
    const val CLIENT_REQUEST: String = "Disconnected by user"
    const val REMOTE_HOST_CLOSED: String = "Room closed by host"
    const val CONNECTION_LOST: String = "Connection lost"
}
