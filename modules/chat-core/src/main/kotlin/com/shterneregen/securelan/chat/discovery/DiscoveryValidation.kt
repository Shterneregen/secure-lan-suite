package com.shterneregen.securelan.chat.discovery

import java.text.Normalizer

/** Shared bounds for untrusted TXT and connection hints. */
object DiscoveryValidation {
    private val uuidPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    fun uuid(value: String): Boolean = uuidPattern.matches(value)
    fun port(value: String): Int? = value.takeIf { it.length in 1..5 && it[0] in '1'..'9' && it.all { c -> c in '0'..'9' } }
        ?.toIntOrNull()?.takeIf { it in 1..65535 }
    fun displayName(value: String): Boolean = value.isNotBlank() && value.toByteArray(Charsets.UTF_8).size <= 128 &&
        value.codePointCount(0, value.length) <= 64 && Normalizer.isNormalized(value, Normalizer.Form.NFC) &&
        value.codePoints().toArray().none { Character.isISOControl(it) || it in 0x202A..0x202E || it in 0x2066..0x2069 ||
            it == 0x061C || it == 0x200E || it == 0x200F || it in 0xD800..0xDFFF }
}
