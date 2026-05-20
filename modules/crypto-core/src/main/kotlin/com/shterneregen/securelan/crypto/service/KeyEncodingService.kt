package com.shterneregen.securelan.crypto.service

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

interface KeyEncodingService {
    fun encodePublicKey(publicKey: PublicKey): ByteArray

    fun encodePrivateKey(privateKey: PrivateKey): ByteArray
    fun decodePublicKey(encoded: ByteArray): PublicKey
    fun decodePrivateKey(encoded: ByteArray): PrivateKey
    fun encodeSecretKey(secretKey: SecretKey): ByteArray
    fun decodeAesKey(encoded: ByteArray): SecretKey
}
