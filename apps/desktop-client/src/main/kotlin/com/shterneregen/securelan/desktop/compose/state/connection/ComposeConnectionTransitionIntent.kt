package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionTransitionIntent(
    val kind: ComposeConnectionTransitionKind,
    val label: String,
    val sourceState: ComposeConnectionLifecycleState,
    val targetState: ComposeConnectionLifecycleState,
    val enabled: Boolean,
    val guardSummary: String,
    val blockedReason: String,
    val cleanupPreview: String,
    val sideEffectContract: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val statusText: String = if (enabled) guardSummary else blockedReason
    val routeSummary: String = "${sourceState.name.lowercase()} -> ${targetState.name.lowercase()}"
    val queuedEvent: ComposeConnectionEvent = ComposeConnectionEvent(
        ComposeConnectionEventKind.INFO,
        "Queued ${
            kind.name.lowercase().replace('_', '-')
        } transition intent for the future live status/connection boundary: $routeSummary; $guardSummary",
    )
}
