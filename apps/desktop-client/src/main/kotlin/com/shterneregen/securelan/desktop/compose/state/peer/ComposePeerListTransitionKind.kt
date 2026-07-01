package com.shterneregen.securelan.desktop.compose.state.peer

public enum class ComposePeerListTransitionKind {
    SELECT_PEER,
    DESELECT_PEER,
    TARGET_PEER_FOR_CHAT,
    TARGET_PEER_FOR_FILE,
    TARGET_PEER_FOR_VOICE,
    TARGET_PEER_FOR_VIDEO,
    TARGET_PEER_FOR_DATA,
    REFRESH_PEER_LIST,
}
