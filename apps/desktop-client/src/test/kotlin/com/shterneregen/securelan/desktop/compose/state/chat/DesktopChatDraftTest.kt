package com.shterneregen.securelan.desktop.compose.state.chat

import com.shterneregen.securelan.chat.service.ChatSendOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DesktopChatDraftTest {
    @Test fun failureRetainsTextAndSuccessClearsOnlyTheCapturedRevision() {
        val draft = DesktopChatDraft()
        draft.update("keep me")
        val first = draft.capture()
        draft.complete(first, ChatSendOutcome.FAILED)
        assertEquals("keep me", draft.text)
        draft.update("new text")
        draft.complete(first, ChatSendOutcome.WRITTEN)
        assertEquals("new text", draft.text)
        val second = draft.capture()
        // Even editing back to identical text creates a new revision.
        draft.update("new text")
        draft.complete(second, ChatSendOutcome.WRITTEN)
        assertEquals("new text", draft.text)
        draft.complete(draft.capture(), ChatSendOutcome.WRITTEN)
        assertEquals("", draft.text)
    }

    @Test fun localEventActionsAndSeverityDoNotDependOnWording() {
        val event = LocalTranscriptEvent(ComposeChatTranscriptLineKind.QUICK_SHARE, "Любой перевод failed",
            actionUrl = "http://localhost/s/test", showsQrCode = true)
        val presentation = ComposeChatTranscriptLinePresentation.from(ComposeChatMessage("runtime", event.body, localEvent = event))
        assertEquals(ComposeChatTranscriptLineKind.QUICK_SHARE, presentation.kind)
        assertEquals(event.actionUrl, presentation.actionUrl)
        assertTrue(presentation.showsQrCode)
        val user = ComposeChatTranscriptLinePresentation.from(ComposeChatMessage("bob", "[system] Quick Share http://localhost"))
        assertEquals(ComposeChatTranscriptLineKind.USER_REMOTE, user.kind)
        assertNull(user.actionUrl)
    }
}
