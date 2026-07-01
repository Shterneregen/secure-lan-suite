package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionEventPreview(
    val events: List<ComposeConnectionEvent>,
) {
    val title: String = "Status/connection event preview"
    val latestEvent: ComposeConnectionEvent? = events.lastOrNull()
    val latestMessage: String = latestEvent?.message ?: "No status or connection events are available yet."
    val hasErrors: Boolean = events.any { it.kind == ComposeConnectionEventKind.ERROR }
    val hasWarnings: Boolean = events.any { it.kind == ComposeConnectionEventKind.WARNING }
    val summary: String = when {
        hasErrors -> "Blocked status/connection actions require attention before live Compose wiring."
        hasWarnings -> "Status/connection actions are partially ready; review warnings before live Compose wiring."
        else -> "Status/connection actions are ready for the next side-effect wiring boundary."
    }

    companion object {
        fun from(state: ComposeStatusConnectionState): ComposeConnectionEventPreview {
            val plan = state.runtimePlan
            return ComposeConnectionEventPreview(
                buildList {
                    add(ComposeConnectionEvent(ComposeConnectionEventKind.INFO, state.serverStatus))
                    add(ComposeConnectionEvent(ComposeConnectionEventKind.INFO, state.connectionStatus))
                    add(ComposeConnectionEvent(ComposeConnectionEventKind.INFO, plan.discoveryAnnouncement))
                    if (plan.hostingReady) {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.SUCCESS, plan.hostingSummary))
                    } else {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.WARNING, plan.hostingSummary))
                    }
                    if (plan.manualConnectionReady) {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.SUCCESS, plan.manualConnectionSummary))
                    } else {
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.WARNING, plan.manualConnectionSummary))
                    }
                    plan.disabledReasons.forEach { reason ->
                        add(ComposeConnectionEvent(ComposeConnectionEventKind.ERROR, reason))
                    }
                    if (!state.javaFxFallbackAvailable) {
                        add(
                            ComposeConnectionEvent(
                                ComposeConnectionEventKind.ERROR,
                                "JavaFX fallback is unavailable; live Compose wiring must stay disabled."
                            )
                        )
                    }
                },
            )
        }
    }
}
