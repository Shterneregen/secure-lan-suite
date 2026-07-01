package com.shterneregen.securelan.desktop.compose.state.quickshare

import com.shterneregen.securelan.desktop.ui.DesktopQuickShareFormatters
import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import com.shterneregen.securelan.desktop.ui.QuickShareEntry
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType

public data class ComposeQuickShareRow(
    val id: String,
    val title: String,
    val typeLabel: String,
    val statusLabel: String,
    val detail: String,
    val url: String,
    val active: Boolean,
) {
    companion object {
        fun from(entry: QuickShareEntry): ComposeQuickShareRow {
            val snapshot = entry.snapshot()
            val typeLabel = when (snapshot.type()) {
                QuickShareType.FILE -> "File link"
                QuickShareType.TEXT -> "Text link"
            }
            val statusLabel = when (snapshot.status()) {
                QuickShareStatus.ACTIVE -> "Active"
                QuickShareStatus.STOPPED -> "Stopped"
                QuickShareStatus.EXPIRED -> "Expired"
                QuickShareStatus.LIMIT_REACHED -> "Limit reached"
            }
            val title = snapshot.displayName().ifBlank { snapshot.fileName() }.ifBlank { snapshot.id() }
            val sizeLabel = snapshot.fileSize().takeIf { it > 0 }?.let(DesktopTransferFormatters::formatMegabytes)
            val detail = buildList {
                add(snapshot.accessLimit()?.let { "${snapshot.accessCount()}/$it opens" } ?: "${snapshot.accessCount()} opens · unlimited")
                add(snapshot.expiresAt()?.let { "expires $it" } ?: "until stopped")
                if (sizeLabel != null) add(sizeLabel)
            }.joinToString(" · ")
            return ComposeQuickShareRow(
                id = entry.id(),
                title = title,
                typeLabel = typeLabel,
                statusLabel = statusLabel,
                detail = detail,
                url = DesktopQuickShareFormatters.preferQuickShareUrl(entry.snapshot()),
                active = entry.active(),
            )
        }
    }
}
