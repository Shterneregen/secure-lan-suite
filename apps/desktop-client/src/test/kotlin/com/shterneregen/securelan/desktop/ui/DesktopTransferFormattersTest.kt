package com.shterneregen.securelan.desktop.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DesktopTransferFormattersTest {
    @Test
    fun shouldFormatMegabytesWithTwoDecimalPlaces() {
        assertEquals("1.50 MB", DesktopTransferFormatters.formatMegabytes(1_572_864))
    }

    @Test
    fun shouldFormatMegabyteTransferSpeed() {
        assertEquals("1.5 MB/s avg", DesktopTransferFormatters.formatTransferSpeed(1_572_864.0))
    }

    @Test
    fun shouldFormatKilobyteTransferSpeed() {
        assertEquals("512 KB/s avg", DesktopTransferFormatters.formatTransferSpeed(524_288.0))
    }

    @Test
    fun shouldFormatByteTransferSpeed() {
        assertEquals("512 B/s avg", DesktopTransferFormatters.formatTransferSpeed(512.0))
    }

    @Test
    fun shouldFormatEmptyTransferHint() {
        assertEquals("No transfers yet", DesktopTransferFormatters.formatTransferHint(0, hasEntries = false))
    }

    @Test
    fun shouldFormatIdleTransferHintWhenHistoryExists() {
        assertEquals(
            "No active transfers. Recent results remain visible below.",
            DesktopTransferFormatters.formatTransferHint(0, hasEntries = true),
        )
    }

    @Test
    fun shouldFormatSingularActiveTransferHint() {
        assertEquals("1 active transfer", DesktopTransferFormatters.formatTransferHint(1, hasEntries = true))
    }

    @Test
    fun shouldFormatPluralActiveTransferHint() {
        assertEquals("2 active transfers", DesktopTransferFormatters.formatTransferHint(2, hasEntries = true))
    }

    @Test
    fun shouldFormatIdleActiveTransferSummary() {
        assertEquals("Transfers idle", DesktopTransferFormatters.formatActiveTransferSummary(0))
    }

    @Test
    fun shouldFormatSingularActiveTransferSummary() {
        assertEquals("1 transfer active", DesktopTransferFormatters.formatActiveTransferSummary(1))
    }

    @Test
    fun shouldFormatPluralActiveTransferSummary() {
        assertEquals("2 transfers active", DesktopTransferFormatters.formatActiveTransferSummary(2))
    }

    @Test
    fun shouldFormatIncomingFileDecisionMessages() {
        assertEquals(
            "[file-recv] rejected report.pdf from Alice: chat is not connected",
            DesktopTransferFormatters.fileRejectedDisconnectedMessage("report.pdf", "Alice"),
        )
        assertEquals(
            "[file-recv] rejected report.pdf from unknown/offline peer Alice",
            DesktopTransferFormatters.fileRejectedUnknownPeerMessage("report.pdf", "Alice"),
        )
        assertEquals(
            "[file-recv] auto-accepted report.pdf from Alice",
            DesktopTransferFormatters.fileAutoAcceptedMessage("report.pdf", "Alice"),
        )
        assertEquals(
            "[file-recv] confirmation failed: dialog closed",
            DesktopTransferFormatters.fileConfirmationFailedDiagnostics("dialog closed"),
        )
        assertEquals(
            "[file-recv] accepted report.pdf from Alice",
            DesktopTransferFormatters.fileConfirmationResultMessage(true, "report.pdf", "Alice"),
        )
        assertEquals(
            "[file-recv] rejected report.pdf from Alice",
            DesktopTransferFormatters.fileConfirmationResultMessage(false, "report.pdf", "Alice"),
        )
    }

    @Test
    fun shouldFormatIncomingFileDialogCopy() {
        assertEquals("Incoming file", DesktopTransferFormatters.incomingFileTitle())
        assertEquals("Accept file from Alice?", DesktopTransferFormatters.incomingFileHeader("Alice"))
        assertEquals(
            "File: report.pdf" + System.lineSeparator() +
                "Size: 1.50 MB" + System.lineSeparator() +
                "Remote: /192.168.1.20:54545",
            DesktopTransferFormatters.incomingFileContent("report.pdf", 1_572_864, "/192.168.1.20:54545"),
        )
    }

    @Test
    fun shouldFormatActiveOutgoingTransferListMetaWithProgressSizeAndSpeed() {
        val entry = TransferEntry("transfer-1", "report.pdf", true, "Sending", 42, 2_097_152)
        entry.speedBytesPerSecond = 1_572_864.0

        assertEquals(
            "↑ Sent — Sending — 42% — 2.00 MB — 1.5 MB/s avg",
            DesktopTransferFormatters.formatTransferListMeta(entry),
        )
    }

    @Test
    fun shouldFormatCompletedIncomingTransferListMetaWithHundredPercentAndSize() {
        val entry = TransferEntry("transfer-2", "archive.zip", false, "Completed", 100, 1_048_576)

        assertEquals(
            "↓ Received — Completed — 100% — 1.00 MB",
            DesktopTransferFormatters.formatTransferListMeta(entry),
        )
    }

    @Test
    fun shouldFormatPendingTransferListMetaWithoutOptionalParts() {
        val entry = TransferEntry("transfer-3", "notes.txt", true, "Queued", 0, 0)

        assertEquals("↑ Sent — Queued", DesktopTransferFormatters.formatTransferListMeta(entry))
    }
}
