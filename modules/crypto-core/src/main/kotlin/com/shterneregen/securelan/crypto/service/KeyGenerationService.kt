package com.shterneregen.securelan.crypto.service

import java.security.KeyPair
import javax.crypto.SecretKey

/**
 * Service for generating cryptographic keys and key pairs.
 */
interface KeyGenerationService {
    /**
     * Generates a new 256-bit symmetric AES key.
     *
     * @return A newly generated [SecretKey].
     * @throws IllegalStateException If AES key generation fails.
     */
    fun generateAesKey(): SecretKey

    /**
     * Generates a new 2048-bit asymmetric RSA key pair.
     *
     * @return A newly generated [KeyPair] containing the public and private keys.
     * @throws IllegalStateException If RSA key pair generation fails.
     */
    fun generateRsaKeyPair(): KeyPair
}
