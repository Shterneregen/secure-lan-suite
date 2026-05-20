package com.shterneregen.securelan.stego.model

enum class StegoContentType(private val codeValue: Byte) {
    BINARY(1),
    UTF8_TEXT(2);

    fun code(): Byte = codeValue

    companion object {
        @JvmStatic
        fun fromCode(code: Byte): StegoContentType = entries
            .firstOrNull { it.codeValue == code }
            ?: throw IllegalArgumentException("Unsupported steganography content type: $code")
    }
}
