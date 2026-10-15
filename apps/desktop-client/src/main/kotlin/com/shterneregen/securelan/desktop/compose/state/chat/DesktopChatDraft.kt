package com.shterneregen.securelan.desktop.compose.state.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shterneregen.securelan.chat.service.ChatSendOutcome

data class ChatDraftRevision(val revision: Long, val text: String)

/** Only the successfully written draft revision may be cleared by an asynchronous completion. */
class DesktopChatDraft {
    var text: String by mutableStateOf("")
        private set
    private var revision = 0L

    fun update(value: String) { text = value; revision++ }
    fun capture(): ChatDraftRevision = ChatDraftRevision(revision, text)
    fun complete(sent: ChatDraftRevision, outcome: ChatSendOutcome) {
        if (outcome == ChatSendOutcome.WRITTEN && revision == sent.revision) update("")
    }
}
