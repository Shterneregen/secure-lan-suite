package com.shterneregen.securelan.desktop.compose.state.transfer

public data class ComposeChatAttachmentCard(
    val title: String,
    val subtitle: String,
    val progressPercent: Int,
    val active: Boolean,
    val failed: Boolean,
    val needsDecision: Boolean,
) {
    val progressLabel: String = when {
        needsDecision -> "Needs review"
        active -> "Transferring · $progressPercent%"
        failed -> "Failed"
        progressPercent >= 100 -> "Complete"
        else -> "$progressPercent%"
    }

    companion object {
        fun incoming(prompt: ComposeIncomingTransferPrompt): ComposeChatAttachmentCard = ComposeChatAttachmentCard(
            title = "Incoming file · ${prompt.fileName}",
            subtitle = "${prompt.senderId} wants to send ${prompt.sizeLabel}",
            progressPercent = 0,
            active = false,
            failed = false,
            needsDecision = true,
        )

        fun transfer(row: ComposeTransferRow): ComposeChatAttachmentCard = ComposeChatAttachmentCard(
            title = row.title,
            subtitle = row.detail,
            progressPercent = row.percent,
            active = row.active,
            failed = row.failed,
            needsDecision = false,
        )
    }
}
