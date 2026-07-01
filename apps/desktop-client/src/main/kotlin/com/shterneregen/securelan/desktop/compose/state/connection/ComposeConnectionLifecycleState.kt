package com.shterneregen.securelan.desktop.compose.state.connection

enum class ComposeConnectionLifecycleState {
    IDLE,
    HOSTING_READY,
    HOSTED,
    CONNECTING_READY,
    CONNECTED,
    BLOCKED_ERROR,
}
