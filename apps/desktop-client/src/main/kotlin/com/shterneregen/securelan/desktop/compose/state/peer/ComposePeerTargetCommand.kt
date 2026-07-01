package com.shterneregen.securelan.desktop.compose.state.peer

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionEvent
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionEventKind

public data class ComposePeerTargetCommand(
    val kind: ComposePeerTargetCommandKind,
    val label: String,
    val enabled: Boolean,
    val summary: String,
    val blockedReason: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val statusText: String = if (enabled) summary else blockedReason
    val queuedEvent: ComposeConnectionEvent = ComposeConnectionEvent(
        ComposeConnectionEventKind.INFO,
        "Queued ${kind.name.lowercase().replace('_', '-')} intent for the future live peer-list boundary: $summary",
    )
}
