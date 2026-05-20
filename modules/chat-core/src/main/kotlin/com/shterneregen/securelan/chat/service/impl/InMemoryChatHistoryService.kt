package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.service.ChatHistoryService
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryChatHistoryService : ChatHistoryService {
    private val lines = CopyOnWriteArrayList<String>()

    override fun append(line: String) {
        lines.add(line)
    }

    override fun getAll(): List<String> = lines.toList()

    override fun dumpAsText(): String = lines.joinToString(System.lineSeparator())

    override fun clear() {
        lines.clear()
    }
}
