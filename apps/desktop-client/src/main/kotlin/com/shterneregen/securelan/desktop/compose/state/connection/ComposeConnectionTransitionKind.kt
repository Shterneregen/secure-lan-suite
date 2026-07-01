package com.shterneregen.securelan.desktop.compose.state.connection

enum class ComposeConnectionTransitionKind {
    START_HOSTING,
    STOP_HOSTING,
    START_MANUAL_CONNECT,
    DISCONNECT_CLIENT,
    CHANGE_DISCOVERY_VISIBILITY,
}
