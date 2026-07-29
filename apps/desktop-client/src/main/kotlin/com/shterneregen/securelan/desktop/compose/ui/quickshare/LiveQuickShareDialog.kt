package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import java.awt.Dimension

@Composable
internal fun LiveQuickShareDialog(
    hostAdapter: ComposeDesktopHostAdapter,
    onClose: () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(760.dp, 780.dp)),
        title = "SecureLanSuite · Quick Share",
        resizable = true,
        onPreviewKeyEvent = { event ->
            if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                onClose()
                true
            } else {
                false
            }
        },
    ) {
        LaunchedEffect(window) {
            window.minimumSize = Dimension(620, 620)
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            QuickShareWindowContent(hostAdapter = hostAdapter, onClose = onClose)
        }
    }
}

@Composable
private fun QuickShareWindowContent(
    hostAdapter: ComposeDesktopHostAdapter,
    onClose: () -> Unit,
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
                imageVector = SecureLanIcons.QuickShare,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colors.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Quick Share", style = MaterialTheme.typography.h5)
                Text(
                    "Create temporary browser links for people on your trusted LAN.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            CompactButton(onClick = onClose, tone = CompactButtonTone.TERTIARY) {
                Icon(
                    imageVector = SecureLanIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Close")
            }
        }
        Divider(color = tokens.colors.borderSubtle)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(end = tokens.spacing.xs, bottom = tokens.spacing.md),
        ) {
            LiveQuickShareCard(hostAdapter)
        }
    }
}
