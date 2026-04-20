package com.shterneregen.securelan.chat.protocol;

public enum WireMessageType {
    HELLO,
    SERVER_KEY,
    CLIENT_KEY,
    ACCEPTED,
    REJECTED,
    CHAT,
    SYSTEM,
    SIGNAL,
    DISCONNECT
}
