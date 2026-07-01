package com.shterneregen.securelan.desktop.compose.state.connection

enum class ComposeAdapterEventKind {
    HOST_STARTED,
    HOST_STOPPED,
    CONNECT_STARTED,
    CONNECTED,
    CONNECT_FAILED,
    DISCONNECTED,
    DISCOVERY_VISIBILITY_CHANGED,
    RUNTIME_ERROR,
    CLEANUP_STARTED,
    CLEANUP_COMPLETED,
}
