package com.shterneregen.securelan.desktop.compose.state.chat

import java.time.Instant
import java.nio.file.Path

public data class ComposeChatMessage(
    val sender: String,
    val text: String,
    val system: Boolean = false,
    val timestamp: Instant = Instant.now(),
    val transcriptLine: String? = null,
    val eventKey: String? = null,
    val actionPath: Path? = null,
) {
    val displayText: String = transcriptLine ?: if (system) "[$sender] $text" else "$sender: $text"
    val displayTime: String = formatComposeChatTimestamp(timestamp)

    companion object {
        fun fromTranscriptLine(
            line: String,
            timestamp: Instant = Instant.now(),
            eventKey: String? = null,
            actionPath: Path? = null,
        ): ComposeChatMessage = ComposeChatMessage(
            sender = "runtime",
            text = line,
            timestamp = timestamp,
            transcriptLine = line,
            eventKey = eventKey,
            actionPath = actionPath,
        )
    }
}
