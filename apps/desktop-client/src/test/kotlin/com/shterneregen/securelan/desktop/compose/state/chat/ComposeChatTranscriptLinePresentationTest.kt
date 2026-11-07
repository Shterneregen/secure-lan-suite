package com.shterneregen.securelan.desktop.compose.state.chat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant

class ComposeChatTranscriptLinePresentationTest {
    @Test
    fun hidesServicePrefixesInLocalAndLegacyEvents() {
        val prefixes = listOf(
            "[system]", "system:", "system: [system]", "[system] system:",
            "[join]", "[left]", "[transfer]", "[call]", "[security]", "[error]",
            "[warning]", "[info]", "[discovery]", "[quick-share]", "[stego]", "[rtc]",
            "[SYSTEM] [INFO]",
        )
        for (prefix in prefixes) {
            val text = "$prefix Disconnected from room"
            val event = LocalTranscriptEvent(ComposeChatTranscriptLineKind.SYSTEM, text)
            assertEquals("Disconnected from room", ComposeChatTranscriptLinePresentation.from(
                ComposeChatMessage("runtime", text, localEvent = event),
            ).body, prefix)
            assertEquals("Disconnected from room", ComposeChatTranscriptLinePresentation.from(text).body, prefix)
        }
        assertEquals("disconnected: Connection lost",
            ComposeChatTranscriptLinePresentation.from("[disconnected] Connection lost").body)
        val disconnected = LocalTranscriptEvent(ComposeChatTranscriptLineKind.SYSTEM, "[disconnected] Connection lost")
        assertEquals("Connection lost", ComposeChatTranscriptLinePresentation.from(
            ComposeChatMessage("runtime", disconnected.body, localEvent = disconnected),
        ).body)
        val sent = LocalTranscriptEvent(ComposeChatTranscriptLineKind.TRANSFER, "[file-send] report.pdf")
        assertEquals("sent: report.pdf", ComposeChatTranscriptLinePresentation.from(
            ComposeChatMessage("runtime", sent.body, localEvent = sent),
        ).body)
    }

    @Test
    fun keepsLocalEventSemanticsAndActionsWhileCleaningDisplayText() {
        val timestamp = Instant.parse("2026-11-07T12:00:00Z")
        val path = Path.of("downloads", "report.pdf")
        val event = LocalTranscriptEvent(
            ComposeChatTranscriptLineKind.TRANSFER, "[file-recv] report.pdf",
            actionUrl = "http://localhost/file", actionPath = path, showsQrCode = true,
        )
        val presentation = ComposeChatTranscriptLinePresentation.from(
            ComposeChatMessage("runtime", event.body, timestamp = timestamp, localEvent = event),
        )
        assertEquals("received: report.pdf", presentation.body)
        assertEquals(event.kind, presentation.kind)
        assertEquals(event.label, presentation.label)
        assertEquals(timestamp, presentation.timestamp)
        assertEquals(event.actionUrl, presentation.actionUrl)
        assertEquals(path, presentation.actionPath)
        assertEquals(true, presentation.showsQrCode)
        assertEquals("[file-recv] report.pdf", event.body)
    }

    @Test
    fun preservesUserTextAndBracketsInsideEvents() {
        for (sender in listOf("bob", "alice")) {
            val text = "[system] [error] system: literal text"
            val message = ComposeChatMessage(sender, text)
            assertEquals(text, ComposeChatTranscriptLinePresentation.from(message, "alice").body)
            assertEquals(text, ComposeChatTranscriptLinePresentation.from("$sender: $text", "alice").body)
        }
        val text = "[system] Saved [report] with [info] inside"
        assertEquals("Saved [report] with [info] inside", ComposeChatTranscriptLinePresentation.from(text).body)
        assertEquals("[report] Saved", ComposeChatTranscriptLinePresentation.from("[system] [report] Saved").body)
    }
}
