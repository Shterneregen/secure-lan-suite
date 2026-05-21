package com.shterneregen.securelan.desktop.ui

object DesktopPeerFormatters {
    @JvmStatic
    fun formatListMeta(peer: PeerPresence): String = when {
        !peer.online() -> if (peer.discovered()) "offline • ${peer.host()}" else "offline"
        peer.discovered() -> "discovered • ${peer.host()}:${peer.chatPort()} • file ${peer.filePort()}"
        else -> "chat • voice • video • file"
    }

    @JvmStatic
    fun formatSelectedPeerMeta(peer: PeerPresence, clientConnected: Boolean): String = if (peer.online()) {
        if (clientConnected) {
            if (peer.discovered()) {
                "Online via chat and LAN discovery — ${peer.host()}:${peer.chatPort()} chat, ${peer.filePort()} file."
            } else {
                "Online in chat — voice and video are available."
            }
        } else if (peer.discovered()) {
            "Discovered via LAN — connect to chat before sending files or starting calls."
        } else {
            "Online candidate — connect to chat before starting voice or video."
        }
    } else {
        "Offline — wait until this peer rejoins the chat or discovery refreshes."
    }
}
