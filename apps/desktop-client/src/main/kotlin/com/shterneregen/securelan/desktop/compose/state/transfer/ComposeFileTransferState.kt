package com.shterneregen.securelan.desktop.compose.state.transfer

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeEmptyStateVisualWeight
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
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Encrypted file transfer"
    val selectedPeer = peerListState.selectedPeer
    val selectedPeerName: String = selectedPeer?.nickname ?: "No peer selected"
    val hasSelectedFile: Boolean = selectedFilePath.trim().isNotEmpty()
    val senderReady: Boolean = senderId.trim().isNotEmpty()
    val passwordReady: Boolean = sessionPassword.isNotEmpty()
    val sendTargetReady: Boolean = statusState.clientConnected && selectedPeer?.online == true && selectedPeer.fileCapable
    val listenerReady: Boolean = statusState.resolvedLocalFilePort != null
    val canSendSelectedFile: Boolean = sendTargetReady && hasSelectedFile && senderReady && passwordReady && javaFxFallbackAvailable
    val activeCount: Long = entries.count { it.active() }.toLong()
    val completedCount: Int = entries.count { it.status == "Completed" }
    val failedCount: Int = entries.count { it.status == "Failed" }
    val waitingPromptCount: Int = incomingPrompts.count { it.waitingForDecision }
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
    val recentEmptyTitle: String = "No recent transfers"
    val recentEmptySituation: String = recentEmptyTitle
    val recentEmptyExplanation: String = "Completed and failed file transfers will appear here after activity finishes."
    val recentEmptyNextAction: String = "Start from Attach or peer actions"
    val recentEmptyDetail: String =
        "$recentEmptyExplanation $recentEmptyNextAction."
    val recentEmptyStructuredCopy: List<String> = listOf(recentEmptySituation, recentEmptyExplanation, recentEmptyNextAction)
    val recentEmptyVisualWeight: ComposeEmptyStateVisualWeight = ComposeEmptyStateVisualWeight.INLINE
    val promptSummary: String = if (incomingPrompts.isEmpty()) {
        "No incoming receive prompts."
    } else {
        val waitingCount = incomingPrompts.count { it.waitingForDecision }
        "Incoming files: ${incomingPrompts.size}; $waitingCount waiting for review."
    }
    val receiveModeLabel: String = if (autoAcceptFiles) {
        "Auto-accept is on for known online peers"
    } else {
        "Ask before saving incoming files"
    }
    val receiveModeDescription: String = if (autoAcceptFiles) {
        "Known online peers can send files without an extra prompt. Unknown or offline senders are still rejected."
    } else {
        "Incoming files from known online peers require your confirmation before they are saved."
    }
    val receiveModeShortLabel: String = if (autoAcceptFiles) "Known peers auto-save" else "Ask before saving"
    val transferCountSummary: String = buildList {
        add("$activeCount active")
        add("$completedCount completed")
        if (failedCount > 0) add("$failedCount failed")
        if (waitingPromptCount > 0) add("$waitingPromptCount needs review")
    }.joinToString(" · ")
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
    val selectedFileSummary: String = if (hasSelectedFile) selectedFileName else "Choose a local file before sending."
    val passwordSummary: String = if (passwordReady) "Using the current room password" else "Reconnect with a room password before sending files."
    val senderSummary: String = if (senderReady) "Sending as ${senderId.trim()}" else "Reconnect with your name before sending files."
    val sendLabel: String = if (canSendSelectedFile) "Send file ready" else "Send file blocked"
    val receiveLabel: String = if (listenerReady) "Receive listener ready" else "Receive listener blocked"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX transfer workspace remains production fallback" else "JavaFX transfer workspace fallback unavailable"
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
        if (!javaFxFallbackAvailable) {
            add("JavaFX fallback is unavailable; keep live Compose file-transfer actions disabled.")
        }
    }
    val nextStepSummary: String = blockedReasons.firstOrNull() ?: "Ready to send encrypted file to $selectedPeerName."
    val chatAttachmentCards: List<ComposeChatAttachmentCard> = buildList {
        incomingPrompts.filter { it.waitingForDecision }.forEach { prompt ->
            add(ComposeChatAttachmentCard.incoming(prompt))
        }
        recentEntryRows.forEach { row ->
            add(ComposeChatAttachmentCard.transfer(row))
        }
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "File-transfer controls are ready."
    } else {
        blockedReasons.joinToString(" · ")
    }
}
