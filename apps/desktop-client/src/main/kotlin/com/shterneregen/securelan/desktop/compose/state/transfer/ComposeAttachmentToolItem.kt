package com.shterneregen.securelan.desktop.compose.state.transfer

public data class ComposeAttachmentToolItem(
    val kind: ComposeAttachmentToolKind,
    val label: String,
    val enabled: Boolean,
    val statusText: String,
    val shortcutHint: String,
) {
    val accessibilityLabel: String = if (enabled) {
        "$label. $statusText. $shortcutHint"
    } else {
        "$label unavailable. $statusText. $shortcutHint"
    }
}
