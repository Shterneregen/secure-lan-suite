package com.shterneregen.securelan.desktop.compose.state.transfer

import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import com.shterneregen.securelan.desktop.ui.TransferEntry

public data class ComposeTransferRow(
    val fileName: String,
    val directionLabel: String,
    val status: String,
    val percent: Int,
    val sizeLabel: String?,
    val speedLabel: String?,
    val active: Boolean,
    val failed: Boolean,
) {
    val completed: Boolean = status == "Completed"
    val title: String = "$directionLabel · $fileName"
    val progressLabel: String = when {
        active -> "$status · $percent%"
        completed -> "Completed · 100%"
        failed -> "Failed"
        percent > 0 -> "$status · $percent%"
        else -> status
    }
    val detail: String = listOfNotNull(sizeLabel, speedLabel).joinToString(" · ").ifBlank { "Size not reported yet" }

    companion object {
        fun from(entry: TransferEntry): ComposeTransferRow = ComposeTransferRow(
            fileName = entry.fileName,
            directionLabel = entry.directionLabel(),
            status = entry.status,
            percent = entry.percent.coerceIn(0, 100),
            sizeLabel = entry.totalBytes.takeIf { it > 0 }?.let(DesktopTransferFormatters::formatMegabytes),
            speedLabel = entry.speedBytesPerSecond.takeIf { entry.active() && it > 0 }?.let(DesktopTransferFormatters::formatTransferSpeed),
            active = entry.active(),
            failed = entry.status == "Failed",
        )
    }
}
