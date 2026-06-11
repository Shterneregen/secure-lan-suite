package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities

object DesktopPeerFormatters {
    @JvmStatic
    fun formatListMeta(peer: PeerPresence): String = when {
        !peer.online() -> if (peer.discovered()) "offline • ${peer.host()}" else "offline"
        peer.discovered() -> "${platformLabel(peer)} • ${peer.host()}:${peer.chatPort()} • file ${peer.filePort()}"
        else -> {
            val capabilities = peer.capabilities()
            val platform = platformLabel(peer).takeUnless { it == "chat" }
            listOfNotNull(
                platform,
                "chat",
                if (capabilities == PeerCapabilities.unknown() || capabilities.supportsVoice()) "voice" else null,
                if (capabilities == PeerCapabilities.unknown() || capabilities.supportsVideo()) "video" else null,
                if (capabilities == PeerCapabilities.unknown() || peer.hasFileEndpoint()) "file" else null,
            ).joinToString(" • ")
        }
    }

    @JvmStatic
    fun formatSelectedPeerMeta(peer: PeerPresence, clientConnected: Boolean): String = if (peer.online()) {
        if (clientConnected) {
            val capabilities = peer.capabilities()
            if (capabilities != PeerCapabilities.unknown() && !peer.host().isNullOrBlank() && peer.filePort() > 0) {
                "Online ${platformLabel(peer)} peer — file receiver advertised at ${peer.host()}:${peer.filePort()} by capabilities metadata."
            } else if (peer.peerId().isNullOrBlank() && !peer.host().isNullOrBlank() && peer.filePort() > 0) {
                "Online via chat — file receiver inferred at ${peer.host()}:${peer.filePort()} for Android/client peers."
            } else if (peer.discovered()) {
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

    private fun platformLabel(peer: PeerPresence): String = when (peer.capabilities().platformKind()) {
        PeerCapabilities.PlatformKind.ANDROID -> "Android"
        PeerCapabilities.PlatformKind.DESKTOP -> "desktop"
        PeerCapabilities.PlatformKind.UNKNOWN -> if (peer.discovered()) "discovered" else "chat"
    }
}
