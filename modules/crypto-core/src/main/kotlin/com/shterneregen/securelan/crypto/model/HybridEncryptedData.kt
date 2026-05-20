package com.shterneregen.securelan.crypto.model

import java.util.Objects

class HybridEncryptedData(
    encryptedKey: ByteArray,
    iv: ByteArray,
    cipherText: ByteArray,
) {
    private val encryptedKey: ByteArray = Objects.requireNonNull(encryptedKey, "encryptedKey").clone()
    private val iv: ByteArray = Objects.requireNonNull(iv, "iv").clone()
    private val cipherText: ByteArray = Objects.requireNonNull(cipherText, "cipherText").clone()

    fun encryptedKey(): ByteArray = encryptedKey.clone()

    fun iv(): ByteArray = iv.clone()

    fun cipherText(): ByteArray = cipherText.clone()

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is HybridEncryptedData &&
                encryptedKey == other.encryptedKey &&
                iv == other.iv &&
                cipherText == other.cipherText)

    override fun hashCode(): Int = Objects.hash(encryptedKey, iv, cipherText)

    override fun toString(): String =
        "HybridEncryptedData[encryptedKey=${encryptedKey.size} bytes, iv=${iv.size} bytes, cipherText=${cipherText.size} bytes]"
}
