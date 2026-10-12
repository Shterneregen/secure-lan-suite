package com.shterneregen.securelan.crypto.service

/**
 * Service for computing cryptographic hash digests.
 */
interface HashService {
    /**
     * Computes the SHA-256 digest of the specified [input] byte array.
     *
     * @param input The input data to hash.
     * @return The 32-byte SHA-256 digest.
     * @throws IllegalStateException If the SHA-256 algorithm is not available.
     */
    fun sha256(input: ByteArray): ByteArray

    /**
     * Computes the SHA-256 digest of the specified [input] byte array and returns it as a lowercase hex string.
     *
     * @param input The input data to hash.
     * @return The 64-character hexadecimal representation of the SHA-256 digest.
     * @throws IllegalStateException If the SHA-256 algorithm is not available.
     */
    fun sha256Hex(input: ByteArray): String
}
