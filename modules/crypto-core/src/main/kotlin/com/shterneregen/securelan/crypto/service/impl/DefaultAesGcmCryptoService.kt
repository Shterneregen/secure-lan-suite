package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.service.AesGcmCryptoService
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Objects
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DefaultAesGcmCryptoService : AesGcmCryptoService {
    override fun encrypt(plainText: ByteArray, key: SecretKey): ByteArray {
        Objects.requireNonNull(plainText, "plainText")
        Objects.requireNonNull(key, "key")
        try {
            val iv = ByteArray(IV_LENGTH)
            SECURE_RANDOM.nextBytes(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val encrypted = cipher.doFinal(plainText)
            return ByteBuffer.allocate(1 + iv.size + encrypted.size)
                .put(iv.size.toByte())
                .put(iv)
                .put(encrypted)
                .array()
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to encrypt AES-GCM payload", exception)
        }
    }

    override fun decrypt(cipherPayload: ByteArray, key: SecretKey): ByteArray {
        Objects.requireNonNull(cipherPayload, "cipherPayload")
        Objects.requireNonNull(key, "key")
        try {
            val buffer = ByteBuffer.wrap(cipherPayload)
            val ivLength = buffer.get().toInt() and 0xff
            if (ivLength <= 0 || buffer.remaining() <= ivLength) {
                throw IllegalArgumentException("Invalid AES-GCM payload")
            }
            val iv = ByteArray(ivLength)
            buffer.get(iv)
            val encrypted = ByteArray(buffer.remaining())
            buffer.get(encrypted)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return cipher.doFinal(encrypted)
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to decrypt AES-GCM payload", exception)
        }
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private val SECURE_RANDOM = SecureRandom()
    }
}
