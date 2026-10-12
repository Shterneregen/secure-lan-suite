package com.shterneregen.securelan.stego.model

/**
 * Identifies the type of content embedded within a steganography carrier.
 *
 * @param codeValue The 1-byte protocol identifier stored in the steganography header.
 */
enum class StegoContentType(private val codeValue: Byte) {
    /** Raw binary data payload. */
    BINARY(1),

    /** UTF-8 encoded text message. */
    UTF8_TEXT(2);

    /**
     * Returns the 1-byte protocol representation of this content type.
     *
     * @return The protocol byte code.
     */
    fun code(): Byte = codeValue

    companion object {
        /**
         * Resolves a [StegoContentType] from its protocol byte code.
         *
         * @param code The protocol byte identifier.
         * @return The matching [StegoContentType].
         * @throws IllegalArgumentException If [code] is not recognized.
         */
        @JvmStatic
        fun fromCode(code: Byte): StegoContentType = entries
            .firstOrNull { it.codeValue == code }
            ?: throw IllegalArgumentException("Unsupported steganography content type: $code")
    }
}
