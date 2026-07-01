package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerListAdapterEventContract(
    val kind: ComposePeerListAdapterEventKind,
    val label: String,
    val ready: Boolean,
    val guarded: Boolean,
    val description: String,
    val prerequisites: List<String>,
    val blockedReason: String,
) {
    val routeTag: String = kind.name.lowercase().replace('_', '-')
    val readinessLabel: String = when {
        !ready && guarded -> "$label blocked (guarded)"
        !ready -> "$label blocked"
        else -> "$label ready"
    }
}
