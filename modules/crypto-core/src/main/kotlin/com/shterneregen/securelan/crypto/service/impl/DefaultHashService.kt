package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.service.HashService
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.HexFormat
import java.util.Objects

class DefaultHashService : HashService {
    override fun sha256(input: ByteArray): ByteArray {
        Objects.requireNonNull(input, "input")
        try {
            return MessageDigest.getInstance("SHA-256").digest(input)
        } catch (exception: NoSuchAlgorithmException) {
            throw IllegalStateException("SHA-256 is not available", exception)
        }
    }

    override fun sha256Hex(input: ByteArray): String = HexFormat.of().formatHex(sha256(input))
}
