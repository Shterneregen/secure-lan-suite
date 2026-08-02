package com.shterneregen.securelan.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class CryptoServicesTest {
    private val cryptoServices = CryptoServices.createDefault()

    @Test
    fun shouldEncryptAndDecryptWithAesGcm() {
        val payload = "hello aes".toByteArray(StandardCharsets.UTF_8)
        val key = cryptoServices.keyGenerationService().generateAesKey()

        val encrypted = cryptoServices.aesGcmCryptoService().encrypt(payload, key)
        val decrypted = cryptoServices.aesGcmCryptoService().decrypt(encrypted, key)

        assertArrayEquals(payload, decrypted)
    }

    @Test
    fun shouldEncryptAndDecryptWithRsaAndSignatures() {
        val payload = "hello rsa".toByteArray(StandardCharsets.UTF_8)
        val keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()

        val encrypted = cryptoServices.rsaCryptoService().encrypt(payload, keyPair.public)
        val decrypted = cryptoServices.rsaCryptoService().decrypt(encrypted, keyPair.private)
        val signature = cryptoServices.signatureService().sign(payload, keyPair.private)

        assertArrayEquals(payload, decrypted)
        assertTrue(cryptoServices.signatureService().verify(payload, signature, keyPair.public))
    }

    @Test
    fun shouldInteroperateWithExplicitAndroidCompatibleOaepParameters() {
        val payload = "cross-provider rsa".toByteArray(StandardCharsets.UTF_8)
        val keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()
        val parameters = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA1,
            PSource.PSpecified.DEFAULT,
        )

        val externalEncryptor = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        externalEncryptor.init(Cipher.ENCRYPT_MODE, keyPair.public, parameters)
        val externallyEncrypted = externalEncryptor.doFinal(payload)
        assertArrayEquals(payload, cryptoServices.rsaCryptoService().decrypt(externallyEncrypted, keyPair.private))

        val serviceEncrypted = cryptoServices.rsaCryptoService().encrypt(payload, keyPair.public)
        val externalDecryptor = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        externalDecryptor.init(Cipher.DECRYPT_MODE, keyPair.private, parameters)
        assertArrayEquals(payload, externalDecryptor.doFinal(serviceEncrypted))
    }

    @Test
    fun shouldRoundTripHybridAndPasswordFileWorkflows() {
        val payload = "hello file crypto".toByteArray(StandardCharsets.UTF_8)
        val keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()

        val hybridEncryptedData = cryptoServices.hybridFileCryptoWorkflow().encrypt(payload, keyPair.public)
        val hybridDecrypted = cryptoServices.hybridFileCryptoWorkflow().decrypt(hybridEncryptedData, keyPair.private)

        val passwordEncryptedData = cryptoServices.passwordFileCryptoWorkflow().encrypt(payload, "secret".toCharArray())
        val passwordDecrypted = cryptoServices.passwordFileCryptoWorkflow().decrypt(passwordEncryptedData, "secret".toCharArray())

        val tempFile = Files.createTempFile("secure-lan-suite", ".bin")
        try {
            Files.write(tempFile, payload)
            val fileDecrypted = cryptoServices.fileCryptoService().decryptFileWithPrivateKey(
                cryptoServices.fileCryptoService().encryptFileWithPublicKey(tempFile, keyPair.public),
                keyPair.private,
            )
            assertArrayEquals(payload, fileDecrypted)
        } finally {
            Files.deleteIfExists(tempFile)
        }

        assertArrayEquals(payload, hybridDecrypted)
        assertArrayEquals(payload, passwordDecrypted)
    }

    @Test
    fun shouldEncodeAndDecodeRsaKeys() {
        val keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()

        val encodedPublic = cryptoServices.keyEncodingService().encodePublicKey(keyPair.public)
        val encodedPrivate = cryptoServices.keyEncodingService().encodePrivateKey(keyPair.private)

        assertTrue(
            encodedPublic.contentEquals(
                cryptoServices.keyEncodingService().encodePublicKey(
                    cryptoServices.keyEncodingService().decodePublicKey(encodedPublic),
                ),
            ),
        )
        assertTrue(
            encodedPrivate.contentEquals(
                cryptoServices.keyEncodingService().encodePrivateKey(
                    cryptoServices.keyEncodingService().decodePrivateKey(encodedPrivate),
                ),
            ),
        )
    }
}
