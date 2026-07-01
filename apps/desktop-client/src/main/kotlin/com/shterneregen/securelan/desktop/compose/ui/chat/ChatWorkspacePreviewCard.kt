package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.LocalReducedMotion
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatWorkspaceState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.components.SubtleContentSurface

@Composable
internal fun ChatWorkspacePreviewCard(initialState: ComposeChatWorkspaceState) {
    var draftMessage by remember { mutableStateOf(initialState.draftMessage) }
    val previewState = initialState.copy(draftMessage = draftMessage)
    val listState = rememberLazyListState()

    LaunchedEffect(previewState.transcriptLines.size) {
        if (previewState.transcriptLines.isNotEmpty()) {
            listState.animateScrollToItem(previewState.transcriptLines.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val reduced = LocalReducedMotion.current
        SubtleContentSurface(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AnimatedContent(
                targetState = previewState.transcriptLines.isNotEmpty(),
                transitionSpec = {
                    fadeIn(motionTween(reduced)) togetherWith fadeOut(motionTween(reduced))
                },
                label = "ChatTranscriptContentPreview",
            ) { hasContent ->
                if (!hasContent) {
                    ChatTranscriptEmptyState(previewState, connected = false)
                } else {
                    val tokens = LocalSecureLanDesignTokens.current
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    ) {
                        items(previewState.messages.size) { index ->
                            ChatTranscriptLine(previewState.messages[index])
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = ComposeShellMetadata.COMPOSER_MIN_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactButton(onClick = {}, enabled = false) { Text("Attach") }
            CompactTextField(
                draftMessage,
                { draftMessage = it },
                label = "",
                modifier = Modifier.weight(1f),
                placeholder = "Type a message for the shared chat...",
            )
            CompactButton(onClick = {}, enabled = false) { Text(previewState.sendLabel) }
        }
        Text(
            text = "${previewState.transcriptSummary} · ${previewState.readinessSummary}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
    }
}
