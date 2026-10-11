package com.shterneregen.securelan.chat.service

/**
 * Service that maintains an ordered in-memory log of chat history lines.
 *
 * Each line is a plain-text string in one of the following formats:
 * - Chat message: `"<nickname>: <text>"`
 * - System event: `"[system] <description>"`
 *
 * Implementations must be thread-safe.
 */
interface ChatHistoryService {

    /**
     * Appends a line to the end of the history log.
     *
     * @param line The text line to append; must not be `null`.
     */
    fun append(line: String)

    /**
     * Returns an immutable snapshot of all history lines in insertion order.
     *
     * @return A new list containing the current history lines.
     */
    fun getAll(): List<String>

    /**
     * Returns all history lines joined by the platform line separator.
     *
     * Equivalent to `getAll().joinToString(System.lineSeparator())`.
     *
     * @return The full chat history as a single multi-line string.
     */
    fun dumpAsText(): String

    /**
     * Clears all history lines.
     *
     * After this call, [getAll] returns an empty list.
     */
    fun clear()
}
