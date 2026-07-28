package com.shterneregen.securelan.desktop.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PeerPresenceTest {
    @Test
    fun shouldExposeInitialPeerState() {
        val lastSeen = Instant.parse("2026-05-21T10:00:00Z")
        val peer = PeerPresence("Alice", true, "peer-1", "192.168.1.10", 5050, 6060, lastSeen)

        assertEquals("Alice", peer.nickname())
        assertTrue(peer.online())
        assertEquals("peer-1", peer.peerId())
        assertEquals("192.168.1.10", peer.host())
        assertEquals(5050, peer.chatPort())
        assertEquals(6060, peer.filePort())
        assertEquals(lastSeen, peer.lastSeen())
        assertTrue(peer.discovered())
    }

    @Test
    fun shouldIgnoreBlankOrNonPositiveDiscoveryUpdates() {
        val peer = PeerPresence("Bob", true, "peer-1", "192.168.1.10", 5050, 6060, null)

        assertFalse(peer.apply(true, " ", " ", 0, -1, null))
        assertEquals("peer-1", peer.peerId())
        assertEquals("192.168.1.10", peer.host())
        assertEquals(5050, peer.chatPort())
        assertEquals(6060, peer.filePort())
        assertTrue(peer.discovered())
    }

    @Test
    fun shouldApplyMeaningfulPeerUpdates() {
        val lastSeen = Instant.parse("2026-05-21T10:01:00Z")
        val peer = PeerPresence("Carol", false, null, null, 0, 0, null)

        assertTrue(peer.apply(true, "peer-2", "10.0.0.8", 7000, 8000, lastSeen))
        assertTrue(peer.online())
        assertEquals("peer-2", peer.peerId())
        assertEquals("10.0.0.8", peer.host())
        assertEquals(7000, peer.chatPort())
        assertEquals(8000, peer.filePort())
        assertEquals(lastSeen, peer.lastSeen())
        assertTrue(peer.discovered())
    }

    @Test
    fun shouldMarkOnlinePeerOfflineOnce() {
        val peer = PeerPresence("Dave", true, null, null, 0, 0, null)

        assertTrue(peer.markOffline())
        assertFalse(peer.online())
        assertFalse(peer.markOffline())
    }
}
