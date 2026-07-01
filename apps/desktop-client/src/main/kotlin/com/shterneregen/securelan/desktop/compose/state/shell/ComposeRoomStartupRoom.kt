package com.shterneregen.securelan.desktop.compose.state.shell

data class ComposeRoomStartupRoom(
    val name: String,
    val host: String,
    val chatPortText: String,
    val filePortText: String,
    val capabilitySummary: String,
    val selected: Boolean,
) {
    val title: String = name
    val subtitle: String = "$capabilitySummary · $host"
    val endpointLabel: String = "$host:$chatPortText"
}
