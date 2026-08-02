package com.shterneregen.securelan.desktop.compose.state.transfer

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import com.shterneregen.securelan.desktop.ui.TransferEntry
import java.nio.file.Paths

public data class ComposeFileTransferState(
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
    val hasSelectedFile: Boolean = selectedFilePath.trim().isNotEmpty()
    val senderReady: Boolean = senderId.trim().isNotEmpty()
    val passwordReady: Boolean = sessionPassword.isNotEmpty()
    val sendTargetReady: Boolean =
        statusState.clientConnected && selectedPeer?.online == true && selectedPeer.fileCapable
    val listenerReady: Boolean = statusState.resolvedLocalFilePort != null
    val canSendSelectedFile: Boolean = sendTargetReady && hasSelectedFile && senderReady && passwordReady
    val activeCount: Long = entries.count { it.active() }.toLong()
    val completedCount: Int = entries.count { it.status == "Completed" }
    val failedCount: Int = entries.count { it.status == "Failed" }
    val waitingPromptCount: Int = incomingPrompts.count { it.waitingForDecision }
    val requiresAttention: Boolean = activeCount > 0 || waitingPromptCount > 0
    val hasRecentTransfers: Boolean = entries.isNotEmpty()
    val hint: String = DesktopTransferFormatters.formatTransferHint(activeCount, entries.isNotEmpty())
    val heroTitle: String = when {
        activeCount > 0 -> DesktopTransferFormatters.formatActiveTransferSummary(activeCount)
        waitingPromptCount > 0 -> "$waitingPromptCount incoming file${if (waitingPromptCount == 1) "" else "s"} waiting"
        entries.isNotEmpty() -> "Transfers are idle"
        else -> "Ready when you need to send a file"
    }
    val heroSubtitle: String = when {
        activeCount > 0 -> "Keep this panel open to watch file progress."
        waitingPromptCount > 0 -> "Review incoming files before saving."
        entries.isNotEmpty() -> "Recent transfers remain below for reference."
        else -> "Choose an online peer, pick a file, and enter the shared password."
    }
    val activeSummary: String = DesktopTransferFormatters.formatActiveTransferSummary(activeCount)
    val entryRows: List<String> = entries.map(DesktopTransferFormatters::formatTransferListMeta)
    val recentEntries: List<TransferEntry> = entries.takeLast(4)
    val recentEntryRows: List<ComposeTransferRow> = recentEntries.map(ComposeTransferRow::from)
    val promptSummary: String = if (incomingPrompts.isEmpty()) {
        "No incoming receive prompts."
    } else {
        val waitingCount = incomingPrompts.count { it.waitingForDecision }
        "Incoming files: ${incomingPrompts.size}; $waitingCount waiting for review."
    }
    val receiveModeLabel: String = "Save incoming files automatically"
    val receiveModeDescription: String =
        "Only files from known peers who are currently online can be received. Unknown or offline senders are always rejected."
    val receiveModeStatusLabel: String = if (autoAcceptFiles) "On" else "Off"
    val receiveModeSupportingText: String = if (autoAcceptFiles) {
        "Files from known online peers are saved without confirmation."
    } else {
        "You will review every incoming file before it is saved."
    }
    val receiveModeShortLabel: String = if (autoAcceptFiles) "Auto-save on" else "Ask before saving"
    val transferCountSummary: String = buildList {
        if (activeCount > 0) add("$activeCount active")
        if (completedCount > 0) add("$completedCount completed")
        if (failedCount > 0) add("$failedCount failed")
        if (waitingPromptCount > 0) add("$waitingPromptCount needs review")
    }.joinToString(" · ").ifBlank { "No transfer activity" }
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
            "$selectedPeerName cannot receive files directly. Use Tools → Share on LAN."

        else -> "Send a file to $selectedPeerName or review recent activity."
    }
    val transferCardTooltip: String = if (selectedPeer != null && !selectedPeer.fileCapable) {
        "This peer did not make direct file receiving available. Quick Share creates a temporary LAN link you can send in chat."
    } else {
        "Active transfers open automatically. To send a new file, select an online peer and use Attach."
    }
    val targetSummary: String = if (sendTargetReady) {
        if (selectedPeer?.discovered == true) {
            "Sending to $selectedPeerName over its discovered LAN file endpoint."
        } else {
            "Sending to $selectedPeerName over its chat file receiver."
        }
    } else {
        selectedPeer?.let { peer ->
            when {
                !statusState.clientConnected -> "Connect to chat before sending to ${peer.nickname}."
                !peer.online -> "${peer.nickname} is offline. Wait until the peer is online."
                !peer.fileCapable -> "${peer.nickname} is online, but no file receiver endpoint is available."
                else -> "Select an online peer before sending."
            }
        } ?: "Select an online peer from the Peers list."
    }
    val selectedFileName: String = selectedFilePath.trim()
        .takeIf(String::isNotEmpty)
        ?.let { runCatching { Paths.get(it).fileName?.toString() ?: it }.getOrDefault(it) }
        ?: "No file selected"
    val selectedFileSummary: String = if (hasSelectedFile) selectedFileName else "No file selected"
    val passwordSummary: String =
        if (passwordReady) "Using the current room password" else "Reconnect with a room password before sending files."
    val senderSummary: String =
        if (senderReady) "Sending as ${senderId.trim()}" else "Reconnect with your name before sending files."
    val sendLabel: String = if (canSendSelectedFile) "Send file ready" else "Send file blocked"
    val receiveLabel: String = if (listenerReady) "Receive listener ready" else "Receive listener blocked"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) {
            add("Connect to chat before sending encrypted files.")
        }
        if (selectedPeer == null) {
            add("Select an online peer before sending files.")
        } else if (!selectedPeer.online) {
            add("Selected peer is offline; wait for discovery or chat presence refresh.")
        } else if (!selectedPeer.fileCapable) {
            add("Selected peer does not have an available file receiver endpoint.")
        }
        if (!hasSelectedFile) {
            add("Choose a local file before sending.")
        }
        if (!senderReady) {
            add("Enter a sender name before sending files.")
        }
        if (!passwordReady) {
            add("Enter the file-transfer session password before sending files.")
        }
        if (!listenerReady) {
            add("Configure a valid local file-transfer listener port.")
        }
    }
    val nextStepSummary: String = blockedReasons.firstOrNull() ?: "Ready to send encrypted file to $selectedPeerName."
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "File-transfer controls are ready."
    } else {
        blockedReasons.joinToString(" · ")
    }
}
