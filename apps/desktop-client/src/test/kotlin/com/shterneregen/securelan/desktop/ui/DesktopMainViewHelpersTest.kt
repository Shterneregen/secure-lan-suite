package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.stego.model.BmpCapacity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant

class DesktopMainViewHelpersTest {
    @Test
    fun shouldSuggestStegoOutputPathNextToBmpCover() {
        val cover = Path.of("images", "cover.bmp")

        assertEquals(
            Path.of("images", "cover-stego.bmp").toAbsolutePath().normalize(),
            DesktopMainViewHelpers.suggestedStegoOutputPath(cover),
        )
    }

    @Test
    fun shouldSuggestStegoOutputPathForUppercaseBmpCover() {
        val cover = Path.of("images", "Cover.BMP")

        assertEquals(
            Path.of("images", "Cover-stego.bmp").toAbsolutePath().normalize(),
            DesktopMainViewHelpers.suggestedStegoOutputPath(cover),
        )
    }

    @Test
    fun shouldAppendStegoBmpWhenCoverHasAnotherExtension() {
        val cover = Path.of("cover.png")

        assertEquals(
            Path.of("cover.png-stego.bmp").toAbsolutePath().normalize(),
            DesktopMainViewHelpers.suggestedStegoOutputPath(cover),
        )
    }

    @Test
    fun shouldKeepBmpExtensionWhenEnsuringBmpPath() {
        val path = Path.of("images", "cover.BMP").toAbsolutePath().normalize()

        assertEquals(path, DesktopMainViewHelpers.ensureBmpExtension(path))
    }

    @Test
    fun shouldAppendBmpExtensionWhenEnsuringBmpPath() {
        assertEquals(
            Path.of("images", "cover.png.bmp").toAbsolutePath().normalize(),
            DesktopMainViewHelpers.ensureBmpExtension(Path.of("images", "cover.png")),
        )
    }

    @Test
    fun shouldFormatStegoCapacity() {
        val capacity = BmpCapacity(320, 240, 24, 230_400, 54, 7_680)

        assertEquals(
            "Capacity: 7680 bytes payload in 320x240 24-bit BMP",
            DesktopMainViewHelpers.formatStegoCapacity(capacity),
        )
    }

    @Test
    fun shouldUseServerFilePortWhenServerRuns() {
        assertEquals(
            5556,
            DesktopMainViewHelpers.resolveLocalFilePort(
                serverRunning = true,
                serverFilePortText = "5556",
                clientFilePortText = "65500",
                defaultFileTransferPort = 6001,
                clientFilePortOffset = 1000,
            ),
        )
    }

    @Test
    fun shouldOffsetClientRemoteFilePortWhenServerDoesNotRun() {
        assertEquals(
            7001,
            DesktopMainViewHelpers.resolveLocalFilePort(
                serverRunning = false,
                serverFilePortText = "5556",
                clientFilePortText = "6001",
                defaultFileTransferPort = 6001,
                clientFilePortOffset = 1000,
            ),
        )
    }

    @Test
    fun shouldFallbackWhenOffsetClientFilePortExceedsMaximumPort() {
        assertEquals(
            7001,
            DesktopMainViewHelpers.resolveLocalFilePort(
                serverRunning = false,
                serverFilePortText = "5556",
                clientFilePortText = "65000",
                defaultFileTransferPort = 6001,
                clientFilePortOffset = 1000,
            ),
        )
    }

    @Test
    fun shouldInferClientFilePortFromHostedFilePort() {
        assertEquals(
            6556,
            DesktopMainViewHelpers.resolveInferredClientFilePort(
                hostFilePortText = "5556",
                defaultFileTransferPort = 6001,
                clientFilePortOffset = 1000,
            ),
        )
    }

    @Test
    fun shouldBlockCallablePeerActionsForAndroidCapabilities() {
        val androidPeer = PeerPresence(
            "Android Phone",
            true,
            "android-peer",
            "192.168.1.30",
            5555,
            7001,
            Instant.parse("2026-05-22T09:00:00Z"),
            PeerCapabilities.android("0.5.0", 7001),
        )
        val compatibilityPeer = PeerPresence(
            "Compatibility Client",
            true,
            null,
            null,
            0,
            0,
            Instant.parse("2026-05-22T09:01:00Z"),
        )

        assertFalse(DesktopMainViewHelpers.selectedPeerCallable(androidPeer))
        assertTrue(DesktopMainViewHelpers.selectedPeerCallable(compatibilityPeer))
    }

    @Test
    fun shouldFallbackWhenInferredClientFilePortExceedsMaximumPort() {
        assertEquals(
            7001,
            DesktopMainViewHelpers.resolveInferredClientFilePort(
                hostFilePortText = "65000",
                defaultFileTransferPort = 6001,
                clientFilePortOffset = 1000,
            ),
        )
    }

    @Test
    fun shouldPreferFileTransferErrorCauseMessage() {
        val error = IllegalStateException("wrapper", IllegalArgumentException("bad port"))

        assertEquals("bad port", DesktopMainViewHelpers.fileTransferErrorMessage(error))
    }

    @Test
    fun shouldUseErrorClassNameWhenFileTransferMessageIsBlank() {
        assertEquals("IllegalArgumentException", DesktopMainViewHelpers.fileTransferErrorMessage(IllegalArgumentException("   ")))
    }

    @Test
    fun shouldExtractIpv4HostFromRemoteAddress() {
        assertEquals("192.168.1.20", DesktopMainViewHelpers.hostFromRemoteAddress("/192.168.1.20:5555"))
    }

    @Test
    fun shouldExtractIpv6HostFromRemoteAddress() {
        assertEquals("fe80::1", DesktopMainViewHelpers.hostFromRemoteAddress("/[fe80::1]:5555"))
    }

    @Test
    fun shouldReturnBlankHostForBlankRemoteAddress() {
        assertEquals("", DesktopMainViewHelpers.hostFromRemoteAddress("   "))
    }

    @Test
    fun shouldMatchSamePeerByPeerIdWhenBothPeerIdsArePresent() {
        assertTrue(DesktopMainViewHelpers.samePeer(peer(peerId = "peer-1"), nickname = "Bob", peerId = "peer-1"))
    }

    @Test
    fun shouldNotFallbackToNicknameWhenBothPeerIdsDiffer() {
        assertFalse(DesktopMainViewHelpers.samePeer(peer(peerId = "peer-1"), nickname = "Alice", peerId = "peer-2"))
    }

    @Test
    fun shouldFallbackToCaseInsensitiveNicknameWhenPeerIdIsMissing() {
        assertTrue(DesktopMainViewHelpers.samePeer(peer(peerId = null), nickname = "alice", peerId = null))
    }

    @Test
    fun shouldKeepDiscoveryEndpointWhenChatPresenceHasOnlyCapabilities() {
        val capabilities = PeerCapabilities.desktop("0.5.0", 5556)
        val chatPeer = PeerPresence("vbook", true, null, null, 0, 5556, null, capabilities)
        val discoveredPeer = DiscoveredPeer(
            "peer-vbook",
            "vbook",
            "192.168.0.10",
            5555,
            5556,
            Instant.parse("2026-08-02T12:00:00Z"),
        )

        val merged = DesktopMainViewHelpers.mergeChatAndDiscoveredPeer(chatPeer, discoveredPeer)

        assertEquals("peer-vbook", merged.peerId())
        assertEquals("192.168.0.10", merged.host())
        assertEquals(5555, merged.chatPort())
        assertEquals(5556, merged.filePort())
        assertEquals(capabilities, merged.capabilities())
        assertTrue(DesktopMainViewHelpers.selectedPeerFileCapable(merged))
    }

    @Test
    fun shouldReuseRunningListenOnlyDiscoveryWhenNicknameChanges() {
        val active = PeerDiscoveryConfig.listenOnly("local-peer", "omen-before-connect")
        val requested = PeerDiscoveryConfig.listenOnly("local-peer", "omen")

        assertTrue(DesktopMainViewHelpers.canReuseDiscoverySession(active, requested))
        assertFalse(
            DesktopMainViewHelpers.canReuseDiscoverySession(
                active,
                PeerDiscoveryConfig.defaults("local-peer", "omen", 5555, 5556),
            ),
        )
        assertFalse(
            DesktopMainViewHelpers.canReuseDiscoverySession(
                active,
                PeerDiscoveryConfig.defaults("local-peer", "omen", 5555, 5556, false),
            ),
        )
    }

    @Test
    fun shouldFormatDiscoveryBroadcastMessage() {
        val config = PeerDiscoveryConfig.defaults("peer-1", "Alice", 5555, 5556, true)

        assertEquals(
            "[discovery] broadcasting as Alice on UDP ${config.discoveryPort}",
            DesktopMainViewHelpers.discoveryStartedMessage(config),
        )
    }

    @Test
    fun shouldFormatHiddenDiscoveryMessage() {
        val config = PeerDiscoveryConfig.defaults("peer-1", "Alice", 5555, 5556, false)

        assertEquals(
            "[discovery] room is hidden; listening on UDP ${config.discoveryPort} without broadcasting",
            DesktopMainViewHelpers.discoveryStartedMessage(config),
        )
    }

    @Test
    fun shouldFormatDiscoveryListenOnlyMessage() {
        assertEquals("[discovery] listening on UDP 54545", DesktopMainViewHelpers.discoveryListeningMessage(54545))
    }

    @Test
    fun shouldFormatDiscoveryErrorAndVisibilityMessages() {
        assertEquals(
            "[discovery-error] socket failed -> bind failed",
            DesktopMainViewHelpers.discoveryErrorDiagnostics("socket failed", IllegalStateException("bind failed")),
        )
        assertEquals(
            "[discovery-error] socket failed",
            DesktopMainViewHelpers.discoveryErrorDiagnostics("socket failed", null),
        )
        assertEquals("[discovery] socket failed", DesktopMainViewHelpers.discoveryChatMessage("socket failed"))
        assertEquals(
            "Looking for nearby rooms. Select a discovered room and connect before sending files or starting a call.",
            DesktopMainViewHelpers.discoverySearchHint(),
        )
        assertEquals("[discovery] room is now discoverable", DesktopMainViewHelpers.discoveryVisibilityMessage(true))
        assertEquals(
            "[discovery] room is now hidden from automatic discovery",
            DesktopMainViewHelpers.discoveryVisibilityMessage(false),
        )
    }

    @Test
    fun shouldFormatDiscoveryPeerDiagnostics() {
        val peer = DiscoveredPeer("peer-1", "Alice", "192.168.1.20", 5555, 5556, Instant.EPOCH)

        assertEquals(
            "[discovery] Alice at 192.168.1.20:5555",
            DesktopMainViewHelpers.discoveryPeerFoundDiagnostics(peer),
        )
        assertEquals(
            "[discovery] expired Alice at 192.168.1.20",
            DesktopMainViewHelpers.discoveryPeerExpiredDiagnostics(peer),
        )
    }

    @Test
    fun shouldFormatLocalNetworkInfoMessages() {
        assertEquals(
            "local network IP is unavailable right now",
            DesktopMainViewHelpers.localNetworkInfoMessage(emptyList()),
        )
        assertEquals(
            "local network IP: 192.168.1.20",
            DesktopMainViewHelpers.localNetworkInfoMessage(listOf("192.168.1.20")),
        )
        assertEquals(
            "local network IPs: 10.0.0.5, 192.168.1.20",
            DesktopMainViewHelpers.localNetworkInfoMessage(listOf("10.0.0.5", "192.168.1.20")),
        )
    }

    @Test
    fun shouldFormatLocalNetworkInfoErrorMessage() {
        assertEquals(
            "failed to determine local network IP: permission denied",
            DesktopMainViewHelpers.localNetworkInfoErrorMessage("permission denied"),
        )
    }

    @Test
    fun shouldDetectHangUpAvailabilityFromRtcState() {
        assertFalse(DesktopMainViewHelpers.hangUpAvailable(null))
        assertFalse(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.IDLE))
        assertTrue(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.NEGOTIATING))
        assertTrue(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.CONNECTING))
        assertTrue(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.CONNECTED))
        assertTrue(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.CLOSING))
        assertFalse(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.CLOSED))
        assertFalse(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.FAILED))
        assertFalse(DesktopMainViewHelpers.hangUpAvailable(RtcSessionState.UNAVAILABLE))
    }

    @Test
    fun shouldDetectSelectedPeerFileCapability() {
        assertFalse(DesktopMainViewHelpers.selectedPeerFileCapable(null))
        assertFalse(DesktopMainViewHelpers.selectedPeerFileCapable(chatOnlyPeer()))
        assertFalse(DesktopMainViewHelpers.selectedPeerFileCapable(discoveredPeer(online = false)))
        assertTrue(DesktopMainViewHelpers.selectedPeerFileCapable(discoveredPeer()))
        assertTrue(DesktopMainViewHelpers.selectedPeerFileCapable(inferredFilePeer()))
        assertTrue(DesktopMainViewHelpers.selectedPeerFileCapable(desktopPeerWithLegacyFilePresence()))
    }

    @Test
    fun shouldDetectSelectedPeerCallableState() {
        assertFalse(DesktopMainViewHelpers.selectedPeerCallable(null))
        assertFalse(DesktopMainViewHelpers.selectedPeerCallable(discoveredPeer(online = false)))
        assertTrue(DesktopMainViewHelpers.selectedPeerCallable(chatOnlyPeer()))
        assertTrue(DesktopMainViewHelpers.selectedPeerCallable(discoveredPeer()))
    }

    private fun peer(peerId: String?): PeerPresence = PeerPresence(
        "Alice",
        true,
        peerId,
        "192.168.1.20",
        5555,
        5556,
        Instant.parse("2026-05-22T09:00:00Z"),
    )

    private fun chatOnlyPeer(): PeerPresence = PeerPresence(
        "Alice",
        true,
        null,
        null,
        0,
        0,
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

    private fun desktopPeerWithLegacyFilePresence(): PeerPresence = PeerPresence(
        "Desktop Peer",
        true,
        null,
        "192.168.1.40",
        NetworkConstants.DEFAULT_CHAT_PORT,
        NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
        Instant.parse("2026-05-22T09:00:00Z"),
        PeerCapabilities.desktop("0.5.0", NetworkConstants.DEFAULT_FILE_TRANSFER_PORT).withFileReceiver(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT, enabled = false),
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
}
