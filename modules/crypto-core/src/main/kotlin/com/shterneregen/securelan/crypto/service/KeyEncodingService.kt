package com.shterneregen.securelan.crypto.service

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

/**
 * Service for encoding cryptographic keys to portable byte array representations
 * and decoding them back into key objects.
 */
interface KeyEncodingService {
    /**
     * Encodes the specified [publicKey] to its standard X.509 format bytes.
     *
     * @param publicKey The public key to encode.
     * @return The X.509 encoded byte array.
     */
    fun encodePublicKey(publicKey: PublicKey): ByteArray

    /**
     * Encodes the specified [privateKey] to its standard PKCS#8 format bytes.
     *
     * @param privateKey The private key to encode.
     * @return The PKCS#8 encoded byte array.
     */
    fun encodePrivateKey(privateKey: PrivateKey): ByteArray

    /**
     * Decodes an X.509 encoded byte array into an RSA [PublicKey].
     *
     * @param encoded The X.509 encoded public key bytes.
     * @return The reconstructed [PublicKey].
     * @throws IllegalStateException If decoding fails.
     */
    fun decodePublicKey(encoded: ByteArray): PublicKey

    /**
     * Decodes a PKCS#8 encoded byte array into an RSA [PrivateKey].
     *
     * @param encoded The PKCS#8 encoded private key bytes.
     * @return The reconstructed [PrivateKey].
     * @throws IllegalStateException If decoding fails.
     */
    fun decodePrivateKey(encoded: ByteArray): PrivateKey

    /**
     * Encodes the specified symmetric [secretKey] to its raw byte format.
     *
     * @param secretKey The secret key to encode.
     * @return The raw key byte array.
     */
    fun encodeSecretKey(secretKey: SecretKey): ByteArray

    /**
     * Decodes a raw AES key byte array into a [SecretKey].
     *
     * @param encoded The raw key bytes.
     * @return The reconstructed AES [SecretKey].
     */
    fun decodeAesKey(encoded: ByteArray): SecretKey
}
