package com.shterneregen.securelan.desktop.compose.ui.peerlist

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata

@Composable
internal fun PeerListEmptyState(peerState: ComposePeerListState) {
    val tokens = LocalSecureLanDesignTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(tokens.spacing.sm),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier.widthIn(max = ComposeShellMetadata.SIDE_EMPTY_STATE_GUIDANCE_MAX_WIDTH),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = peerState.emptyStateSituation,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.76f),
            )
            Text(
                text = peerState.emptyStateExplanation,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f),
            )
        }
    }
}
