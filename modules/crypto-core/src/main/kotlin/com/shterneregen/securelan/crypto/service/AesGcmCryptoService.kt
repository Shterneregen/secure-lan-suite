package com.shterneregen.securelan.crypto.service

import javax.crypto.SecretKey

/**
 * Service providing authenticated symmetric encryption and decryption using AES in Galois/Counter Mode (GCM).
 *
 * AES-GCM provides both confidentiality and data integrity (authentication tag).
 * Payloads produced by this service encapsulate the initialization vector (IV) alongside the ciphertext.
 */
interface AesGcmCryptoService {
    /**
     * Encrypts the provided [plainText] using AES-GCM with the specified symmetric [key].
     *
     * A cryptographically secure random IV is generated for each encryption call.
     * The returned byte array has the format:
     * `[1-byte IV length] + [IV bytes] + [ciphertext + 128-bit authentication tag]`.
     *
     * @param plainText The plaintext bytes to encrypt.
     * @param key The AES secret key.
     * @return The encrypted payload containing the IV prefix and ciphertext.
     * @throws IllegalStateException If cipher initialization or encryption fails.
     */
    fun encrypt(plainText: ByteArray, key: SecretKey): ByteArray

    /**
     * Decrypts the provided [cipherPayload] using AES-GCM with the specified symmetric [key].
     *
     * The payload is expected to conform to the format produced by [encrypt]:
     * `[1-byte IV length] + [IV bytes] + [ciphertext + authentication tag]`.
     *
     * @param cipherPayload The payload bytes containing the IV prefix and ciphertext.
     * @param key The AES secret key.
     * @return The decrypted plaintext bytes.
     * @throws IllegalArgumentException If the payload format is invalid or corrupted.
     * @throws IllegalStateException If decryption or authentication verification fails.
     */
    fun decrypt(cipherPayload: ByteArray, key: SecretKey): ByteArray
}
