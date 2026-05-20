package com.shterneregen.securelan.chat.protocol

import java.util.Objects

class WireMessage(
    type: WireMessageType,
    sender: String?,
    payload: String?,
) {
    val type: WireMessageType = Objects.requireNonNull(type, "type must not be null")!!
    val sender: String = sender ?: ""
    val payload: String = payload ?: ""

    fun type(): WireMessageType = type
    fun sender(): String = sender
    fun payload(): String = payload

    fun serialize(): String = escape(type.name) + SEPARATOR + escape(sender) + SEPARATOR + escape(payload)

    override fun equals(other: Any?): Boolean =
        this === other || (other is WireMessage && type == other.type && sender == other.sender && payload == other.payload)

    override fun hashCode(): Int = Objects.hash(type, sender, payload)

    override fun toString(): String = "WireMessage[type=$type, sender=$sender, payload=$payload]"

    companion object {
        private const val SEPARATOR = '|'
        private const val ESCAPE = '\\'

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
