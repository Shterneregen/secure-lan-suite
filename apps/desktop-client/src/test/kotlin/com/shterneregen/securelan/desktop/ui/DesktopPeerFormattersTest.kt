package com.shterneregen.securelan.desktop.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class DesktopPeerFormattersTest {
    @Test
    fun shouldFormatOfflineListMetaWithoutDiscovery() {
        assertEquals("offline", DesktopPeerFormatters.formatListMeta(peer(online = false)))
    }

    @Test
    fun shouldFormatOfflineListMetaWithDiscoveryHost() {
        assertEquals("offline • 192.168.1.20", DesktopPeerFormatters.formatListMeta(discoveredPeer(online = false)))
    }

    @Test
    fun shouldFormatDiscoveredOnlineListMeta() {
        assertEquals(
            "discovered • 192.168.1.20:5555 • file 5556",
            DesktopPeerFormatters.formatListMeta(discoveredPeer()),
        )
    }

    @Test
    fun shouldFormatChatOnlyOnlineListMeta() {
        assertEquals("chat • voice • video • file", DesktopPeerFormatters.formatListMeta(peer()))
    }

    @Test
    fun shouldFormatSelectedDiscoveredPeerWhileConnected() {
        assertEquals(
            "Online via chat and LAN discovery — 192.168.1.20:5555 chat, 5556 file.",
            DesktopPeerFormatters.formatSelectedPeerMeta(discoveredPeer(), clientConnected = true),
        )
    }

    @Test
    fun shouldFormatSelectedChatOnlyPeerWhileConnected() {
        assertEquals(
            "Online in chat — voice and video are available.",
            DesktopPeerFormatters.formatSelectedPeerMeta(peer(), clientConnected = true),
        )
    }

    @Test
    fun shouldFormatSelectedInferredFilePeerWhileConnected() {
        assertEquals(
            "Online via chat — file receiver inferred at 192.168.1.30:6051 for Android/client peers.",
            DesktopPeerFormatters.formatSelectedPeerMeta(inferredFilePeer(), clientConnected = true),
        )
    }

    @Test
    fun shouldFormatSelectedDiscoveredPeerBeforeChatConnection() {
        assertEquals(
            "Discovered via LAN — connect to chat before sending files or starting calls.",
            DesktopPeerFormatters.formatSelectedPeerMeta(discoveredPeer(), clientConnected = false),
        )
    }

    @Test
    fun shouldFormatSelectedOnlineCandidateBeforeChatConnection() {
        assertEquals(
            "Online candidate — connect to chat before starting voice or video.",
            DesktopPeerFormatters.formatSelectedPeerMeta(peer(), clientConnected = false),
        )
    }

    @Test
    fun shouldFormatSelectedOfflinePeer() {
        assertEquals(
            "Offline — wait until this peer rejoins the chat or discovery refreshes.",
            DesktopPeerFormatters.formatSelectedPeerMeta(discoveredPeer(online = false), clientConnected = true),
        )
    }

    private fun peer(online: Boolean = true): PeerPresence = PeerPresence(
        "Alice",
        online,
        null,
        null,
        0,
        0,
        Instant.parse("2026-05-22T09:00:00Z"),
    )

    private fun discoveredPeer(online: Boolean = true): PeerPresence = PeerPresence(
        "Alice",
        online,
        "peer-1",
        "192.168.1.20",
        5555,
        5556,
        Instant.parse("2026-05-22T09:00:00Z"),
    )

    private fun inferredFilePeer(): PeerPresence = PeerPresence(
        "Android Phone",
        true,
        null,
        "192.168.1.30",
        5050,
        6051,
        Instant.parse("2026-05-22T09:00:00Z"),
    )
}
