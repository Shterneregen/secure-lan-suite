package com.shterneregen.securelan.desktop.compose.state.transfer

public data class ComposeAttachmentToolsState(
    val peerSelected: Boolean,
    val fileTargetReady: Boolean,
) {
    val title: String = "Attach"
    val layoutContract: ComposeAttachmentMenuLayoutContract = ComposeAttachmentMenuLayoutContract()
    val menuItems: List<ComposeAttachmentToolItem> = listOf(
        ComposeAttachmentToolItem(
            kind = ComposeAttachmentToolKind.SECURE_FILE,
            label = "Send secure file",
            enabled = fileTargetReady,
            statusText = when {
                fileTargetReady -> "Choose a file for the selected person."
                !peerSelected -> "Select an online person before sending a secure file."
                else -> "Direct sending is unavailable for this person. Use Tools → Quick Share instead."
            },
            shortcutHint = "Attach → Send secure file",
        ),
    )
}
