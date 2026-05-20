package com.shterneregen.securelan.chat.protocol

enum class WireMessageType {
    HELLO,
    SERVER_KEY,
    CLIENT_KEY,
    ACCEPTED,
    REJECTED,
    CHAT,
    SYSTEM,
    USER_JOINED,
    USER_LEFT,
    SIGNAL,
    DISCONNECT,
}
