package com.shterneregen.securelan.androidclient.protocol

import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoCompat {
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private val random = SecureRandom()

    fun generateAesKey(): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        return generator.generateKey()
    }

    fun encodeSecretKey(key: SecretKey): ByteArray = key.encoded

    fun decodeSecretKey(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")

    fun decodePublicKey(bytes: ByteArray): PublicKey = KeyFactory.getInstance("RSA")
        .generatePublic(X509EncodedKeySpec(bytes))

    fun generateRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }

    fun encodePublicKey(publicKey: PublicKey): ByteArray = publicKey.encoded

    fun rsaEncrypt(plainText: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, RSA_OAEP_SHA256_WITH_MGF1_SHA1)
        return cipher.doFinal(plainText)
    }

    fun rsaDecrypt(cipherText: ByteArray, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey, RSA_OAEP_SHA256_WITH_MGF1_SHA1)
        return cipher.doFinal(cipherText)
    }

    fun aesEncrypt(plainText: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainText)
        return ByteBuffer.allocate(1 + iv.size + encrypted.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
    }

    fun aesDecrypt(payload: ByteArray, key: SecretKey): ByteArray {
        val buffer = ByteBuffer.wrap(payload)
        val ivLength = buffer.get().toInt() and 0xff
        require(ivLength > 0 && buffer.remaining() > ivLength) { "Invalid AES-GCM payload" }
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private val RSA_OAEP_SHA256_WITH_MGF1_SHA1 = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA1,
        PSource.PSpecified.DEFAULT,
    )
}
