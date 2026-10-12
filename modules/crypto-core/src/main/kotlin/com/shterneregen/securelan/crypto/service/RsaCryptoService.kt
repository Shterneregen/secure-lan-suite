package com.shterneregen.securelan.crypto.service

import java.security.PrivateKey
import java.security.PublicKey

/**
 * Service providing asymmetric RSA encryption and decryption.
 *
 * Uses Optimal Asymmetric Encryption Padding (OAEP) with SHA-256 digest and MGF1 masking
 * for secure asymmetric key wrapping or payload exchange.
 */
interface RsaCryptoService {
    /**
     * Encrypts the specified [plainText] using the recipient's RSA [publicKey].
     *
     * @param plainText The plaintext bytes to encrypt. Due to RSA OAEP padding limits,
     *                  the length of [plainText] must not exceed the maximum OAEP payload size
     *                  for the key size (e.g. 190 bytes for RSA-2048 with SHA-256).
     * @param publicKey The recipient's RSA public key.
     * @return The encrypted ciphertext bytes.
     * @throws IllegalStateException If encryption fails.
     */
    fun encrypt(plainText: ByteArray, publicKey: PublicKey): ByteArray

    /**
     * Decrypts the specified [cipherText] using the recipient's RSA [privateKey].
     *
     * @param cipherText The RSA-OAEP encrypted ciphertext bytes.
     * @param privateKey The recipient's RSA private key.
     * @return The decrypted plaintext bytes.
     * @throws IllegalStateException If decryption fails.
     */
    fun decrypt(cipherText: ByteArray, privateKey: PrivateKey): ByteArray
}
