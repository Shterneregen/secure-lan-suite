package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerListLifecycleStep(
    val state: ComposePeerListLifecycleState,
    val ready: Boolean,
    val label: String,
    val sideEffectContract: String,
) {
    val displayText: String = if (ready) "$label ready" else "$label blocked"
}
