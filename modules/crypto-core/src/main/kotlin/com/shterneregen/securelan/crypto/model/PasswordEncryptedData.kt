package com.shterneregen.securelan.crypto.model

import java.util.Objects

/**
 * Immutable container for data encrypted using password-based encryption (PBE/PBKDF2 with AES-GCM).
 *
 * In this scheme:
 * - A cryptographic salt ([salt]) is used along with a passphrase to derive an AES key via PBKDF2.
 * - An initialization vector ([iv]) is generated for AES-GCM encryption.
 * - The data is encrypted using AES-GCM to produce the [cipherText] (including the authentication tag).
 *
 * Internal byte arrays are defensively cloned upon construction and retrieval to preserve immutability.
 *
 * @param salt The cryptographic salt bytes used during key derivation.
 * @param iv The initialization vector bytes used for AES-GCM.
 * @param cipherText The AES-GCM ciphertext bytes including authentication tag.
 */
class PasswordEncryptedData(
    salt: ByteArray,
    iv: ByteArray,
    cipherText: ByteArray,
) {
    private val salt: ByteArray = Objects.requireNonNull(salt, "salt").clone()
    private val iv: ByteArray = Objects.requireNonNull(iv, "iv").clone()
    private val cipherText: ByteArray = Objects.requireNonNull(cipherText, "cipherText").clone()

    /**
     * Returns a defensive copy of the cryptographic salt bytes.
     *
     * @return A cloned byte array containing the salt.
     */
    fun salt(): ByteArray = salt.clone()

    /**
     * Returns a defensive copy of the AES-GCM initialization vector.
     *
     * @return A cloned byte array containing the IV.
     */
    fun iv(): ByteArray = iv.clone()

    /**
     * Returns a defensive copy of the ciphertext bytes.
     *
     * @return A cloned byte array containing the ciphertext and authentication tag.
     */
    fun cipherText(): ByteArray = cipherText.clone()

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is PasswordEncryptedData &&
                salt.contentEquals(other.salt) &&
                iv.contentEquals(other.iv) &&
                cipherText.contentEquals(other.cipherText))

    override fun hashCode(): Int {
        var result = salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + cipherText.contentHashCode()
        return result
    }

    override fun toString(): String =
        "PasswordEncryptedData[salt=${salt.size} bytes, iv=${iv.size} bytes, cipherText=${cipherText.size} bytes]"
}
