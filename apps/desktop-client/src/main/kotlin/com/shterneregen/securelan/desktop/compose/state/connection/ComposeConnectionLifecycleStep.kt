package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionLifecycleStep(
    val state: ComposeConnectionLifecycleState,
    val ready: Boolean,
    val label: String,
    val sideEffectContract: String,
) {
    val displayText: String = if (ready) "$label ready" else "$label blocked"
}
