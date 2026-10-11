package com.shterneregen.securelan.chat.protocol

import java.util.Objects

/**
 * An immutable, frame-level message exchanged between chat peers over a TCP socket.
 *
 * A [WireMessage] consists of three fields:
 * - [type]    — the message category (see [WireMessageType]).
 * - [sender]  — the nickname of the sending party (empty string when not applicable).
 * - [payload] — message-specific content; interpretation depends on [type].
 *
 * ## Wire format
 * Messages are serialized to a single UTF-8 text line by [serialize] and deserialized
 * by [deserialize].  The format is:
 * ```
 * <escaped-type>|<escaped-sender>|<escaped-payload>
 * ```
 * The pipe character (`|`), backslash (`\`), carriage return (`\r`), and newline (`\n`)
 * are escaped inside each field so that a complete message always occupies exactly one line.
 *
 * @property type    The [WireMessageType] that categorizes this message.
 * @property sender  The sender's nickname; never `null` (defaults to an empty string).
 * @property payload The message payload; never `null` (defaults to an empty string).
 */
class WireMessage(
    type: WireMessageType,
    sender: String?,
    payload: String?,
) {
    /** The [WireMessageType] that categorizes this message. */
    val type: WireMessageType = Objects.requireNonNull(type, "type must not be null")!!

    /** The sender's nickname. Never `null`; an empty string is used when no sender is set. */
    val sender: String = sender ?: ""

    /** The message payload. Never `null`; an empty string is used when no payload is set. */
    val payload: String = payload ?: ""

    // Java-friendly accessor aliases kept for binary compatibility with Java callers.
    /** Returns [type]. Provided for Java interoperability; prefer the property in Kotlin. */
    fun type(): WireMessageType = type

    /** Returns [sender]. Provided for Java interoperability; prefer the property in Kotlin. */
    fun sender(): String = sender

    /** Returns [payload]. Provided for Java interoperability; prefer the property in Kotlin. */
    fun payload(): String = payload

    /**
     * Serializes this message to a single-line wire string.
     *
     * Special characters inside each field are escaped so that the result contains no raw
     * newline or pipe characters.
     *
     * @return A non-null, single-line string representation of this message.
     */
    fun serialize(): String = escape(type.name) + SEPARATOR + escape(sender) + SEPARATOR + escape(payload)

    override fun equals(other: Any?): Boolean =
        this === other || (other is WireMessage && type == other.type && sender == other.sender && payload == other.payload)

    override fun hashCode(): Int = Objects.hash(type, sender, payload)

    override fun toString(): String = "WireMessage[type=$type, sender=$sender, payload=$payload]"

    companion object {
        private const val SEPARATOR = '|'
        private const val ESCAPE = '\\'

        /**
         * Deserializes a wire-format string produced by [serialize] back into a [WireMessage].
         *
         * @param line The raw line read from the socket (must contain exactly three pipe-separated fields).
         * @return The deserialized [WireMessage].
         * @throws IllegalArgumentException If [line] does not contain exactly three fields or the
         *                                  type token is not a valid [WireMessageType] name.
         */
        @JvmStatic
        fun deserialize(line: String): WireMessage {
            Objects.requireNonNull(line, "line must not be null")
            val parts = splitEscaped(line, SEPARATOR)
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid wire message format: expected 3 parts, got ${parts.size}")
            }
            return WireMessage(
                WireMessageType.valueOf(unescape(parts[0])),
                unescape(parts[1]),
                unescape(parts[2]),
            )
        }

        /**
         * Splits [input] on unescaped occurrences of [separator], respecting the escape character.
         *
         * An escape sequence `\<char>` is kept verbatim (both the backslash and the following
         * character) in the current part so that [unescape] can interpret it later.
         */
        private fun splitEscaped(input: String, separator: Char): List<String> {
            val parts = ArrayList<String>()
            val current = StringBuilder()
            var escaping = false
            for (c in input) {
                if (escaping) {
                    current.append(ESCAPE)
                    current.append(c)
                    escaping = false
                    continue
                }
                if (c == ESCAPE) {
                    escaping = true
                    continue
                }
                if (c == separator) {
                    parts.add(current.toString())
                    current.setLength(0)
                    continue
                }
                current.append(c)
            }
            if (escaping) {
                current.append(ESCAPE)
            }
            parts.add(current.toString())
            return parts
        }

        /**
         * Escapes special characters in [value] so that it can be safely embedded in a wire line.
         *
         * The following characters are escaped:
         * - `\` → `\\`
         * - `|` → `\|`
         * - `\n` → `\n` (literal two-char sequence)
         * - `\r` → `\r` (literal two-char sequence)
         */
        private fun escape(value: String): String {
            val out = StringBuilder(value.length)
            for (c in value) {
                when (c) {
                    ESCAPE -> out.append("\\\\")
                    SEPARATOR -> out.append("\\|")
                    '\n' -> out.append("\\n")
                    '\r' -> out.append("\\r")
                    else -> out.append(c)
                }
            }
            return out.toString()
        }

        /**
         * Reverses the escaping applied by [escape], restoring the original field value.
         */
        private fun unescape(value: String): String {
            val out = StringBuilder(value.length)
            var escaping = false
            for (c in value) {
                if (escaping) {
                    when (c) {
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        '\\' -> out.append('\\')
                        '|' -> out.append('|')
                        else -> {
                            out.append('\\')
                            out.append(c)
                        }
                    }
                    escaping = false
                    continue
                }
                if (c == ESCAPE) {
                    escaping = true
                } else {
                    out.append(c)
                }
            }
            if (escaping) {
                out.append(ESCAPE)
            }
            return out.toString()
        }
    }
}
