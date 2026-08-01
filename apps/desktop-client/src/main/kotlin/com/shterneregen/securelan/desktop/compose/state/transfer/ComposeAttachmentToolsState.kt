package com.shterneregen.securelan.desktop.compose.state.transfer

public data class ComposeAttachmentToolsState(
    val peerSelected: Boolean,
    val fileTargetReady: Boolean,
    val quickShareAvailable: Boolean = true,
    val steganographyAvailable: Boolean = true,
) {
    val title: String = "Attach"
    val layoutContract: ComposeAttachmentMenuLayoutContract = ComposeAttachmentMenuLayoutContract()
    val menuItems: List<ComposeAttachmentToolItem> = buildList {
        add(
            ComposeAttachmentToolItem(
                kind = ComposeAttachmentToolKind.SECURE_FILE,
                label = "Send secure file",
                enabled = fileTargetReady,
                statusText = when {
                    fileTargetReady -> "Choose a file for the selected person."
                    !peerSelected -> "Select an online person before sending a secure file."
                    else -> "Direct sending is unavailable for this person. Use Share on LAN temporarily below."
                },
                shortcutHint = "Attach → Send secure file",
            ),
        )
        if (quickShareAvailable) {
            add(
                ComposeAttachmentToolItem(
                    kind = ComposeAttachmentToolKind.QUICK_SHARE,
                    label = "Share on LAN temporarily",
                    enabled = true,
                    statusText = "Create a temporary trusted-LAN browser link.",
                    shortcutHint = "Attach → Share on LAN temporarily",
                ),
            )
        }
        add(
            ComposeAttachmentToolItem(
                kind = ComposeAttachmentToolKind.ENCRYPTED_TEXT_OR_FILE,
                label = "Send encrypted text or file",
                enabled = false,
                statusText = "Use secure file sending or Quick Share here; encrypted text packaging remains in the privacy workflow.",
                shortcutHint = "Attach → Send encrypted text or file",
            ),
        )
        if (steganographyAvailable) {
            add(
                ComposeAttachmentToolItem(
                    kind = ComposeAttachmentToolKind.STEGANOGRAPHY,
                    label = "Steganography",
                    enabled = true,
                    statusText = "Hide a message in an image or extract one from a stego BMP.",
                    shortcutHint = "Attach → Steganography",
                ),
            )
        }
    }
    val primaryItems: List<String> = menuItems.map(ComposeAttachmentToolItem::label)
    val enabledItems: List<ComposeAttachmentToolItem> = menuItems.filter(ComposeAttachmentToolItem::enabled)
    val disabledItems: List<ComposeAttachmentToolItem> = menuItems.filterNot(ComposeAttachmentToolItem::enabled)
    val disabledStatusText: String = disabledItems.firstOrNull()?.statusText
        ?: "All attachment actions are available in this context."
    val discoverableWithinTwoInteractions: Boolean = listOf(
        ComposeAttachmentToolKind.SECURE_FILE,
        ComposeAttachmentToolKind.QUICK_SHARE,
        ComposeAttachmentToolKind.STEGANOGRAPHY,
    ).all { requiredKind -> menuItems.any { it.kind == requiredKind } }
    val preservesKeyboardAccess: Boolean = true
    val restoresFocusAfterDismissal: Boolean = true
    val summary: String = when {
        peerSelected && fileTargetReady -> "Choose how to add a file to this conversation."
        peerSelected -> "Direct sending is unavailable. Share on LAN temporarily instead."
        else -> "Select an online peer to send secure files; LAN sharing and privacy tools stay available."
    }
    val keepsAdvancedToolsContextual: Boolean = menuItems.any { it.kind == ComposeAttachmentToolKind.QUICK_SHARE } &&
        menuItems.any { it.kind == ComposeAttachmentToolKind.STEGANOGRAPHY }
}
