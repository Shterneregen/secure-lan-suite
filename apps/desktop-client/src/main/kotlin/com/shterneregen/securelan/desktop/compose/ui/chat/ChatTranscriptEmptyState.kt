package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatWorkspaceState

@Composable
internal fun ChatTranscriptEmptyState(chatState: ComposeChatWorkspaceState) {
    val tokens = LocalSecureLanDesignTokens.current
    Box(modifier = Modifier.fillMaxSize().padding(tokens.spacing.md), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = ComposeShellMetadata.CENTER_EMPTY_STATE_GUIDANCE_MAX_WIDTH),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chatState.transcriptEmptyTitle,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
            )
            Text(
                text = chatState.transcriptEmptyExplanation,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
            )
            Surface(
                shape = RoundedCornerShape(tokens.radius.small),
                border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                color = tokens.colors.surfaceLevel2,
            ) {
                Text(
                    text = chatState.transcriptEmptyActionLabel,
                    modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                )
            }
        }
    }
}
