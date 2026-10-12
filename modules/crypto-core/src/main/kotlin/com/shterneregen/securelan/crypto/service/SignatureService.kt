package com.shterneregen.securelan.crypto.service

import java.security.PrivateKey
import java.security.PublicKey

/**
 * Service providing digital signature generation and verification.
 */
interface SignatureService {
    /**
     * Generates a digital signature for the given [data] using the signer's [privateKey].
     *
     * @param data The data bytes to sign.
     * @param privateKey The signer's RSA private key.
     * @return The digital signature bytes.
     * @throws IllegalStateException If signing fails.
     */
    fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray

    /**
     * Verifies a digital [signature] against the expected [data] using the signer's [publicKey].
     *
     * @param data The original signed data bytes.
     * @param signature The digital signature bytes to verify.
     * @param publicKey The signer's RSA public key.
     * @return `true` if the signature is valid for the given data and public key, `false` otherwise.
     * @throws IllegalStateException If signature verification encounters a cryptographic error.
     */
    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean
}
