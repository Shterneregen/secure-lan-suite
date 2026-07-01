package com.shterneregen.securelan.desktop.compose.state.chat

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeEmptyStateVisualWeight
import java.time.Instant

public data class ComposeChatWorkspaceState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val draftMessage: String = "Hello from Compose preview",
    val messages: List<ComposeChatMessage> = defaultPreviewMessages(),
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Shared room chat"
    val subtitle: String = peerListState.selectedPeer?.let { peer ->
        "Actions on the right will target “${peer.nickname}”. Text chat is visible to everyone in this room."
    } ?: "Connect to chat, then select a peer on the left for voice, video, and file actions."

    val transcriptLines: List<String> = messages.map(ComposeChatMessage::displayText)
    val transcriptMessageTimes: List<String> = messages.map(ComposeChatMessage::displayTime)
    val transcriptSummary: String =
        if (messages.isEmpty()) "No chat messages yet." else "Preview transcript lines: ${messages.size}"
    val transcriptEmptyTitle: String = when {
        !statusState.clientConnected -> "Room not open yet"
        peerListState.selectedPeer == null -> "Choose someone to start"
        else -> "Start the conversation"
    }
    val transcriptEmptyDetailConnected: String = if (peerListState.selectedPeer == null) {
        "You are in the shared room chat. Select a person on the left to chat, send files, or start a call."
    } else {
        "You are chatting with ${peerListState.selectedPeer.nickname}. Say hello to the room and your messages will appear here."
    }
    val transcriptEmptyDetailDisconnected: String =
        "You are in the shared room chat, but it is not active. Host or join a trusted LAN room to start messaging."
    val transcriptEmptyActionLabel: String = when {
        !statusState.clientConnected -> "Open or join a room"
        peerListState.selectedPeer == null -> "Select a person"
        else -> "Type your first message"
    }
    val transcriptEmptySituation: String = transcriptEmptyTitle
    val transcriptEmptyExplanation: String = if (statusState.clientConnected) transcriptEmptyDetailConnected else transcriptEmptyDetailDisconnected
    val transcriptEmptyNextAction: String = transcriptEmptyActionLabel
    val transcriptEmptyStructuredCopy: List<String> = listOf(transcriptEmptySituation, transcriptEmptyExplanation, transcriptEmptyNextAction)
    val transcriptEmptyVisualWeight: ComposeEmptyStateVisualWeight = ComposeEmptyStateVisualWeight.PRIMARY_GUIDANCE
    val draftValid: Boolean = draftMessage.trim().isNotEmpty()
    val canSendMessage: Boolean = statusState.clientConnected && draftValid
    val sendLabel: String = if (canSendMessage) "Send ready" else "Send blocked"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX chat workspace remains production fallback" else "JavaFX chat workspace fallback unavailable"
    val microinteractionChecklist: List<String> = listOf(
        "Hover highlights interactive rows without changing layout.",
        "Focus uses the design-system focus border and keeps keyboard navigation visible.",
        "Loading, success, and failure states use inline feedback pills instead of blocking dialogs.",
        "Connection, peer presence, transfer completion, and call changes append contextual transcript feedback.",
        "Composer focus returns after sending a message.",
    )
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) {
            add("Connect to chat before sending shared-room messages.")
        }
        if (!draftValid) {
            add("Type a non-empty message before sending.")
        }
        if (!javaFxFallbackAvailable) {
            add("JavaFX fallback is unavailable; keep live Compose chat sending disabled.")
        }
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Chat input is ready for the next live workspace wiring boundary."
    } else {
        blockedReasons.joinToString(" · ")
    }

    companion object {
        fun defaultPreviewMessages(): List<ComposeChatMessage> = listOf(
            ComposeChatMessage("Astra Laptop", "Desktop chat transcript preview", timestamp = Instant.parse("2026-05-26T20:01:00Z")),
            ComposeChatMessage("join", "Beta Phone", system = true, timestamp = Instant.parse("2026-05-26T20:02:00Z")),
        )
    }
}
