package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.service.KeyEncodingService
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Objects
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class DefaultKeyEncodingService : KeyEncodingService {
    override fun encodePublicKey(publicKey: PublicKey): ByteArray {
        Objects.requireNonNull(publicKey, "publicKey")
        return publicKey.encoded.clone()
    }

    override fun encodePrivateKey(privateKey: PrivateKey): ByteArray {
        Objects.requireNonNull(privateKey, "privateKey")
        return privateKey.encoded.clone()
    }

    override fun decodePublicKey(encoded: ByteArray): PublicKey {
        Objects.requireNonNull(encoded, "encoded")
        try {
            return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded))
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to decode public key", exception)
        }
    }

    override fun decodePrivateKey(encoded: ByteArray): PrivateKey {
        Objects.requireNonNull(encoded, "encoded")
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(encoded))
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to decode private key", exception)
        }
    }

    override fun encodeSecretKey(secretKey: SecretKey): ByteArray {
        Objects.requireNonNull(secretKey, "secretKey")
        return secretKey.encoded.clone()
    }

    override fun decodeAesKey(encoded: ByteArray): SecretKey {
        Objects.requireNonNull(encoded, "encoded")
        return SecretKeySpec(encoded.clone(), "AES")
    }
}
