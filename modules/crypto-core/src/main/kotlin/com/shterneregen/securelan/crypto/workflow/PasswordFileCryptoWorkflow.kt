package com.shterneregen.securelan.crypto.workflow

import com.shterneregen.securelan.crypto.model.PasswordEncryptedData

/**
 * Workflow for password-based encryption and decryption of byte buffers.
 *
 * Derives a key using PBKDF2 with HMAC-SHA256 and encrypts the data using AES-GCM.
 */
interface PasswordFileCryptoWorkflow {
    /**
     * Encrypts the provided [fileBytes] using a key derived from [password] and a random salt.
     *
     * @param fileBytes The raw data bytes to encrypt.
     * @param password The character array passphrase.
     * @return The resulting [PasswordEncryptedData] containing the salt, IV, and ciphertext.
     */
    fun encrypt(fileBytes: ByteArray, password: CharArray): PasswordEncryptedData

    /**
     * Decrypts the [encryptedData] using a key derived from [password] and the stored salt.
     *
     * @param encryptedData The password-encrypted container containing the salt, IV, and ciphertext.
     * @param password The character array passphrase.
     * @return The decrypted raw data bytes.
     */
    fun decrypt(encryptedData: PasswordEncryptedData, password: CharArray): ByteArray
}
