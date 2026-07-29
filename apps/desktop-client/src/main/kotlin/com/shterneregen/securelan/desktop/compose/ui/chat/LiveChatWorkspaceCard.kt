package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalReducedMotion
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatWorkspaceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolKind
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolsState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyMode
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.components.SubtleContentSurface
import com.shterneregen.securelan.desktop.compose.util.openComposeFileChooser
import com.shterneregen.securelan.desktop.compose.util.resolveAttachCandidatePeer

@Composable
internal fun LiveChatWorkspaceCard(
    hostAdapter: ComposeDesktopHostAdapter,
    peerState: ComposePeerListState,
    onOpenQuickShare: () -> Unit,
    onOpenSteganography: (ComposeSteganographyMode) -> Unit,
    videoStageContent: @Composable () -> Unit,
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
        val peer = selectedFilePeer ?: return
        val path = openComposeFileChooser("Choose file to send to ${peer.nickname}") ?: return
        hostAdapter.sendFileToPeer(path, hostAdapter.statusState.nickname, peer, hostAdapter.currentRoomPassword)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val videoStageVisible = hostAdapter.experimentalVideoState.currentSession != null
        AnimatedVisibility(
            visible = videoStageVisible,
            enter = fadeIn(motionTween()) + expandVertically(motionTween(), expandFrom = Alignment.Top),
            exit = shrinkVertically(motionTween(), shrinkTowards = Alignment.Top) + fadeOut(motionTween()),
        ) {
            videoStageContent()
        }
        SubtleContentSurface(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val transferState = ComposeFileTransferState(
                statusState = hostAdapter.statusState,
                peerListState = peerState,
                entries = hostAdapter.transferEntries,
                incomingPrompts = hostAdapter.incomingTransferPrompts,
                autoAcceptFiles = hostAdapter.autoAcceptIncomingFiles,
            )
            val hasTranscript = transcript.isNotEmpty() || transferState.chatAttachmentCards.isNotEmpty()
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
                        modifier = Modifier.fillMaxSize().padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    ) {
                        items(transcript.size) { index ->
                            ChatTranscriptLine(transcript[index], hostAdapter.statusState.nickname)
                        }
                        items(transferState.chatAttachmentCards.size) { index ->
                            ChatAttachmentCardRow(transferState.chatAttachmentCards[index])
                        }
                    }
                }
            }
        }
        var attachMenuExpanded by remember { mutableStateOf(false) }
        var attachmentStatusText by remember(attachmentTools.disabledStatusText) { mutableStateOf(attachmentTools.disabledStatusText) }
        val attachButtonFocusRequester = remember { FocusRequester() }
        fun dismissAttachMenu(restoreComposerFocus: Boolean) {
            attachMenuExpanded = false
            if (restoreComposerFocus) {
                chatInputFocusRequester.requestFocus()
            } else {
                attachButtonFocusRequester.requestFocus()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = ComposeShellMetadata.COMPOSER_MIN_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachmentComposerMenu(
                tools = attachmentTools,
                expanded = attachMenuExpanded,
                statusText = attachmentStatusText,
                onExpandedChange = { expanded ->
                    attachMenuExpanded = expanded
                    if (expanded) {
                        attachmentStatusText = attachmentTools.summary
                    }
                },
                onStatusTextChange = { attachmentStatusText = it },
                onDismiss = { dismissAttachMenu(restoreComposerFocus = true) },
                onItemSelected = { item ->
                    when (item.kind) {
                        ComposeAttachmentToolKind.SECURE_FILE -> {
                            dismissAttachMenu(restoreComposerFocus = true)
                            attachSelectedFile()
                        }
                        ComposeAttachmentToolKind.QUICK_SHARE -> {
                            dismissAttachMenu(restoreComposerFocus = false)
                            onOpenQuickShare()
                        }
                        ComposeAttachmentToolKind.STEGANOGRAPHY -> {
                            dismissAttachMenu(restoreComposerFocus = false)
                            onOpenSteganography(ComposeSteganographyMode.HIDE)
                        }
                        ComposeAttachmentToolKind.ENCRYPTED_TEXT_OR_FILE -> {
                            attachmentStatusText = item.statusText
                        }
                    }
                },
                attachButtonFocusRequester = attachButtonFocusRequester,
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
