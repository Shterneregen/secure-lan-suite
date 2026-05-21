package com.shterneregen.securelan.desktop.ui

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

    private fun peer(peerId: String?): PeerPresence = PeerPresence(
        "Alice",
        true,
        peerId,
        "192.168.1.20",
        5555,
        5556,
        Instant.parse("2026-05-22T09:00:00Z"),
    )
}
