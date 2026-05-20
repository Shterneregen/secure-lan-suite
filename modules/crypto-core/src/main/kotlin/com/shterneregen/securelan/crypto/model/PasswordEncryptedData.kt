package com.shterneregen.securelan.crypto.model

import java.util.Objects

class PasswordEncryptedData(
    salt: ByteArray,
    iv: ByteArray,
    cipherText: ByteArray,
) {
    private val salt: ByteArray = Objects.requireNonNull(salt, "salt").clone()
    private val iv: ByteArray = Objects.requireNonNull(iv, "iv").clone()
    private val cipherText: ByteArray = Objects.requireNonNull(cipherText, "cipherText").clone()

    fun salt(): ByteArray = salt.clone()

    fun iv(): ByteArray = iv.clone()

    fun cipherText(): ByteArray = cipherText.clone()

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is PasswordEncryptedData &&
                salt == other.salt &&
                iv == other.iv &&
                cipherText == other.cipherText)

    override fun hashCode(): Int = Objects.hash(salt, iv, cipherText)

    override fun toString(): String =
        "PasswordEncryptedData[salt=${salt.size} bytes, iv=${iv.size} bytes, cipherText=${cipherText.size} bytes]"
}
