package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.service.HashService
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Objects

/**
 * Default implementation of [HashService] providing standard SHA-256 computation and hex formatting.
 */
class DefaultHashService : HashService {
    override fun sha256(input: ByteArray): ByteArray {
        Objects.requireNonNull(input, "input")
        try {
            return MessageDigest.getInstance("SHA-256").digest(input)
        } catch (exception: NoSuchAlgorithmException) {
            throw IllegalStateException("SHA-256 is not available", exception)
        }
    }

    override fun sha256Hex(input: ByteArray): String {
        val digest = sha256(input)
        val digits = "0123456789abcdef"
        val hex = CharArray(digest.size * 2)
        digest.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xff
            hex[index * 2] = digits[value ushr 4]
            hex[index * 2 + 1] = digits[value and 0x0f]
        }
        return String(hex)
    }
}
