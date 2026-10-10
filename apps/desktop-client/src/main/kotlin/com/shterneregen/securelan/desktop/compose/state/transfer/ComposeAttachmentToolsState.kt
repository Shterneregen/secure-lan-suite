package com.shterneregen.securelan.desktop.compose.state.transfer

data class ComposeAttachmentToolsState(
    val peerSelected: Boolean,
    val fileTargetReady: Boolean,
) {
    val title: String = "Attach"
    val enabled: Boolean = fileTargetReady
    val statusText: String = when {
        fileTargetReady -> "Choose one or more files to send securely to the selected person."
        !peerSelected -> "Select an online person before attaching files."
        else -> "Direct sending is unavailable for this person. Use Tools → Quick Share instead."
    }
    val accessibilityLabel: String = if (enabled) {
        "$title files. $statusText"
    } else {
        "$title files unavailable. $statusText"
    }
}
