package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionTransitionPlan(
    val transitions: List<ComposeConnectionTransitionIntent>,
) {
    val title: String = "Status/connection transition intents"
    val enabledTransitions: List<ComposeConnectionTransitionIntent> =
        transitions.filter(ComposeConnectionTransitionIntent::enabled)
    val blockedTransitions: List<ComposeConnectionTransitionIntent> =
        transitions.filterNot(ComposeConnectionTransitionIntent::enabled)
    val enabledSummary: String = if (enabledTransitions.isEmpty()) {
        "No status/connection transitions are ready for future live Compose wiring."
    } else {
        "Ready transitions: ${enabledTransitions.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedTransitions.isEmpty()) {
        "No status/connection transitions are blocked in this preview state."
    } else {
        "Blocked transitions: ${blockedTransitions.joinToString { it.label }}"
    }
    val cleanupSummary: String = transitions.joinToString(" · ") { "${it.label}: ${it.cleanupPreview}" }
    val sideEffectSummary: String = transitions.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    fun transition(kind: ComposeConnectionTransitionKind): ComposeConnectionTransitionIntent =
        transitions.first { it.kind == kind }

    companion object {
        fun from(
            state: ComposeStatusConnectionState,
            lifecyclePlan: ComposeConnectionLifecyclePlan = state.lifecyclePlan,
            controlPlan: ComposeConnectionControlPlan = state.controlPlan,
        ): ComposeConnectionTransitionPlan {
            val source = lifecyclePlan.currentState
            val fallbackBlock = "JavaFX fallback is unavailable; transition intents must remain local and blocked."
            fun blocked(command: ComposeConnectionCommand): String =
                if (!lifecyclePlan.fallbackAvailable) fallbackBlock else command.blockedReason

            fun cleanupPreviewFor(target: ComposeConnectionLifecycleState): String = when (target) {
                ComposeConnectionLifecycleState.HOSTED -> listOf(
                    "Stop discovery announcement",
                    "Disconnect local self-client if attached",
                    "Stop hosted chat server",
                    "Stop hosted file listener",
                    "Return to listen-only discovery",
                ).joinToString(" → ")

                ComposeConnectionLifecycleState.CONNECTED -> buildList {
                    add("Disconnect chat client")
                    add("Stop client-only local file listener")
                    if (state.localServerRunning) {
                        add("Keep hosted room running until Stop hosting is requested")
                    }
                    add("Return to listen-only discovery")
                }.joinToString(" → ")

                ComposeConnectionLifecycleState.BLOCKED_ERROR -> listOf(
                    "Do not invoke runtime services",
                    "Keep JavaFX fallback path active",
                    "Fix blockers before retrying live Compose binding",
                ).joinToString(" → ")

                ComposeConnectionLifecycleState.IDLE,
                ComposeConnectionLifecycleState.HOSTING_READY,
                ComposeConnectionLifecycleState.CONNECTING_READY,
                    -> "No runtime cleanup is planned while Compose stays side-effect free"
            }

            val openRoom = controlPlan.command(ComposeConnectionCommandKind.OPEN_ROOM)
            val stopHosting = controlPlan.command(ComposeConnectionCommandKind.STOP_HOSTING)
            val connect = controlPlan.command(ComposeConnectionCommandKind.CONNECT)
            val disconnect = controlPlan.command(ComposeConnectionCommandKind.DISCONNECT)
            val discoverability = controlPlan.command(ComposeConnectionCommandKind.SET_DISCOVERABLE)

            return ComposeConnectionTransitionPlan(
                listOf(
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.START_HOSTING,
                        label = "Start hosting transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.HOSTED,
                        enabled = source == ComposeConnectionLifecycleState.IDLE && openRoom.enabled && lifecyclePlan.step(
                            ComposeConnectionLifecycleState.HOSTING_READY
                        ).ready,
                        guardSummary = "Host inputs, local self-connect, file listener, and discovery config are prepared for a future live host transition.",
                        blockedReason = blocked(openRoom),
                        cleanupPreview = cleanupPreviewFor(ComposeConnectionLifecycleState.HOSTED),
                        sideEffectContract = "future implementation may start host services only after this local intent is accepted by a runtime adapter",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.STOP_HOSTING,
                        label = "Stop hosting transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.IDLE,
                        enabled = stopHosting.enabled,
                        guardSummary = "Hosted-room cleanup order is explicit before any future runtime stop call is allowed.",
                        blockedReason = blocked(stopHosting),
                        cleanupPreview = lifecyclePlan.cleanupOrderSummary,
                        sideEffectContract = "future implementation may stop discovery, local self-client, server, and file listener in the documented order",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.START_MANUAL_CONNECT,
                        label = "Manual connect transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.CONNECTED,
                        enabled = source == ComposeConnectionLifecycleState.IDLE && connect.enabled && lifecyclePlan.step(
                            ComposeConnectionLifecycleState.CONNECTING_READY
                        ).ready,
                        guardSummary = "Manual host, chat port, nickname, password, and local file listener are prepared for a future live connect transition.",
                        blockedReason = blocked(connect),
                        cleanupPreview = cleanupPreviewFor(ComposeConnectionLifecycleState.CONNECTED),
                        sideEffectContract = "future implementation may connect only through chat-core service boundaries after this intent is accepted",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.DISCONNECT_CLIENT,
                        label = "Disconnect transition",
                        sourceState = source,
                        targetState = if (state.localServerRunning) ComposeConnectionLifecycleState.HOSTED else ComposeConnectionLifecycleState.IDLE,
                        enabled = disconnect.enabled,
                        guardSummary = "Client disconnect cleanup is separated from hosted-room cleanup when hosting remains active.",
                        blockedReason = blocked(disconnect),
                        cleanupPreview = lifecyclePlan.cleanupOrderSummary,
                        sideEffectContract = "future implementation may disconnect the chat client without changing hosted-room ownership unless requested separately",
                    ),
                    ComposeConnectionTransitionIntent(
                        kind = ComposeConnectionTransitionKind.CHANGE_DISCOVERY_VISIBILITY,
                        label = "Discovery visibility transition",
                        sourceState = source,
                        targetState = ComposeConnectionLifecycleState.HOSTED,
                        enabled = discoverability.enabled,
                        guardSummary = "Discovery visibility change is local-intent only and keeps UDP payload format unchanged.",
                        blockedReason = blocked(discoverability),
                        cleanupPreview = "No cleanup; future runtime wiring may only switch announcement visibility while keeping listen-only discovery available.",
                        sideEffectContract = "future implementation may update discovery announcement visibility without changing ports or payload format",
                    ),
                ),
            )
        }
    }
}
