package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState

@Composable
internal fun QuickShareLinksPanel(
    state: ComposeQuickShareState,
    onCopy: (String) -> Unit,
    onOpen: (String) -> Unit,
    onStop: (String) -> Unit,
) {
    if (!state.running && state.shareRowsDetailed.isEmpty()) {
        return
    }

    QuickShareSection(
        title = "Links",
        subtitle = "${state.activeShareCountLabel} · ${state.inactiveShareCountLabel}",
    ) {
        if (state.shareRowsDetailed.isEmpty()) {
            Text(
                text = "Browser links you create will appear here.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
            )
        } else {
            state.shareRowsDetailed.forEach { row ->
                QuickShareLinkRow(row = row, onCopy = onCopy, onOpen = onOpen, onStop = onStop)
            }
        }
    }
}
