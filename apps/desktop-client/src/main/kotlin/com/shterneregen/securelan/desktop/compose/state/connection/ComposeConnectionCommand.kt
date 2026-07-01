package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionCommand(
    val kind: ComposeConnectionCommandKind,
    val label: String,
    val enabled: Boolean,
    val summary: String,
    val blockedReason: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val statusText: String = if (enabled) summary else blockedReason
    val queuedEvent: ComposeConnectionEvent = ComposeConnectionEvent(
        ComposeConnectionEventKind.INFO,
        "Queued ${
            kind.name.lowercase().replace('_', '-')
        } command for the future live status/connection boundary: $summary",
    )
}
