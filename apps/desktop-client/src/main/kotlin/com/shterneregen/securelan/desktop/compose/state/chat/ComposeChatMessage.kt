package com.shterneregen.securelan.desktop.compose.state.chat

import java.nio.file.Path
import java.time.Instant

/**
 * Immutable transcript row with a local receive time; it adds no fields to the chat protocol.
 *
 * @property eventKey Optional identity for replacing a transfer's progress row in place.
 * @property actionPath Local received-file path exposed by a completed transfer's open action.
 * @property transcriptLine Preformatted runtime line, taking precedence over sender/text formatting.
 */
public data class ComposeChatMessage(
    val sender: String,
    val text: String,
    val system: Boolean = false,
    val timestamp: Instant = Instant.now(),
    val transcriptLine: String? = null,
    val eventKey: String? = null,
    val actionPath: Path? = null,
    val localEvent: LocalTranscriptEvent? = null,
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
