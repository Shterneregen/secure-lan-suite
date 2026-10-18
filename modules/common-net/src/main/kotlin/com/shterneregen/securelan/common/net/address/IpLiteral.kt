package com.shterneregen.securelan.common.net.address

/** A canonical numeric address. Parsing never invokes a name service. Scope belongs to the receiver. */
class IpLiteral private constructor(val value: String, private val octets: List<Int>) {
    val isIpv4: Boolean get() = octets.size == 4
    val isLinkLocal: Boolean get() = if (isIpv4) octets[0] == 169 && octets[1] == 254 else octets[0] == 254 && octets[1] and 192 == 128
    val isUnicast: Boolean get() = if (isIpv4) {
        octets[0] != 0 && octets[0] != 127 && octets[0] < 224 && octets != listOf(255, 255, 255, 255)
    } else octets.any { it != 0 } && octets != List(15) { 0 } + 1 && octets[0] != 255
    fun bytes(): ByteArray = octets.map(Int::toByte).toByteArray()
    override fun toString(): String = value
    override fun equals(other: Any?): Boolean = other is IpLiteral && value == other.value
    override fun hashCode(): Int = value.hashCode()

    companion object {
        fun parse(raw: String): IpLiteral? {
            if (raw.isEmpty() || raw.length > 45 || raw.any { it !in "0123456789abcdefABCDEF:." }) return null
            if (':' !in raw) return ipv4(raw)?.let { IpLiteral(it.joinToString("."), it) }
            if (raw.count { it == ':' } < 2 || raw.indexOf("::") != raw.lastIndexOf("::")) return null
            val compressed = "::" in raw
            val sides = raw.split("::", limit = 2)
            fun groups(part: String): List<Int>? {
                if (part.isEmpty()) return emptyList()
                val tokens = part.split(':')
                val result = mutableListOf<Int>()
                tokens.forEachIndexed { i, token ->
                    if ('.' in token) {
                        if (i != tokens.lastIndex) return null
                        val v4 = ipv4(token) ?: return null
                        result += (v4[0] shl 8) or v4[1]
                        result += (v4[2] shl 8) or v4[3]
                    } else {
                        if (token.length !in 1..4) return null
                        result += token.toIntOrNull(16) ?: return null
                    }
                }
                return result
            }
            val left = groups(sides[0]) ?: return null
            val right = if (compressed) groups(sides[1]) ?: return null else emptyList()
            if (compressed && left.size + right.size >= 8 || !compressed && left.size != 8) return null
            if ('.' in sides[0] && compressed) return null
            val words = left + List(8 - left.size - right.size) { 0 } + right
            val bytes = words.flatMap { listOf(it shr 8, it and 255) }
            if (words.take(5).all { it == 0 } && words[5] == 65535) {
                return IpLiteral(bytes.takeLast(4).joinToString("."), bytes.takeLast(4))
            }
            var start = -1
            var length = 1
            var i = 0
            while (i < 8) {
                if (words[i] != 0) { i++; continue }
                val begin = i
                while (i < 8 && words[i] == 0) i++
                if (i - begin > length) { start = begin; length = i - begin }
            }
            val canonical = if (start < 0) words.joinToString(":") { it.toString(16) } else {
                words.take(start).joinToString(":") { it.toString(16) } + "::" +
                    words.drop(start + length).joinToString(":") { it.toString(16) }
            }
            return IpLiteral(canonical, bytes)
        }

        private fun ipv4(raw: String): List<Int>? {
            val parts = raw.split('.')
            if (parts.size != 4) return null
            return parts.map {
                if (it.isEmpty() || it.length > 3 || it.length > 1 && it[0] == '0' || it.any { c -> c !in '0'..'9' }) return null
                it.toIntOrNull()?.takeIf { n -> n in 0..255 } ?: return null
            }
        }
    }
}
