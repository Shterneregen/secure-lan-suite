package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionLifecyclePlan(
    val currentState: ComposeConnectionLifecycleState,
    val steps: List<ComposeConnectionLifecycleStep>,
    val blockedReasons: List<String>,
    val fallbackAvailable: Boolean,
    val rollbackFallbackRequired: Boolean,
    val cleanupOrder: List<String>,
) {
    val title: String = "Live status/connection binding contract"
    val stateLabel: String = currentState.name.lowercase().replace('_', '/')
    val readySteps: List<ComposeConnectionLifecycleStep> = steps.filter(ComposeConnectionLifecycleStep::ready)
    val readinessSummary: String = if (readySteps.isEmpty()) {
        "No live lifecycle steps are ready for Compose wiring in the current preview state."
    } else {
        "Ready lifecycle steps: ${readySteps.joinToString { it.label }}"
    }
    val blockedSummary: String = if (blockedReasons.isEmpty()) {
        "No lifecycle blockers; service calls still remain intentionally deferred to a later slice."
    } else {
        blockedReasons.joinToString(" · ")
    }
    val fallbackStatus: String = if (fallbackAvailable) {
        "JavaFX fallback available for rollback"
    } else {
        "JavaFX fallback unavailable; live Compose binding must remain blocked"
    }
    val cleanupOrderSummary: String = cleanupOrder.joinToString(" → ")
    val sideEffectContractSummary: String = steps.joinToString(" · ") { "${it.label}: ${it.sideEffectContract}" }

    fun step(state: ComposeConnectionLifecycleState): ComposeConnectionLifecycleStep = steps.first { it.state == state }

    companion object {
        fun from(
            state: ComposeStatusConnectionState,
            runtimePlan: ComposeConnectionRuntimePlan = state.runtimePlan,
        ): ComposeConnectionLifecyclePlan {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val blockers = blockedReasonsFor(state, fallbackAvailable)
            val currentState = when {
                blockers.isNotEmpty() -> ComposeConnectionLifecycleState.BLOCKED_ERROR
                state.clientConnected -> ComposeConnectionLifecycleState.CONNECTED
                state.localServerRunning -> ComposeConnectionLifecycleState.HOSTED
                else -> ComposeConnectionLifecycleState.IDLE
            }
            val steps = listOf(
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.IDLE,
                    ready = currentState == ComposeConnectionLifecycleState.IDLE && blockers.isEmpty(),
                    label = "Idle",
                    sideEffectContract = "observe validated preview state only; do not start sockets or subscribe to discovery",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.HOSTING_READY,
                    ready = fallbackAvailable && runtimePlan.hostingReady,
                    label = "Hosting-ready",
                    sideEffectContract = "prepare host, local self-connect, file listener, and discovery announcement inputs without invoking services",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.HOSTED,
                    ready = fallbackAvailable && state.localServerRunning,
                    label = "Hosted",
                    sideEffectContract = "reflect hosted-room status and discovery visibility while JavaFX remains runtime owner",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.CONNECTING_READY,
                    ready = fallbackAvailable && runtimePlan.manualConnectionReady,
                    label = "Connecting-ready",
                    sideEffectContract = "prepare manual connect and client file-listener inputs without opening sockets",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.CONNECTED,
                    ready = fallbackAvailable && state.clientConnected,
                    label = "Connected",
                    sideEffectContract = "reflect connected-client status while chat, file-transfer, and RTC signaling stay JavaFX-owned",
                ),
                ComposeConnectionLifecycleStep(
                    state = ComposeConnectionLifecycleState.BLOCKED_ERROR,
                    ready = blockers.isNotEmpty(),
                    label = "Blocked/error",
                    sideEffectContract = "surface validation or fallback blockers before any live binding can run",
                ),
            )

            return ComposeConnectionLifecyclePlan(
                currentState = currentState,
                steps = steps,
                blockedReasons = blockers,
                fallbackAvailable = fallbackAvailable,
                rollbackFallbackRequired = true,
                cleanupOrder = cleanupOrderFor(currentState, state.localServerRunning),
            )
        }

        private fun blockedReasonsFor(
            state: ComposeStatusConnectionState,
            fallbackAvailable: Boolean,
        ): List<String> = buildList {
            if (!fallbackAvailable) {
                add("JavaFX fallback is unavailable; rollback safety is required before live Compose binding.")
            }
            if (!state.nicknameValid) {
                add("Nickname is blank; host/connect lifecycle must remain blocked.")
            }
            if (state.serverChatPort == null || state.serverFilePort == null) {
                add("Room ports are invalid; hosted lifecycle cannot be prepared.")
            }
            if (!state.manualHostValid) {
                add("Manual host is blank; connecting lifecycle cannot be prepared.")
            }
            if (state.clientChatPort == null || state.clientFilePort == null) {
                add("Manual connection ports are invalid; connected lifecycle cannot be prepared.")
            }
        }

        private fun cleanupOrderFor(
            state: ComposeConnectionLifecycleState,
            hostingActive: Boolean,
        ): List<String> = when (state) {
            ComposeConnectionLifecycleState.IDLE,
            ComposeConnectionLifecycleState.HOSTING_READY,
            ComposeConnectionLifecycleState.CONNECTING_READY,
                -> listOf("No runtime cleanup is planned while Compose stays side-effect free")

            ComposeConnectionLifecycleState.HOSTED -> listOf(
                "Stop discovery announcement",
                "Disconnect local self-client if attached",
                "Stop hosted chat server",
                "Stop hosted file listener",
                "Return to listen-only discovery",
            )

            ComposeConnectionLifecycleState.CONNECTED -> buildList {
                add("Disconnect chat client")
                add("Stop client-only local file listener")
                if (hostingActive) {
                    add("Keep hosted room running until Stop hosting is requested")
                }
                add("Return to listen-only discovery")
            }

            ComposeConnectionLifecycleState.BLOCKED_ERROR -> listOf(
                "Do not invoke runtime services",
                "Keep JavaFX fallback path active",
                "Fix blockers before retrying live Compose binding",
            )
        }
    }
}
