package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.service.ChatHistoryService
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe, in-memory implementation of [ChatHistoryService] backed by a [CopyOnWriteArrayList].
 *
 * All reads return a consistent snapshot without blocking writers, making this implementation
 * suitable for scenarios where reads are frequent but writes are occasional (e.g. a chat UI that
 * polls history on the main thread while server threads append messages).
 *
 * **Note:** History is kept in memory only and is lost when the server stops.
 */
class InMemoryChatHistoryService : ChatHistoryService {

    private val lines = CopyOnWriteArrayList<String>()

    /**
     * Appends [line] to the end of the history log.
     *
     * @param line The text line to append; must not be `null`.
     */
    override fun append(line: String) {
        lines.add(line)
    }

    /**
     * Returns a point-in-time snapshot of all history lines in insertion order.
     *
     * @return A new, immutable list of history lines.
     */
    override fun getAll(): List<String> = lines.toList()

    /**
     * Returns all history lines joined by the platform-specific line separator.
     *
     * @return The full chat history as a single multi-line string.
     */
    override fun dumpAsText(): String = lines.joinToString(System.lineSeparator())

    /**
     * Removes all history lines.
     */
    override fun clear() {
        lines.clear()
    }
}
