package com.shterneregen.securelan.desktop.compose.state.connection

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer

data class ComposeConnectionJoinTarget(
    val nickname: String,
    val host: String,
    val chatPortText: String,
    val filePortText: String,
) {
    val endpointLabel: String = "$host:$chatPortText · files $filePortText"

    companion object {
        fun fromDiscoveredPeer(peer: DiscoveredPeer): ComposeConnectionJoinTarget = ComposeConnectionJoinTarget(
            nickname = peer.nickname,
            host = peer.host,
            chatPortText = peer.chatPort.toString(),
            filePortText = peer.filePort.toString(),
        )
    }
}
