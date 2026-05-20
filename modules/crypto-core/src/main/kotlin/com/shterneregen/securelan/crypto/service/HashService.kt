package com.shterneregen.securelan.crypto.service

interface HashService {
    fun sha256(input: ByteArray): ByteArray

    fun sha256Hex(input: ByteArray): String
}
