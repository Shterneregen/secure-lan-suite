package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionControlPlan(
    val commands: List<ComposeConnectionCommand>,
) {
    val title: String = "Status/connection control boundary"
    val enabledCommands: List<ComposeConnectionCommand> = commands.filter(ComposeConnectionCommand::enabled)
    val disabledCommands: List<ComposeConnectionCommand> = commands.filterNot(ComposeConnectionCommand::enabled)
    val enabledSummary: String = if (enabledCommands.isEmpty()) {
        "No status/connection controls are ready for Compose-side command dispatch."
    } else {
        "Ready controls: ${enabledCommands.joinToString { it.label }}"
    }
    val disabledSummary: String = if (disabledCommands.isEmpty()) {
        "No status/connection controls are blocked in this state."
    } else {
        "Blocked controls: ${disabledCommands.joinToString { it.label }}"
    }

    fun command(kind: ComposeConnectionCommandKind): ComposeConnectionCommand = commands.first { it.kind == kind }

    companion object {
        fun from(state: ComposeStatusConnectionState): ComposeConnectionControlPlan {
            val plan = state.runtimePlan
            val fallbackBlock = "JavaFX fallback is unavailable; keep live Compose status/connection dispatch disabled."
            fun blocked(defaultReason: String): String =
                if (!state.javaFxFallbackAvailable) fallbackBlock else defaultReason

            return ComposeConnectionControlPlan(
                listOf(
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.OPEN_ROOM,
                        label = "Open room",
                        enabled = state.javaFxFallbackAvailable && state.actionState.openRoomReady && plan.hostingReady,
                        summary = plan.hostingSummary,
                        blockedReason = blocked(plan.disabledReasons.firstOrNull { it.startsWith("Hosting command") }
                            ?: plan.hostingSummary),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.STOP_HOSTING,
                        label = "Stop hosting",
                        enabled = state.javaFxFallbackAvailable && state.actionState.stopHostingReady,
                        summary = "Stop hosting the room so it is no longer visible on the network.",
                        blockedReason = blocked("Stop hosting is blocked until a room is currently hosted."),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.CONNECT,
                        label = "Connect",
                        enabled = state.javaFxFallbackAvailable && state.actionState.connectReady && plan.manualConnectionReady,
                        summary = plan.manualConnectionSummary,
                        blockedReason = blocked(plan.disabledReasons.firstOrNull { it.startsWith("Manual connection command") }
                            ?: plan.manualConnectionSummary),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.DISCONNECT,
                        label = "Disconnect",
                        enabled = state.javaFxFallbackAvailable && state.actionState.disconnectReady,
                        summary = "Leave the joined room. File sharing continues if you are hosting a room.",
                        blockedReason = blocked("Disconnect is blocked until you are connected."),
                    ),
                    ComposeConnectionCommand(
                        kind = ComposeConnectionCommandKind.SET_DISCOVERABLE,
                        label = if (state.discoverable) "Hide room" else "Make discoverable",
                        enabled = state.javaFxFallbackAvailable && state.actionState.discoverabilityToggleReady,
                        summary = if (state.discoverable) {
                            "Hide the room from nearby discovery; people with the address can still join."
                        } else {
                            "Show the room to nearby trusted peers."
                        },
                        blockedReason = blocked("Room visibility can change only while you are hosting."),
                    ),
                ),
            )
        }
    }
}
