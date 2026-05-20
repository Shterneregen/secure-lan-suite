package com.shterneregen.securelan.crypto.workflow.impl

import com.shterneregen.securelan.crypto.model.HybridEncryptedData
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService
import com.shterneregen.securelan.crypto.service.KeyEncodingService
import com.shterneregen.securelan.crypto.service.KeyGenerationService
import com.shterneregen.securelan.crypto.service.RsaCryptoService
import com.shterneregen.securelan.crypto.workflow.HybridFileCryptoWorkflow
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Objects

class DefaultHybridFileCryptoWorkflow(
    private val aesGcmCryptoService: AesGcmCryptoService,
    private val rsaCryptoService: RsaCryptoService,
    private val keyGenerationService: KeyGenerationService,
    private val keyEncodingService: KeyEncodingService,
) : HybridFileCryptoWorkflow {
    init {
        Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService")
        Objects.requireNonNull(rsaCryptoService, "rsaCryptoService")
        Objects.requireNonNull(keyGenerationService, "keyGenerationService")
        Objects.requireNonNull(keyEncodingService, "keyEncodingService")
    }

    override fun encrypt(fileBytes: ByteArray, publicKey: PublicKey): HybridEncryptedData {
        Objects.requireNonNull(fileBytes, "fileBytes")
        Objects.requireNonNull(publicKey, "publicKey")
        val sessionKey = keyGenerationService.generateAesKey()
        val encryptedPayload = aesGcmCryptoService.encrypt(fileBytes, sessionKey)
        val buffer = ByteBuffer.wrap(encryptedPayload)
        val ivLength = buffer.get().toInt() and 0xff
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)
        val encryptedKey = rsaCryptoService.encrypt(keyEncodingService.encodeSecretKey(sessionKey), publicKey)
        return HybridEncryptedData(encryptedKey, iv, cipherText)
    }

    override fun decrypt(encryptedData: HybridEncryptedData, privateKey: PrivateKey): ByteArray {
        Objects.requireNonNull(encryptedData, "encryptedData")
        Objects.requireNonNull(privateKey, "privateKey")
        val encodedAesKey = rsaCryptoService.decrypt(encryptedData.encryptedKey(), privateKey)
        val sessionKey = keyEncodingService.decodeAesKey(encodedAesKey)
        val payload = ByteBuffer.allocate(1 + encryptedData.iv().size + encryptedData.cipherText().size)
            .put(encryptedData.iv().size.toByte())
            .put(encryptedData.iv())
            .put(encryptedData.cipherText())
            .array()
        return aesGcmCryptoService.decrypt(payload, sessionKey)
    }
}
