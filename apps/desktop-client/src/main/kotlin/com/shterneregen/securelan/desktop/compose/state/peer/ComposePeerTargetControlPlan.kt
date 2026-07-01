package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerTargetControlPlan(
    val commands: List<ComposePeerTargetCommand>,
) {
    val title: String = "Selected-peer command boundary"
    val enabledCommands: List<ComposePeerTargetCommand> = commands.filter(ComposePeerTargetCommand::enabled)
    val disabledCommands: List<ComposePeerTargetCommand> = commands.filterNot(ComposePeerTargetCommand::enabled)
    val enabledSummary: String = if (enabledCommands.isEmpty()) {
        "No selected-peer commands are ready."
    } else {
        "Ready peer commands: ${enabledCommands.joinToString { it.label }}"
    }
    val disabledSummary: String = if (disabledCommands.isEmpty()) {
        "No selected-peer commands are blocked."
    } else {
        "Blocked peer commands: ${disabledCommands.joinToString { it.label }}"
    }

    fun command(kind: ComposePeerTargetCommandKind): ComposePeerTargetCommand = commands.first { it.kind == kind }

    companion object {
        fun from(
            peer: ComposePeerListItem?,
            targetActions: ComposePeerTargetActions,
            javaFxFallbackAvailable: Boolean,
            selectedTargetKind: ComposePeerTargetCommandKind? = null,
        ): ComposePeerTargetControlPlan {
            val peerName = peer?.nickname ?: "selected peer"
            val fallbackBlock = "JavaFX peer-list fallback is unavailable; keep live Compose peer dispatch disabled."
            fun blocked(defaultReason: String): String = if (!javaFxFallbackAvailable) fallbackBlock else defaultReason
            fun firstBlockedReason(): String = targetActions.blockedReasons.firstOrNull()
                ?: "Select an online peer before dispatching peer-targeted commands."
            fun label(kind: ComposePeerTargetCommandKind, base: String): String =
                if (kind == selectedTargetKind) "$base selected" else base

            return ComposePeerTargetControlPlan(
                listOf(
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.CHAT_TARGET,
                        label = label(ComposePeerTargetCommandKind.CHAT_TARGET, "Use for chat"),
                        enabled = javaFxFallbackAvailable && targetActions.chatReady,
                        summary = "Prepare $peerName as the chat target.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.FILE_TARGET,
                        label = label(ComposePeerTargetCommandKind.FILE_TARGET, "Use for files"),
                        enabled = javaFxFallbackAvailable && targetActions.fileReady,
                        summary = "Prepare $peerName as the encrypted file-transfer target.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.VOICE_TARGET,
                        label = label(ComposePeerTargetCommandKind.VOICE_TARGET, "Use for voice"),
                        enabled = javaFxFallbackAvailable && targetActions.voiceReady,
                        summary = "Prepare $peerName for a voice call.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.VIDEO_TARGET,
                        label = label(ComposePeerTargetCommandKind.VIDEO_TARGET, "Use for video"),
                        enabled = javaFxFallbackAvailable && targetActions.videoReady,
                        summary = "Prepare $peerName for an experimental video call.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                    ComposePeerTargetCommand(
                        kind = ComposePeerTargetCommandKind.DATA_TARGET,
                        label = label(ComposePeerTargetCommandKind.DATA_TARGET, "Use for real-time data"),
                        enabled = javaFxFallbackAvailable && targetActions.dataChannelReady,
                        summary = "Prepare $peerName for sending real-time data.",
                        blockedReason = blocked(firstBlockedReason()),
                    ),
                ),
            )
        }
    }
}
