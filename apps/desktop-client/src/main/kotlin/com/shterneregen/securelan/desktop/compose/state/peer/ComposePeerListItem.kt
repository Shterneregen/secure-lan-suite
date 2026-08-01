package com.shterneregen.securelan.desktop.compose.state.peer

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.desktop.ui.DesktopPeerFormatters
import com.shterneregen.securelan.desktop.ui.PeerPresence
import java.time.Instant

public data class ComposePeerListItem(
    val nickname: String,
    val online: Boolean,
    val discovered: Boolean,
    val listMeta: String,
    val selectedMeta: String,
    val filePort: Int = 0,
    val voiceCapable: Boolean = true,
    val videoCapable: Boolean = true,
    val dataChannelCapable: Boolean = true,
    val fileCapableOverride: Boolean? = null,
) {
    val fileCapable: Boolean = fileCapableOverride ?: (discovered || filePort > 0)
    val fileCapabilityAdvertised: Boolean = fileCapable
    val realtimeCapable: Boolean = voiceCapable || videoCapable || dataChannelCapable
    val availabilityLabel: String = if (online) "Online" else "Offline"
    val capabilityLabels: List<String> = buildList {
        if (voiceCapable) add("Voice")
        if (videoCapable) add("Video")
        if (fileCapabilityAdvertised) add("File")
    }
    val capabilitySummary: String = if (capabilityLabels.isEmpty()) "Chat only" else capabilityLabels.joinToString(" · ")
    val actionSummary: String = when {
        !online -> "Offline target; wait for chat or discovery refresh before enabling actions."
        !realtimeCapable && fileCapable -> "Chat and encrypted file transfer are available; voice, video, and real-time data are not."
        !realtimeCapable -> "Only chat is available; voice, video, file transfer, and real-time data are not."
        fileCapable && discovered ->
            "Chat, encrypted file transfer, voice, and experimental video are available for this peer after connection."
        fileCapable ->
            "Chat, encrypted file transfer, voice, and experimental video are available; file receiver was inferred from chat."
        else -> "Chat, voice, and experimental video are available; encrypted file transfer needs a file receiver endpoint."
    }

    companion object {
        fun fromPeer(peer: PeerPresence, clientConnected: Boolean): ComposePeerListItem {
            val capabilities = peer.capabilities()
            val unknownCapabilities = capabilities == PeerCapabilities.unknown()
            return ComposePeerListItem(
                nickname = peer.nickname(),
                online = peer.online(),
                discovered = peer.discovered() && !peer.peerId().isNullOrBlank(),
                listMeta = DesktopPeerFormatters.formatListMeta(peer),
                selectedMeta = DesktopPeerFormatters.formatSelectedPeerMeta(peer, clientConnected),
                filePort = peer.filePort(),
                voiceCapable = unknownCapabilities || capabilities.supportsVoice(),
                videoCapable = unknownCapabilities || capabilities.supportsVideo(),
                dataChannelCapable = unknownCapabilities || capabilities.supportsRtcDataChannel(),
                fileCapableOverride = DesktopMainViewHelpers.selectedPeerFileCapable(peer),
            )
        }

        fun fromDiscoveredPeer(peer: DiscoveredPeer): ComposePeerListItem = fromPeer(
            PeerPresence(
                peer.nickname,
                true,
                peer.peerId,
                peer.host,
                peer.chatPort,
                peer.filePort,
                peer.lastSeen,
            ),
            clientConnected = true,
        )

        fun defaultPreviewItems(clientConnected: Boolean): List<ComposePeerListItem> = listOf(
            fromPeer(
                PeerPresence(
                    "Astra Laptop",
                    true,
                    "peer-astra",
                    "192.168.1.20",
                    NetworkConstants.DEFAULT_CHAT_PORT,
                    NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
                    Instant.parse("2026-05-22T09:00:00Z"),
                ),
                clientConnected,
            ),
            fromPeer(
                PeerPresence(
                    "Beta Phone",
                    true,
                    "peer-beta",
                    null,
                    0,
                    0,
                    Instant.parse("2026-05-22T09:01:00Z"),
                ),
                clientConnected,
            ),
            fromPeer(
                PeerPresence(
                    "Offline NAS",
                    false,
                    "peer-nas",
                    "192.168.1.30",
                    NetworkConstants.DEFAULT_CHAT_PORT,
                    NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
                    Instant.parse("2026-05-22T08:45:00Z"),
                ),
                clientConnected,
            ),
        )
    }
}
