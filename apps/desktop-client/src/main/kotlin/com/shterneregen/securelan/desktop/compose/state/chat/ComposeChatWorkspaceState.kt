package com.shterneregen.securelan.desktop.compose.state.chat

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import java.time.Instant

/** Derives shared-room composer readiness and transcript presentation from connection and peer state. */
data class ComposeChatWorkspaceState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val draftMessage: String = "Hello from Compose preview",
    val messages: List<ComposeChatMessage> = defaultPreviewMessages(),
) {
    val title: String = "Shared room chat"
    val subtitle: String = peerListState.selectedPeer?.let { peer ->
        "Actions on the right will target “${peer.nickname}”. Text chat is visible to everyone in this room."
    } ?: "Connect to chat, then select a peer on the left for voice, video, and file actions."

    val transcriptLines: List<String> = messages.map(ComposeChatMessage::displayText)
    val transcriptSummary: String =
        if (messages.isEmpty()) "No chat messages yet." else "Preview transcript lines: ${messages.size}"
    val transcriptEmptyTitle: String = when {
        statusState.localServerRunning || statusState.clientConnected -> "No messages yet"
        else -> "Room not open yet"
    }
    val transcriptEmptyDetailConnected: String =
        "Messages appear here for everyone in the room. Select a participant only when sending files or starting a call."
    val transcriptEmptyDetailDisconnected: String =
        "You are in the shared room chat, but it is not active. Host or join a trusted LAN room to start messaging."
    val transcriptEmptyExplanation: String = if (statusState.clientConnected || statusState.localServerRunning) transcriptEmptyDetailConnected else transcriptEmptyDetailDisconnected
    val draftValid: Boolean = draftMessage.trim().isNotEmpty()
    val canSendMessage: Boolean = statusState.clientConnected && draftValid
    val sendLabel: String = if (canSendMessage) "Send ready" else "Send blocked"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) {
            add("Connect to chat before sending shared-room messages.")
        }
        if (!draftValid) {
            add("Type a non-empty message before sending.")
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
