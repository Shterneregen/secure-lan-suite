package com.shterneregen.securelan.desktop.compose.state.chat

import java.nio.file.Path

enum class LocalTranscriptSeverity { INFO, SUCCESS, ERROR }

/** Local semantics are independent of display wording. Wire system text uses the legacy adapter. */
data class LocalTranscriptEvent(
    val kind: ComposeChatTranscriptLineKind,
    val body: String,
    val severity: LocalTranscriptSeverity = LocalTranscriptSeverity.INFO,
    val actionUrl: String? = null,
    val actionPath: Path? = null,
    val showsQrCode: Boolean = false,
) {
    val label: String get() = when (kind) {
        ComposeChatTranscriptLineKind.TRANSFER -> "File"
        ComposeChatTranscriptLineKind.QUICK_SHARE -> "Quick Share"
        ComposeChatTranscriptLineKind.PRESENCE -> "Presence"
        ComposeChatTranscriptLineKind.CALL -> "Call"
        ComposeChatTranscriptLineKind.SECURITY -> "Security"
        else -> "System"
    }
}
