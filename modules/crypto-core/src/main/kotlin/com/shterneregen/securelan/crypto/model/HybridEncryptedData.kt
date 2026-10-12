package com.shterneregen.securelan.crypto.model

import java.util.Objects

/**
 * Immutable container for data encrypted using a hybrid encryption scheme.
 *
 * In this scheme:
 * - A symmetric AES key is generated for the session or payload and encrypted with the recipient's RSA public key ([encryptedKey]).
 * - An initialization vector ([iv]) is generated for the AES-GCM cipher.
 * - The actual payload is encrypted with AES-GCM producing the [cipherText] (including the authentication tag).
 *
 * Internal byte arrays are defensively cloned upon construction and retrieval to preserve immutability.
 *
 * @param encryptedKey The RSA-encrypted symmetric key bytes.
 * @param iv The initialization vector bytes used for AES-GCM.
 * @param cipherText The AES-GCM ciphertext bytes including authentication tag.
 */
class HybridEncryptedData(
    encryptedKey: ByteArray,
    iv: ByteArray,
    cipherText: ByteArray,
) {
    private val encryptedKey: ByteArray = Objects.requireNonNull(encryptedKey, "encryptedKey").clone()
    private val iv: ByteArray = Objects.requireNonNull(iv, "iv").clone()
    private val cipherText: ByteArray = Objects.requireNonNull(cipherText, "cipherText").clone()

    /**
     * Returns a defensive copy of the RSA-encrypted symmetric key bytes.
     *
     * @return A cloned byte array containing the encrypted symmetric key.
     */
    fun encryptedKey(): ByteArray = encryptedKey.clone()

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
            (other is HybridEncryptedData &&
                encryptedKey.contentEquals(other.encryptedKey) &&
                iv.contentEquals(other.iv) &&
                cipherText.contentEquals(other.cipherText))

    override fun hashCode(): Int {
        var result = encryptedKey.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + cipherText.contentHashCode()
        return result
    }

    override fun toString(): String =
        "HybridEncryptedData[encryptedKey=${encryptedKey.size} bytes, iv=${iv.size} bytes, cipherText=${cipherText.size} bytes]"
}
