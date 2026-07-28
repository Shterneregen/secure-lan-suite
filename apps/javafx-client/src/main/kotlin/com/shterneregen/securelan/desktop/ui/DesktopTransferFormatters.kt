package com.shterneregen.securelan.desktop.ui

import java.util.Locale

object DesktopTransferFormatters {
    private const val KIBIBYTE = 1024.0
    private const val MEBIBYTE = KIBIBYTE * KIBIBYTE

    @JvmStatic
    fun formatMegabytes(bytes: Long): String = String.format(Locale.ROOT, "%.2f MB", bytes / MEBIBYTE)

    @JvmStatic
    fun formatTransferSpeed(bytesPerSecond: Double): String = when {
        bytesPerSecond >= MEBIBYTE -> String.format(Locale.ROOT, "%.1f MB/s avg", bytesPerSecond / MEBIBYTE)
        bytesPerSecond >= KIBIBYTE -> String.format(Locale.ROOT, "%.0f KB/s avg", bytesPerSecond / KIBIBYTE)
        else -> String.format(Locale.ROOT, "%.0f B/s avg", bytesPerSecond)
    }

    @JvmStatic
    fun formatTransferHint(activeCount: Long, hasEntries: Boolean): String = when {
        activeCount == 0L && !hasEntries -> "No transfers yet"
        activeCount == 0L -> "No active transfers. Recent results remain visible below."
        else -> "$activeCount active transfer${if (activeCount == 1L) "" else "s"}"
    }

    @JvmStatic
    fun formatActiveTransferSummary(activeCount: Long): String =
        if (activeCount == 0L) "Transfers idle" else "$activeCount transfer${if (activeCount == 1L) " active" else "s active"}"

    @JvmStatic
    fun fileRejectedDisconnectedMessage(fileName: String, senderId: String): String =
        "[file-recv] rejected $fileName from $senderId: chat is not connected"

    @JvmStatic
    fun fileRejectedUnknownPeerMessage(fileName: String, senderId: String): String =
        "[file-recv] rejected $fileName from unknown/offline peer $senderId"

    @JvmStatic
    fun fileAutoAcceptedMessage(fileName: String, senderId: String): String =
        "[file-recv] auto-accepted $fileName from $senderId"

    @JvmStatic
    fun fileConfirmationFailedDiagnostics(message: String?): String = "[file-recv] confirmation failed: $message"

    @JvmStatic
    fun incomingFileTitle(): String = "Incoming file"

    @JvmStatic
    fun incomingFileHeader(senderId: String): String = "Accept file from $senderId?"

    @JvmStatic
    fun incomingFileContent(fileName: String, fileSize: Long, remoteAddress: String): String =
        "File: $fileName" + System.lineSeparator() +
            "Size: ${formatMegabytes(fileSize)}" + System.lineSeparator() +
            "Remote: $remoteAddress"

    @JvmStatic
    fun fileConfirmationResultMessage(accepted: Boolean, fileName: String, senderId: String): String =
        "[file-recv] ${if (accepted) "accepted" else "rejected"} $fileName from $senderId"

    @JvmStatic
    fun formatTransferListMeta(entry: TransferEntry): String = buildString {
        append(entry.directionLabel())
        append(" — ")
        append(entry.status)
        if (entry.percent > 0 && entry.percent < 100 && entry.active()) {
            append(" — ")
            append(entry.percent)
            append('%')
        } else if (entry.percent == 100 && entry.status == "Completed") {
            append(" — 100%")
        }
        if (entry.totalBytes > 0) {
            append(" — ")
            append(formatMegabytes(entry.totalBytes))
        }
        if (entry.active() && entry.speedBytesPerSecond > 0) {
            append(" — ")
            append(formatTransferSpeed(entry.speedBytesPerSecond))
        }
    }
}
