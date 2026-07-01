package com.shterneregen.securelan.desktop.compose.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
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
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.ui.media.AudioVideoDevicesPreviewCard
import com.shterneregen.securelan.desktop.compose.ui.media.LiveAudioVideoDevicesCard
import java.awt.Dimension

@Composable
internal fun LiveSettingsDialog(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    onClose: () -> Unit,
) {
    SettingsDialog(onClose = onClose) {
        LiveAudioVideoDevicesCard(hostAdapter = hostAdapter, peerState = peerState)
    }
}

@Composable
internal fun PreviewSettingsDialog(onClose: () -> Unit) {
    SettingsDialog(onClose = onClose) {
        AudioVideoDevicesPreviewCard()
    }
}

@Composable
private fun SettingsDialog(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(980.dp, 780.dp)),
        title = "SecureLanSuite · Settings",
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
            window.minimumSize = Dimension(820, 640)
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            SettingsWindowContent(onClose = onClose, content = content)
        }
    }
}

@Composable
private fun SettingsWindowContent(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
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
                imageVector = SecureLanIcons.Settings,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colors.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings", style = MaterialTheme.typography.h5)
                Text(
                    "Configure SecureLanSuite for calls and everyday use.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
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
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            SettingsNavigation(modifier = Modifier.width(190.dp).fillMaxHeight())
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(end = tokens.spacing.xs, bottom = tokens.spacing.md),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsNavigation(modifier: Modifier = Modifier) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radius.large),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        color = tokens.colors.surfaceLevel1,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(tokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Text(
                "SETTINGS",
                modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
                style = MaterialTheme.typography.overline,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(tokens.radius.medium),
                color = MaterialTheme.colors.primary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.32f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = SecureLanIcons.Devices,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colors.primary,
                    )
                    Text(
                        "Audio & video",
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.primary,
                    )
                }
            }
        }
    }
}
