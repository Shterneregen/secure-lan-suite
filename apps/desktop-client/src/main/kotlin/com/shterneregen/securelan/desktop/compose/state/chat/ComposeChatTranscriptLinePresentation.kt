package com.shterneregen.securelan.desktop.compose.state.chat

import java.time.Instant

public data class ComposeChatTranscriptLinePresentation(
    val kind: ComposeChatTranscriptLineKind,
    val label: String,
    val body: String,
    val timestamp: Instant = Instant.now(),
) {
    val displayTime: String = formatComposeChatTimestamp(timestamp)

    companion object {
        fun from(line: String, localNickname: String = "", timestamp: Instant = Instant.now()): ComposeChatTranscriptLinePresentation {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()
            val normalizedLocal = localNickname.trim().lowercase()
            val localPrefix = localNickname.trim()
            val messageMatch = Regex("^([^:\\[][^:]*):\\s*(.*)$").matchEntire(trimmed)
            val messageSender = messageMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val messageBody = messageMatch?.groupValues?.getOrNull(2)?.trim().orEmpty()
            val presencePattern = Regex("(?:^|\\s)(joined|left)\\s+the\\s+chat[\\s.!?]*$", RegexOption.IGNORE_CASE)
            val kind = when {
                lower.startsWith("[call]") -> ComposeChatTranscriptLineKind.CALL
                lower.startsWith("[security]") -> ComposeChatTranscriptLineKind.SECURITY
                lower.startsWith("[file-send]") || lower.startsWith("[file-recv]") ||
                    lower.startsWith("[transfer]") -> ComposeChatTranscriptLineKind.TRANSFER
                lower.startsWith("[error]") || lower.startsWith("[warning]") ->
                    ComposeChatTranscriptLineKind.SECURITY
                lower.startsWith("[disconnected]") || lower.startsWith("[system]") ||
                    lower.startsWith("system:") -> ComposeChatTranscriptLineKind.SYSTEM
                lower.startsWith("[join]") || lower.startsWith("[left]") || presencePattern.containsMatchIn(trimmed) ->
                    ComposeChatTranscriptLineKind.PRESENCE
                lower.startsWith("[info]") || lower.startsWith("[discovery]") || lower.startsWith("[quick-share]") ||
                    lower.startsWith("[stego]") || lower.startsWith("[rtc]") -> ComposeChatTranscriptLineKind.DIAGNOSTIC
                lower.contains(" failed") || lower.contains(" error") ->
                    ComposeChatTranscriptLineKind.SECURITY
                normalizedLocal.isNotEmpty() && lower.startsWith("$normalizedLocal:") -> ComposeChatTranscriptLineKind.USER_LOCAL
                normalizedLocal.isNotEmpty() && lower.startsWith("$normalizedLocal ->") -> ComposeChatTranscriptLineKind.USER_LOCAL
                messageMatch != null -> ComposeChatTranscriptLineKind.USER_REMOTE
                else -> ComposeChatTranscriptLineKind.SYSTEM
            }
            val label = when (kind) {
                ComposeChatTranscriptLineKind.USER_LOCAL -> "You"
                ComposeChatTranscriptLineKind.USER_REMOTE -> messageSender.ifBlank { "Message" }
                ComposeChatTranscriptLineKind.SYSTEM -> "System"
                ComposeChatTranscriptLineKind.PRESENCE -> "Presence"
                ComposeChatTranscriptLineKind.TRANSFER -> "File"
                ComposeChatTranscriptLineKind.SECURITY -> "Security"
                ComposeChatTranscriptLineKind.DIAGNOSTIC -> "Info"
                ComposeChatTranscriptLineKind.CALL -> "Call"
            }
            val body = when (kind) {
                ComposeChatTranscriptLineKind.USER_LOCAL -> trimmed
                    .removePrefix("$localPrefix:")
                    .removePrefix("$localPrefix ->")
                    .trim()
                ComposeChatTranscriptLineKind.USER_REMOTE -> messageBody.ifBlank { trimmed }
                ComposeChatTranscriptLineKind.SYSTEM -> trimmed
                    .replace(Regex("^system:\\s*\\[system]\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^\\[system]\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^\\[disconnected]\\s*", RegexOption.IGNORE_CASE), "disconnected: ")
                    .trim()
                ComposeChatTranscriptLineKind.PRESENCE -> trimmed
                    .replace(Regex("^system:\\s*\\[system]\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^\\[system]\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^\\[join]\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^\\[left]\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()
                ComposeChatTranscriptLineKind.TRANSFER -> trimmed
                    .replace(Regex("^\\[file-send]\\s*", RegexOption.IGNORE_CASE), "sent: ")
                    .replace(Regex("^\\[file-recv]\\s*", RegexOption.IGNORE_CASE), "received: ")
                    .replace(Regex("^\\[transfer]\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()
                ComposeChatTranscriptLineKind.SECURITY -> trimmed
                    .replace(Regex("^\\[error]\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^\\[warning]\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()
                ComposeChatTranscriptLineKind.DIAGNOSTIC -> trimmed
                ComposeChatTranscriptLineKind.CALL -> trimmed
                    .replace(Regex("^\\[call]\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()
            }
            return ComposeChatTranscriptLinePresentation(kind, label, body, timestamp)
        }
    }
}
