package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalReducedMotion
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeCallWorkspaceFocusMode
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatMessage
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatWorkspaceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolsState
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopBatchPhase
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferBatch
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferPhase
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.components.SubtleContentSurface
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.util.openComposeTransferFileChooser
import com.shterneregen.securelan.desktop.compose.util.resolveAttachCandidatePeer
import java.awt.Cursor

@Composable
internal fun LiveChatWorkspaceCard(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    videoStageContent: @Composable (Modifier) -> Unit,
) {
    var draftMessage by remember { mutableStateOf("") }
    val transcript = hostAdapter.chatMessages.filterIndexed { index, message ->
        index == 0 || message.displayText != hostAdapter.chatMessages[index - 1].displayText
    }
    val selectedPeer = peerState.selectedPeer
    val selectedFilePeer = resolveAttachCandidatePeer(selectedPeer, hostAdapter::discoveredPeerFor)
    val attachmentTools = ComposeAttachmentToolsState(
        peerSelected = selectedPeer != null,
        fileTargetReady = selectedFilePeer != null,
    )
    val chatState = ComposeChatWorkspaceState(
        statusState = hostAdapter.statusState,
        peerListState = peerState,
        draftMessage = draftMessage,
    )
    val listState = rememberLazyListState()
    val chatInputFocusRequester = remember { FocusRequester() }
    val videoStageVisible = hostAdapter.experimentalVideoState.currentSession != null
    var focusMode by remember { mutableStateOf(ComposeCallWorkspaceFocusMode.SPLIT) }
    var videoFraction by remember { mutableStateOf(0.48f) }
    var splitAreaHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(videoStageVisible) {
        if (!videoStageVisible) {
            focusMode = ComposeCallWorkspaceFocusMode.SPLIT
        }
    }

    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) {
            listState.animateScrollToItem(transcript.lastIndex)
        }
    }

    fun sendDraftMessage() {
        if (draftMessage.isNotBlank() && hostAdapter.chatConnected) {
            hostAdapter.sendMessage(draftMessage.trim())
            draftMessage = ""
            chatInputFocusRequester.requestFocus()
        }
    }

    fun attachSelectedFile() {
        // Reopening restores an existing draft instead of discarding it.
        if (hostAdapter.transferDraft != null) {
            hostAdapter.openTransferReview()
            return
        }
        val peer = selectedFilePeer ?: return
        val target = hostAdapter.captureTransferTarget(peer) ?: return
        val paths = openComposeTransferFileChooser("Choose files to send to ${peer.nickname}")
        if (paths.isNotEmpty()) {
            hostAdapter.addTransferFiles(target, paths)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (videoStageVisible) {
            CallWorkspaceFocusBar(
                focusMode = focusMode,
                onFocusModeChange = { focusMode = it },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { splitAreaHeightPx = it.height },
            ) {
                when (focusMode) {
                    ComposeCallWorkspaceFocusMode.VIDEO ->
                        videoStageContent(Modifier.fillMaxSize())

                    ComposeCallWorkspaceFocusMode.CHAT ->
                        LiveTranscriptSurface(
                            transcript = transcript,
                            chatState = chatState,
                            localNickname = hostAdapter.statusState.nickname,
                            listState = listState,
                            modifier = Modifier.fillMaxSize(),
                        )

                    ComposeCallWorkspaceFocusMode.SPLIT -> Column(Modifier.fillMaxSize()) {
                        videoStageContent(
                            Modifier
                                .fillMaxWidth()
                                .weight(videoFraction),
                        )
                        VideoChatResizeDivider(
                            onDrag = { deltaY ->
                                if (splitAreaHeightPx > 0) {
                                    val minVideoFraction =
                                        with(density) { 180.dp.toPx() / splitAreaHeightPx }.coerceAtMost(0.42f)
                                    val maxVideoFraction =
                                        (1f - with(density) { 140.dp.toPx() / splitAreaHeightPx }).coerceAtLeast(0.58f)
                                    videoFraction = (videoFraction + deltaY / splitAreaHeightPx)
                                        .coerceIn(minVideoFraction, maxVideoFraction)
                                }
                            },
                        )
                        LiveTranscriptSurface(
                            transcript = transcript,
                            chatState = chatState,
                            localNickname = hostAdapter.statusState.nickname,
                            listState = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f - videoFraction),
                        )
                    }
                }
            }
        } else {
            LiveTranscriptSurface(
                transcript = transcript,
                chatState = chatState,
                localNickname = hostAdapter.statusState.nickname,
                listState = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        if (!videoStageVisible || focusMode.showsChat) {
            hostAdapter.transferBatch?.let { batch ->
                DesktopTransferBatchSummary(hostAdapter, batch)
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = ComposeShellMetadata.COMPOSER_MIN_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AttachmentComposerButton(
                    tools = attachmentTools,
                    onClick = ::attachSelectedFile,
                )
                CompactTextField(
                    draftMessage,
                    { draftMessage = it },
                    label = "",
                    modifier = Modifier.weight(1f).then(Modifier.focusRequester(chatInputFocusRequester)),
                    onSubmit = ::sendDraftMessage,
                    placeholder = "Type a message for the shared chat...",
                )
                CompactButton(
                    onClick = ::sendDraftMessage,
                    enabled = draftMessage.isNotBlank() && hostAdapter.chatConnected,
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun CallWorkspaceFocusBar(
    focusMode: ComposeCallWorkspaceFocusMode,
    onFocusModeChange: (ComposeCallWorkspaceFocusMode) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.64f),
        shape = RoundedCornerShape(tokens.radius.medium),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Call layout",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.caption,
                color = tokens.colors.textSecondary,
            )
            CallWorkspaceModeButton(
                label = "Split view",
                selected = focusMode == ComposeCallWorkspaceFocusMode.SPLIT,
                onClick = { onFocusModeChange(ComposeCallWorkspaceFocusMode.SPLIT) },
            )
            CallWorkspaceModeButton(
                label = "Focus video",
                selected = focusMode == ComposeCallWorkspaceFocusMode.VIDEO,
                onClick = { onFocusModeChange(ComposeCallWorkspaceFocusMode.VIDEO) },
            )
            CallWorkspaceModeButton(
                label = "Focus chat",
                selected = focusMode == ComposeCallWorkspaceFocusMode.CHAT,
                onClick = { onFocusModeChange(ComposeCallWorkspaceFocusMode.CHAT) },
            )
        }
    }
}

@Composable
private fun CallWorkspaceModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    CompactButton(
        onClick = onClick,
        tone = if (selected) CompactButtonTone.SECONDARY else CompactButtonTone.TERTIARY,
    ) {
        Text(label)
    }
}

@Composable
private fun VideoChatResizeDivider(onDrag: (Float) -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
            .semantics { contentDescription = "Resize video and chat" }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(52.dp).height(3.dp),
            color = tokens.colors.borderSubtle.copy(alpha = 0.86f),
            shape = RoundedCornerShape(tokens.radius.pill),
        ) {}
    }
}

@Composable
private fun LiveTranscriptSurface(
    transcript: List<ComposeChatMessage>,
    chatState: ComposeChatWorkspaceState,
    localNickname: String,
    listState: LazyListState,
    modifier: Modifier,
) {
    SubtleContentSurface(modifier = modifier) {
        val hasTranscript = transcript.isNotEmpty()
        val reduced = LocalReducedMotion.current
        AnimatedContent(
            targetState = hasTranscript,
            transitionSpec = {
                fadeIn(motionTween(reduced)) togetherWith fadeOut(motionTween(reduced))
            },
            label = "ChatTranscriptContent",
        ) { hasContent ->
            if (!hasContent) {
                ChatTranscriptEmptyState(chatState)
            } else {
                val tokens = LocalSecureLanDesignTokens.current
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                ) {
                    items(transcript.size) { index ->
                        ChatTranscriptLine(transcript[index], localNickname)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopTransferBatchSummary(
    hostAdapter: ComposeDesktopHostAdapter,
    batch: DesktopTransferBatch,
) {
    // Finished results live in the per-file transcript events and Transfers, not in a persistent panel.
    if (!batch.unfinished) return
    val tokens = LocalSecureLanDesignTokens.current
    val title = "Sending to ${batch.target.nickname}: ${batch.completed} of ${batch.items.size} confirmed"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.6f),
        shape = RoundedCornerShape(tokens.radius.medium),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(title, style = MaterialTheme.typography.subtitle2)
                    Text(
                        text = batch.summary,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                    )
                }
                when (batch.phase) {
                    DesktopBatchPhase.RUNNING -> CompactButton(
                        onClick = { hostAdapter.stopTransferBatch() },
                        tone = CompactButtonTone.TERTIARY,
                    ) { Text("Stop after current") }

                    DesktopBatchPhase.STOP_REQUESTED -> Text(
                        "Stopping after current file…",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )

                    DesktopBatchPhase.PAUSED -> {
                        CompactButton(onClick = { hostAdapter.retryTransferBatch() }) { Text("Retry") }
                        CompactButton(
                            onClick = { hostAdapter.skipFailedTransferAndContinue() },
                            tone = CompactButtonTone.TERTIARY,
                        ) { Text("Skip") }
                        CompactButton(
                            onClick = { hostAdapter.stopTransferBatch() },
                            tone = CompactButtonTone.DESTRUCTIVE,
                        ) { Text("Finish") }
                    }

                    DesktopBatchPhase.FINISHED -> Unit
                }
            }
            val queued = batch.items.filter { it.phase == DesktopTransferPhase.QUEUED }
            if (queued.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 144.dp),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
                ) {
                    items(queued.size, key = { queued[it].id }) { index ->
                        val item = queued[index]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                        ) {
                            Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.caption)
                            CompactButton(
                                onClick = { hostAdapter.removeQueuedTransferItem(item.id) },
                                tone = CompactButtonTone.TERTIARY,
                            ) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}
