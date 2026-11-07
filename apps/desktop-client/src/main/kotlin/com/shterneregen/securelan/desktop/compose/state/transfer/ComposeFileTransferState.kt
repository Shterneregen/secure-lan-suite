package com.shterneregen.securelan.desktop.compose.state.transfer

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import com.shterneregen.securelan.desktop.ui.TransferEntry

/**
 * Derives transfer readiness, receive prompts, active rows, and recent history for the workspace.
 * [entries] must contain detached event snapshots. [sessionPassword] is transient and must not be logged.
 */
data class ComposeFileTransferState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val selectedFilePath: String = "",
    val senderId: String = statusState.nickname,
    val sessionPassword: String = "",
    val entries: List<TransferEntry> = emptyList(),
    val incomingPrompts: List<ComposeIncomingTransferPrompt> = emptyList(),
    val autoAcceptFiles: Boolean = false,
) {
    val title: String = "Encrypted file transfer"
    val selectedPeer = peerListState.selectedPeer
    val selectedPeerName: String = selectedPeer?.nickname ?: "No peer selected"
    val activeCount: Long = entries.count { it.active() }.toLong()
    val failedCount: Int = entries.count { it.status == "Failed" }
    val waitingPromptCount: Int = incomingPrompts.count { it.waitingForDecision }
    val requiresAttention: Boolean = activeCount > 0 || waitingPromptCount > 0
    val hasRecentTransfers: Boolean = entries.isNotEmpty()
    val hint: String = DesktopTransferFormatters.formatTransferHint(activeCount, entries.isNotEmpty())
    val activeEntryRows: List<ComposeTransferRow> = entries.filter { it.active() }.map(ComposeTransferRow::from)
    val recentEntries: List<TransferEntry> = entries.filterNot { it.active() }.takeLast(4)
    val recentEntryRows: List<ComposeTransferRow> = recentEntries.map(ComposeTransferRow::from)
    val receiveModeLabel: String = "Save incoming files automatically"
    val receiveModeDescription: String =
        "Only files from known peers who are currently online can be received. Unknown or offline senders are always rejected."
    val receiveModeStatusLabel: String = if (autoAcceptFiles) "On" else "Off"
    val receiveModeSupportingText: String = if (autoAcceptFiles) {
        "Files from known online peers are saved without confirmation."
    } else {
        "You will review every incoming file before it is saved."
    }
    val receiveModeShortLabel: String = if (autoAcceptFiles) "Auto-save: on" else "Auto-save: off"
    val transferCardBadge: String? = buildList {
        if (activeCount > 0) add("$activeCount active")
        if (waitingPromptCount > 0) add("$waitingPromptCount to review")
        if (failedCount > 0) add("$failedCount failed")
        if (isEmpty() && recentEntryRows.isNotEmpty()) add("${recentEntryRows.size} recent")
    }.joinToString(" · ").takeIf(String::isNotBlank)
    val transferCardSummary: String = when {
        waitingPromptCount > 0 -> "Review incoming files before they are saved."
        activeCount > 0 -> "Transfer progress and recent activity."
        selectedPeer == null -> "Select an online peer, then use Attach to send a file."
        !statusState.clientConnected -> "Connect to chat before sending a file to $selectedPeerName."
        !selectedPeer.online -> "$selectedPeerName is offline."
        !selectedPeer.fileCapable ->
            "$selectedPeerName cannot receive files directly. Use Tools → Quick Share."

        else -> "Use Attach in chat to send a file to $selectedPeerName."
    }
    val transferCardTooltip: String = if (selectedPeer != null && !selectedPeer.fileCapable) {
        "This peer did not make direct file receiving available. Quick Share creates a temporary LAN link you can send in chat."
    } else {
        "Active transfers open automatically. To send a new file, select an online peer and use Attach."
    }
}
