package com.shterneregen.securelan.crypto.service

import com.shterneregen.securelan.crypto.model.HybridEncryptedData
import com.shterneregen.securelan.crypto.model.PasswordEncryptedData
import java.nio.file.Path
import java.security.PrivateKey
import java.security.PublicKey

/**
 * High-level file encryption and decryption service.
 *
 * Supports hybrid encryption (using RSA public keys) and password-based encryption (PBKDF2/AES-GCM).
 */
interface FileCryptoService {
    /**
     * Reads the file at [path] and encrypts its contents using hybrid encryption with the recipient's [publicKey].
     *
     * @param path The file path to read and encrypt.
     * @param publicKey The recipient's RSA public key.
     * @return The resulting [HybridEncryptedData].
     * @throws IllegalStateException If file reading or encryption fails.
     */
    fun encryptFileWithPublicKey(path: Path, publicKey: PublicKey): HybridEncryptedData

    /**
     * Decrypts [encryptedData] using the recipient's RSA [privateKey].
     *
     * @param encryptedData The hybrid encrypted container.
     * @param privateKey The recipient's RSA private key.
     * @return The decrypted file bytes.
     * @throws IllegalStateException If decryption fails.
     */
    fun decryptFileWithPrivateKey(encryptedData: HybridEncryptedData, privateKey: PrivateKey): ByteArray

    /**
     * Reads the file at [path] and encrypts its contents using password-based encryption with [password].
     *
     * @param path The file path to read and encrypt.
     * @param password The passphrase character array.
     * @return The resulting [PasswordEncryptedData].
     * @throws IllegalStateException If file reading or encryption fails.
     */
    fun encryptFileWithPassword(path: Path, password: CharArray): PasswordEncryptedData

    /**
     * Decrypts [encryptedData] using password-based encryption with [password].
     *
     * @param encryptedData The password-encrypted container.
     * @param password The passphrase character array.
     * @return The decrypted file bytes.
     * @throws IllegalStateException If decryption fails.
     */
    fun decryptFileWithPassword(encryptedData: PasswordEncryptedData, password: CharArray): ByteArray
}
