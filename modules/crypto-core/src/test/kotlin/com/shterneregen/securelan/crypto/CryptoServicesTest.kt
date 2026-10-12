package com.shterneregen.securelan.crypto

import com.shterneregen.securelan.crypto.model.HybridEncryptedData
import com.shterneregen.securelan.crypto.model.PasswordEncryptedData
import com.shterneregen.securelan.crypto.util.CryptoExceptions
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.cert.CertificateFactory
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
    fun shouldFailAesGcmDecryptionOnInvalidOrEmptyPayload() {
        val key = cryptoServices.keyGenerationService().generateAesKey()

        assertThrows(IllegalArgumentException::class.java) {
            cryptoServices.aesGcmCryptoService().decrypt(ByteArray(0), key)
        }
        assertThrows(IllegalArgumentException::class.java) {
            cryptoServices.aesGcmCryptoService().decrypt(byteArrayOf(12), key)
        }
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
    fun shouldFailSignatureVerificationOnTamperedDataOrSignature() {
        val payload = "hello rsa signature".toByteArray(StandardCharsets.UTF_8)
        val keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()
        val signature = cryptoServices.signatureService().sign(payload, keyPair.private)

        val tamperedPayload = "tampered payload".toByteArray(StandardCharsets.UTF_8)
        assertFalse(cryptoServices.signatureService().verify(tamperedPayload, signature, keyPair.public))

        val tamperedSignature = signature.clone()
        tamperedSignature[0] = (tamperedSignature[0].toInt() xor 0xFF).toByte()
        assertFalse(cryptoServices.signatureService().verify(payload, tamperedSignature, keyPair.public))
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

    @Test
    fun shouldCalculateSha256AndHex() {
        val data = "hello world".toByteArray(StandardCharsets.UTF_8)
        val hash = cryptoServices.hashService().sha256(data)
        val hex = cryptoServices.hashService().sha256Hex(data)

        assertEquals(32, hash.size)
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hex)
    }

    @Test
    fun shouldSupportHybridEncryptedDataEqualsAndHashCode() {
        val key1 = byteArrayOf(1, 2, 3)
        val iv1 = byteArrayOf(4, 5, 6)
        val cipher1 = byteArrayOf(7, 8, 9)

        val first = HybridEncryptedData(key1, iv1, cipher1)
        val second = HybridEncryptedData(key1.clone(), iv1.clone(), cipher1.clone())
        val different = HybridEncryptedData(byteArrayOf(9, 9, 9), iv1, cipher1)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, different)
        assertTrue(first.toString().contains("encryptedKey=3 bytes"))

        // Verify defensive cloning
        key1[0] = 99
        assertEquals(1, first.encryptedKey()[0])
    }

    @Test
    fun shouldSupportPasswordEncryptedDataEqualsAndHashCode() {
        val salt1 = byteArrayOf(1, 2, 3)
        val iv1 = byteArrayOf(4, 5, 6)
        val cipher1 = byteArrayOf(7, 8, 9)

        val first = PasswordEncryptedData(salt1, iv1, cipher1)
        val second = PasswordEncryptedData(salt1.clone(), iv1.clone(), cipher1.clone())
        val different = PasswordEncryptedData(byteArrayOf(9, 9, 9), iv1, cipher1)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, different)
        assertTrue(first.toString().contains("salt=3 bytes"))

        // Verify defensive cloning
        salt1[0] = 99
        assertEquals(1, first.salt()[0])
    }

    @Test
    fun shouldManagePkcs12KeyStoreEntriesAndPersistence() {
        val keyStoreService = cryptoServices.keyStoreService()
        val keyStore = keyStoreService.createPkcs12()
        val password = "keystore-pass".toCharArray()

        val certBytes = TEST_CERTIFICATE_PEM.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certBytes))
        val alias = "test-cert"

        keyStoreService.setCertificateEntry(keyStore, alias, cert)
        assertEquals(cert, keyStoreService.getCertificate(keyStore, alias))
        assertNotNull(keyStoreService.getPublicKey(keyStore, alias))

        val tempFile = Files.createTempFile("test-keystore", ".p12")
        try {
            keyStoreService.store(keyStore, tempFile, password)
            val loaded = keyStoreService.load(tempFile, password)

            assertEquals(cert, keyStoreService.getCertificate(loaded, alias))
            assertNull(keyStoreService.getCertificate(loaded, "non-existent"))
            assertThrows(IllegalStateException::class.java) {
                keyStoreService.getPrivateKey(loaded, "non-existent", password)
            }
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun shouldValidateWithCryptoExceptionsAndCryptoSupport() {
        assertTrue(CryptoSupport.isCryptoEnabled())

        val missingEx = CryptoExceptions.missing("testParam")
        assertEquals("testParam must not be null", missingEx.message)

        val emptyEx = CryptoExceptions.empty("testParam")
        assertEquals("testParam must not be empty", emptyEx.message)

        val cause = RuntimeException("root cause")
        val failedEx = CryptoExceptions.failed("perform test", cause)
        assertEquals("Failed to perform test", failedEx.message)
        assertEquals(cause, failedEx.cause)
    }

    companion object {
        private const val TEST_CERTIFICATE_PEM = """
-----BEGIN CERTIFICATE-----
MIIC0zCCAbugAwIBAgIIO1WV/337fZAwDQYJKoZIhvcNAQEMBQAwGDEWMBQGA1UE
AxMNU2VjdXJlTGFuVGVzdDAeFw0yNjEwMTIxODMxMDZaFw0yNzEwMTIxODMxMDZa
MBgxFjAUBgNVBAMTDVNlY3VyZUxhblRlc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDZ2Skw8zfWZuyUeLiNnBR9hknljsYZDStYqlS3pAXgV628LXt8
n/2yand871+DgiLcdn/oDiRihWRluv3/jGuqAaD8FNzyBjN3x1EEaeLBYZ7VN/P5
j7MhTlNWq6eTVXPr3ntQtyxLLBTp1m6SYJ+rNwd1sueSTs28mdt8GLJM4SA0nh3l
oqwN67yf4IhTwxsFFd4MSMjZO8U3xJmmENN2CbRY/yqVnWpKjjE4JEOmTKfvIXt1
M855Z/f3Eewz3yWCLQD209Zrd5kw9Pms5CWYuI7RktD0zuJBWuODVIH2Jqtu+3VZ
YxHW86n+kb7P6erxe6UVEOjR5JRKMGPkB1xxAgMBAAGjITAfMB0GA1UdDgQWBBTK
wWpokXVAfsXQaL4YyjdKShvzzTANBgkqhkiG9w0BAQwFAAOCAQEAWjqrQPnCxK48
rRoRvrJulmbiGvI3iEYNNXg1qW58fWI079Kt2EpyUqNe4dWKVv9gMYSoO3d4x7EA
RLvHVrZY8aVkN5EZ6bV9VxWl+nN6lYHjHq5T+IpAoNdGKkB5yr7wsNwFvhSZGuZE
akueOGeZiYhTMZPXJVJHdGnfKGouBPgBDku6rLJ/tmLlgo8rnixq3LRV2IIetL4Q
adPoHdmjx0plhs+1jxhtLOwgGKhmIrDNrK4NusUn9z6HQbub37zalTq9+Z5HlQeC
Na9r8RmMpbxhnN5isvP4W6QReetE+jGlvRDrRcPrVco5rQqpC1BjOs8RnAB2/+JF
nNH4N//1fA==
-----END CERTIFICATE-----
"""
    }
}
