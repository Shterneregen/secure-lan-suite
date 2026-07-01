package com.shterneregen.securelan.desktop.compose.state.peer

public enum class ComposePeerListLifecycleState {
    IDLE,
    DISCOVERING,
    PEERS_VISIBLE,
    PEER_SELECTED,
    PEER_TARGETED,
    BLOCKED_ERROR,
}
