package com.shterneregen.securelan.desktop.compose.state.chat

internal enum class ComposeCallWorkspaceFocusMode(
    val showsVideo: Boolean,
    val showsChat: Boolean,
    val splitResizable: Boolean,
) {
    SPLIT(showsVideo = true, showsChat = true, splitResizable = true),
    VIDEO(showsVideo = true, showsChat = false, splitResizable = false),
    CHAT(showsVideo = false, showsChat = true, splitResizable = false),
}
