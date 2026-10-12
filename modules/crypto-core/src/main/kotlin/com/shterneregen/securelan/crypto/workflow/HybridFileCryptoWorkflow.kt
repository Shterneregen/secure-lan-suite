package com.shterneregen.securelan.crypto.workflow

import com.shterneregen.securelan.crypto.model.HybridEncryptedData
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Workflow combining symmetric AES-GCM and asymmetric RSA encryption to securely encrypt and decrypt byte buffers.
 */
interface HybridFileCryptoWorkflow {
    /**
     * Encrypts the provided [fileBytes] using a generated ephemeral AES session key,
     * which is then encrypted with the recipient's RSA [publicKey].
     *
     * @param fileBytes The raw file or data bytes to encrypt.
     * @param publicKey The recipient's RSA public key.
     * @return The resulting [HybridEncryptedData] containing the encrypted session key, IV, and ciphertext.
     */
    fun encrypt(fileBytes: ByteArray, publicKey: PublicKey): HybridEncryptedData

    /**
     * Decrypts the [encryptedData] using the recipient's RSA [privateKey] to unwrap the AES session key,
     * then decrypts the ciphertext payload using AES-GCM.
     *
     * @param encryptedData The hybrid encrypted container.
     * @param privateKey The recipient's RSA private key.
     * @return The decrypted raw file bytes.
     */
    fun decrypt(encryptedData: HybridEncryptedData, privateKey: PrivateKey): ByteArray
}
