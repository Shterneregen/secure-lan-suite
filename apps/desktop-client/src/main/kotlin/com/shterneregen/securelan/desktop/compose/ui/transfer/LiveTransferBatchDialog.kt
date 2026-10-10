package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferDraft
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferItem
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferPhase
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.util.openComposeTransferFileChooser
import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import java.awt.Dimension

/** Compact pre-send review for a file batch. Closing with Escape keeps the draft. */
@Composable
internal fun LiveTransferBatchDialog(
    hostAdapter: ComposeDesktopHostAdapter,
    onClose: () -> Unit,
) {
    val draft = hostAdapter.transferDraft ?: return
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(580.dp, 640.dp)),
        title = "Files for ${draft.target.nickname}",
        resizable = true,
        onPreviewKeyEvent = { event ->
            when {
                event.key == Key.Escape && event.type == KeyEventType.KeyUp -> {
                    onClose()
                    true
                }

                event.key == Key.Enter && event.type == KeyEventType.KeyUp -> {
                    if (draft.items.isNotEmpty()) {
                        hostAdapter.startTransferDraft()
                        true
                    } else {
                        false
                    }
                }

                else -> false
            }
        },
    ) {
        LaunchedEffect(window) {
            window.minimumSize = Dimension(460, 420)
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            TransferBatchReviewContent(hostAdapter, draft)
        }
    }
}

@Composable
private fun TransferBatchReviewContent(
    hostAdapter: ComposeDesktopHostAdapter,
    draft: DesktopTransferDraft,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(
        modifier = Modifier.fillMaxSize().padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SecureLanIcons.File,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colors.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Files for ${draft.target.nickname}", style = MaterialTheme.typography.h6)
                Text(
                    draftSummary(draft),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(tokens.radius.medium),
            border = BorderStroke(1.dp, tokens.colors.borderSubtle),
            color = tokens.colors.surfaceLevel1,
        ) {
            if (draft.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No files selected yet. Add files to send to ${draft.target.nickname}.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(tokens.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
                ) {
                    items(draft.items, key = { it.id }) { item ->
                        TransferDraftRow(item, onRemove = { hostAdapter.removeTransferDraftItem(item.id) })
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactButton(
                onClick = {
                    val paths = openComposeTransferFileChooser("Add files for ${draft.target.nickname}")
                    if (paths.isNotEmpty()) hostAdapter.addTransferFiles(draft.target, paths)
                },
                tone = CompactButtonTone.TERTIARY,
            ) { Text("Add files") }
            CompactButton(
                onClick = { hostAdapter.clearTransferDraft() },
                enabled = draft.items.isNotEmpty(),
                tone = CompactButtonTone.DESTRUCTIVE,
            ) { Text("Clear") }
            Spacer(Modifier.weight(1f))
            CompactButton(
                onClick = { hostAdapter.startTransferDraft() },
                enabled = draft.items.isNotEmpty(),
            ) { Text("Send ${draft.items.size} ${if (draft.items.size == 1) "file" else "files"}") }
        }
    }
}

@Composable
private fun TransferDraftRow(item: DesktopTransferItem, onRemove: () -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.small),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SecureLanIcons.File,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.phase.label()} · ${formatSize(item.size)}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = SecureLanIcons.Close,
                    contentDescription = "Remove ${item.name}",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun draftSummary(draft: DesktopTransferDraft): String {
    val count = draft.items.size
    val files = "$count ${if (count == 1) "file" else "files"}"
    val bytes = draft.knownBytes
    return if (bytes == null) "$files · size pending" else "$files · ${DesktopTransferFormatters.formatMegabytes(bytes)}"
}

private fun formatSize(size: Long?): String =
    if (size == null) "size unavailable" else DesktopTransferFormatters.formatMegabytes(size)

internal fun DesktopTransferPhase.label(): String = when (this) {
    DesktopTransferPhase.QUEUED -> "Queued"
    DesktopTransferPhase.PREPARING -> "Preparing"
    DesktopTransferPhase.WAITING_FOR_ACCEPTANCE -> "Waiting for acceptance"
    DesktopTransferPhase.SENDING -> "Sending"
    DesktopTransferPhase.WAITING_FOR_CONFIRMATION -> "Waiting for confirmation"
    DesktopTransferPhase.COMPLETED -> "Confirmed"
    DesktopTransferPhase.FAILED -> "Failed"
    DesktopTransferPhase.OUTCOME_UNKNOWN -> "Confirmation not received"
    DesktopTransferPhase.SKIPPED -> "Skipped"
}
