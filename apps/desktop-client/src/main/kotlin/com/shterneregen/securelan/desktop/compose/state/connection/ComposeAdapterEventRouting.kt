package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeAdapterEventRouting(
    val contracts: List<ComposeAdapterEventContract>,
    val summary: String,
    val fallbackAvailable: Boolean,
    val readyEvents: List<ComposeAdapterEventContract>,
    val blockedEvents: List<ComposeAdapterEventContract>,
    val cleanupOrderSummary: String,
) {
    val title: String = "Host runtime adapter event contract"
    val subtitle: String =
        "Side-effect-free event contract for future live status/connection integration; JavaFX still owns all runtime services."
    val readyCount: Int = readyEvents.size
    val blockedCount: Int = blockedEvents.size
    val totalCount: Int = contracts.size
    val readinessSummary: String = when {
        readyCount == totalCount -> "All $totalCount adapter events are ready for future live wiring."
        blockedCount == totalCount -> "All $totalCount adapter events are blocked; JavaFX fallback must remain active."
        else -> "$readyCount of $totalCount adapter events ready; $blockedCount blocked."
    }
    val fallbackStatus: String =
        if (fallbackAvailable) "JavaFX fallback available; adapter event routing is speculative." else "JavaFX fallback unavailable; live adapter event routing must remain blocked."
    val blockedSummary: String = if (blockedEvents.isEmpty()) {
        "No adapter events are blocked; all event contracts are ready for the future runtime adapter boundary."
    } else {
        blockedEvents.joinToString(" · ") { "${it.readinessLabel}: ${it.blockedReason}" }
    }
    val eventOrderLabel: String = contracts.joinToString(" → ") { it.routeTag }

    companion object {
        fun from(
            state: ComposeStatusConnectionState,
            lifecyclePlan: ComposeConnectionLifecyclePlan = state.lifecyclePlan,
            runtimePlan: ComposeConnectionRuntimePlan = state.runtimePlan,
        ): ComposeAdapterEventRouting {
            val fallbackAvailable = state.javaFxFallbackAvailable
            val fallbackBlock = "JavaFX fallback is unavailable; live adapter event routing must remain blocked."
            fun block(reason: String): String = if (!fallbackAvailable) fallbackBlock else reason

            val hostingReady =
                fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.HOSTING_READY).ready
            val hosted = fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.HOSTED).ready
            val connectingReady =
                fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.CONNECTING_READY).ready
            val connected = fallbackAvailable && lifecyclePlan.step(ComposeConnectionLifecycleState.CONNECTED).ready

            val hostStartedReady = hostingReady && !hosted
            val hostStartedBlocked = if (!fallbackAvailable) fallbackBlock
            else if (!state.nicknameValid) "Nickname is blank; host-started event cannot be received."
            else if (state.serverChatPort == null || state.serverFilePort == null) "Room ports are invalid; host-started event cannot be received."
            else if (state.localServerRunning) "Room is already hosted; host-started event is redundant while hosted-event is in effect."
            else "Host-started event prerequisites are not met."

            val hostStoppedReady = hosted
            val hostStoppedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Room is not currently hosted; host-stopped event can only fire after a hosted state."

            val connectStartedReady = connectingReady && !state.clientConnected
            val connectStartedBlocked = if (!fallbackAvailable) fallbackBlock
            else if (!state.nicknameValid) "Nickname is blank; connect-started event cannot be received."
            else if (!state.manualHostValid) "Manual host is blank; connect-started event cannot be received."
            else if (state.clientChatPort == null || state.clientFilePort == null) "Manual connection ports are invalid; connect-started event cannot be received."
            else if (state.clientConnected) "Client is already connected; connect-started event is redundant while connected-event is in effect."
            else "Connect-started event prerequisites are not met."

            val connectedReady = connected
            val connectedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Client is not connected; connected event can only fire after a live manual connection is established."

            val connectFailedReady = connectingReady
            val connectFailedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Manual connection inputs are not ready; connect-failed event cannot fire without a valid connect target."

            val disconnectedReady = connected
            val disconnectedBlocked = if (!fallbackAvailable) fallbackBlock
            else "Client is not connected; disconnected event can only fire after a client disconnect occurs."

            val discoveryVisibilityReady = hosted
            val discoveryVisibilityBlocked = if (!fallbackAvailable) fallbackBlock
            else "Room is not hosted; discovery visibility change events can only fire while a room is hosted."

            val runtimeErrorBlocked = if (!fallbackAvailable) fallbackBlock
            else "No runtime errors are expected in this preview state; runtime-error event is guarded and fires only on actual errors."

            val cleanupStartedReady = hosted || connected
            val cleanupStartedBlocked = if (!fallbackAvailable) fallbackBlock
            else "No active runtime state requires cleanup; cleanup-started event is guarded until hosting or connection is active."

            val cleanupCompletedReady = hosted || connected
            val cleanupCompletedBlocked = if (!fallbackAvailable) fallbackBlock
            else "No active runtime state requires cleanup completion; cleanup-completed event is guarded until cleanup-started fires."

            val contracts = listOf(
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.HOST_STARTED,
                    label = "Host started",
                    ready = hostStartedReady,
                    guarded = !hostStartedReady && block(hostStartedBlocked) == hostStartedBlocked && fallbackAvailable,
                    description = "Future runtime adapter fires this when the hosted chat server, file listener, and discovery announcement are live.",
                    prerequisites = listOf("valid nickname", "valid room ports", "not already hosted"),
                    blockedReason = block(hostStartedBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.HOST_STOPPED,
                    label = "Host stopped",
                    ready = hostStoppedReady,
                    guarded = !hostStoppedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this after discovery, self-client, chat server, and file listener are stopped.",
                    prerequisites = listOf("room currently hosted"),
                    blockedReason = block(hostStoppedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.HOST_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CONNECT_STARTED,
                    label = "Connect started",
                    ready = connectStartedReady,
                    guarded = !connectStartedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when a manual chat-client connection attempt begins.",
                    prerequisites = listOf("valid manual host", "valid manual ports", "not already connected"),
                    blockedReason = block(connectStartedBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CONNECTED,
                    label = "Connected",
                    ready = connectedReady,
                    guarded = !connectedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when the chat client successfully connects to a remote peer.",
                    prerequisites = listOf("client connection active"),
                    blockedReason = block(connectedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CONNECT_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CONNECT_FAILED,
                    label = "Connect failed",
                    ready = connectFailedReady,
                    guarded = true,
                    description = "Future runtime adapter fires this when a manual connection attempt fails; it is guarded and only fires on actual failures.",
                    prerequisites = listOf("valid manual connection inputs ready"),
                    blockedReason = block(connectFailedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CONNECT_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.DISCONNECTED,
                    label = "Disconnected",
                    ready = disconnectedReady,
                    guarded = !disconnectedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this after the chat client disconnects and cleanup order is observed.",
                    prerequisites = listOf("client was connected"),
                    blockedReason = block(disconnectedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CONNECTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED,
                    label = "Discovery visibility changed",
                    ready = discoveryVisibilityReady,
                    guarded = !discoveryVisibilityReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when discovery announcement visibility switches without changing ports or payload format.",
                    prerequisites = listOf("room currently hosted"),
                    blockedReason = block(discoveryVisibilityBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.HOST_STARTED),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.RUNTIME_ERROR,
                    label = "Runtime error",
                    ready = false,
                    guarded = true,
                    description = "Future runtime adapter fires this when an unexpected runtime error occurs; it is always guarded and fires only on exceptional conditions.",
                    prerequisites = listOf("actual runtime error occurs"),
                    blockedReason = block(runtimeErrorBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CLEANUP_STARTED,
                    label = "Cleanup started",
                    ready = cleanupStartedReady,
                    guarded = !cleanupStartedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this when the documented cleanup order begins executing.",
                    prerequisites = listOf("active runtime state (hosted or connected)"),
                    blockedReason = block(cleanupStartedBlocked),
                    cleanupAfter = emptyList(),
                ),
                ComposeAdapterEventContract(
                    kind = ComposeAdapterEventKind.CLEANUP_COMPLETED,
                    label = "Cleanup completed",
                    ready = cleanupCompletedReady,
                    guarded = !cleanupCompletedReady && fallbackAvailable,
                    description = "Future runtime adapter fires this after the cleanup order is fully executed and resources are released.",
                    prerequisites = listOf("cleanup was started"),
                    blockedReason = block(cleanupCompletedBlocked),
                    cleanupAfter = listOf(ComposeAdapterEventKind.CLEANUP_STARTED),
                ),
            )

            val readyEvents = contracts.filter { it.ready }
            val blockedEvents = contracts.filterNot { it.ready }
            val cleanupOrderSummary =
                if (contracts.none { it.ready && it.kind == ComposeAdapterEventKind.CLEANUP_COMPLETED }) {
                    "Cleanup event order is not yet applicable; no active runtime state requires cleanup."
                } else {
                    "Cleanup events fire in deterministic order: cleanup-started → cleanup-completed after hosted/connected events are resolved."
                }

            return ComposeAdapterEventRouting(
                contracts = contracts,
                summary = lifecyclePlan.sideEffectContractSummary,
                fallbackAvailable = fallbackAvailable,
                readyEvents = readyEvents,
                blockedEvents = blockedEvents,
                cleanupOrderSummary = cleanupOrderSummary,
            )
        }
    }
}
