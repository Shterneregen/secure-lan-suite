package com.shterneregen.securelan.common.net.address

/**
 * An immutable 48-bit MAC. Parsing is syntactic; [isWakeTarget] applies WoL target policy.
 *
 * @property value Canonical lowercase colon-separated octets, suitable for persistence.
 */
class MacAddress private constructor(val value: String) {
    /** Whether this is a nonzero unicast target; locally administered addresses remain valid. */
    val isWakeTarget: Boolean
        get() = value != "00:00:00:00:00:00" && value.substring(0, 2).toInt(16) and 1 == 0

    /** Returns a new array so callers cannot mutate the value. */
    fun bytes(): ByteArray = value.split(':').map { it.toInt(16).toByte() }.toByteArray()

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is MacAddress && value == other.value

    override fun hashCode(): Int = value.hashCode()

    companion object {
        private val compact = Regex("[0-9a-fA-F]{12}")
        private val colon = Regex("[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}")
        private val hyphen = Regex("[0-9a-fA-F]{2}(-[0-9a-fA-F]{2}){5}")
        private val dotted = Regex("[0-9a-fA-F]{4}(\\.[0-9a-fA-F]{4}){2}")

        /**
         * Accepts colon, hyphen, compact hex and dotted notation; never resolves addresses.
         *
         * @param raw Exact input; whitespace and mixed separators are rejected rather than repaired.
         * @return A syntactically valid value, or null. Callers must separately check [isWakeTarget].
         */
        fun parse(raw: String): MacAddress? {
            // Validate octet widths before byte conversion; truncating values such as hex 100 is unsafe.
            if (raw.length !in 12..17 || !listOf(compact, colon, hyphen, dotted).any { it.matches(raw) }) return null
            val hex = raw.filter { it != ':' && it != '-' && it != '.' }.lowercase()
            return MacAddress(hex.chunked(2).joinToString(":"))
        }

        /** Builds the canonical value from exactly six bytes, or returns null for another length. */
        fun fromBytes(bytes: ByteArray): MacAddress? {
            if (bytes.size != 6) return null
            return parse(bytes.joinToString(":") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') })
        }
    }
}
