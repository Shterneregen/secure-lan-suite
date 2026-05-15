package com.shterneregen.securelan.androidclient.protocol

enum class WireMessageType {
    HELLO,
    SERVER_KEY,
    CLIENT_KEY,
    ACCEPTED,
    REJECTED,
    CHAT,
    SYSTEM,
    USER_JOINED,
    USER_LEFT,
    SIGNAL,
    DISCONNECT,
}

data class WireMessage(
    val type: WireMessageType,
    val sender: String,
    val payload: String,
) {
    fun serialize(): String = listOf(type.name, sender, payload).joinToString(SEPARATOR.toString()) { escape(it) }

    companion object {
        private const val SEPARATOR = '|'
        private const val ESCAPE = '\\'

        fun deserialize(line: String): WireMessage {
            val parts = splitEscapedRaw(line)
            require(parts.size == 3) { "Invalid wire message format" }
            return WireMessage(
                type = WireMessageType.valueOf(unescape(parts[0])),
                sender = unescape(parts[1]),
                payload = unescape(parts[2]),
            )
        }

        private fun splitEscapedRaw(input: String): List<String> {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            var escaping = false
            input.forEach { c ->
                when {
                    escaping -> {
                        current.append(ESCAPE)
                        current.append(c)
                        escaping = false
                    }
                    c == ESCAPE -> escaping = true
                    c == SEPARATOR -> {
                        parts += current.toString()
                        current.setLength(0)
                    }
                    else -> current.append(c)
                }
            }
            if (escaping) current.append(ESCAPE)
            parts += current.toString()
            return parts
        }

        private fun escape(value: String): String = buildString(value.length) {
            value.forEach { c ->
                when (c) {
                    ESCAPE -> append("\\\\")
                    SEPARATOR -> append("\\|")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    else -> append(c)
                }
            }
        }

        private fun unescape(value: String): String = buildString(value.length) {
            var escaping = false
            value.forEach { c ->
                if (escaping) {
                    when (c) {
                        'n' -> append('\n')
                        'r' -> append('\r')
                        '\\' -> append('\\')
                        '|' -> append('|')
                        else -> {
                            append('\\')
                            append(c)
                        }
                    }
                    escaping = false
                } else if (c == ESCAPE) {
                    escaping = true
                } else {
                    append(c)
                }
            }
            if (escaping) append(ESCAPE)
        }
    }
}
