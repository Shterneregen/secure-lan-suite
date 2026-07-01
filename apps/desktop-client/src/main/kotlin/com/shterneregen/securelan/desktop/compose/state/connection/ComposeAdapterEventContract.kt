package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeAdapterEventContract(
    val kind: ComposeAdapterEventKind,
    val label: String,
    val ready: Boolean,
    val guarded: Boolean,
    val description: String,
    val prerequisites: List<String>,
    val blockedReason: String,
    val cleanupAfter: List<ComposeAdapterEventKind>,
) {
    val routeTag: String = kind.name.lowercase().replace('_', '-')
    val readinessLabel: String = when {
        !ready && guarded -> "$label blocked (guarded)"
        !ready -> "$label blocked"
        else -> "$label ready"
    }
    val eventOrderNote: String = if (cleanupAfter.isEmpty()) "standalone" else "after ${
        cleanupAfter.joinToString {
            it.name.lowercase().replace('_', '-')
        }
    }"
}
