package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerListTransitionIntent(
    val kind: ComposePeerListTransitionKind,
    val label: String,
    val sourceState: ComposePeerListLifecycleState,
    val targetState: ComposePeerListLifecycleState,
    val enabled: Boolean,
    val guardSummary: String,
    val blockedReason: String,
    val sideEffectContract: String,
) {
    val displayLabel: String = if (enabled) label else "$label blocked"
    val routeSummary: String = "${sourceState.name.lowercase()} -> ${targetState.name.lowercase()}"
}
