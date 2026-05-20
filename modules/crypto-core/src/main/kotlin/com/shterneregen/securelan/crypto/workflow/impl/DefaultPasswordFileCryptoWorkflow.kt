package com.shterneregen.securelan.crypto.workflow.impl

import com.shterneregen.securelan.crypto.model.PasswordEncryptedData
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Objects
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class DefaultPasswordFileCryptoWorkflow : PasswordFileCryptoWorkflow {
    override fun encrypt(fileBytes: ByteArray, password: CharArray): PasswordEncryptedData {
        Objects.requireNonNull(fileBytes, "fileBytes")
        Objects.requireNonNull(password, "password")
        try {
            val salt = ByteArray(SALT_LENGTH)
            val iv = ByteArray(IV_LENGTH)
            SECURE_RANDOM.nextBytes(salt)
            SECURE_RANDOM.nextBytes(iv)
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return PasswordEncryptedData(salt, iv, cipher.doFinal(fileBytes))
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to encrypt file with password", exception)
        }
    }

    override fun decrypt(encryptedData: PasswordEncryptedData, password: CharArray): ByteArray {
        Objects.requireNonNull(encryptedData, "encryptedData")
        Objects.requireNonNull(password, "password")
        try {
            val key = deriveKey(password, encryptedData.salt())
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, encryptedData.iv()))
            return cipher.doFinal(encryptedData.cipherText())
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to decrypt file with password", exception)
        }
    }

    @Throws(GeneralSecurityException::class)
    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val ITERATIONS = 65_536
        private const val KEY_SIZE = 256
        private const val TAG_LENGTH_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val SECURE_RANDOM = SecureRandom()
    }
}
