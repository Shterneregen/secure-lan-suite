package com.shterneregen.securelan.crypto;

import com.shterneregen.securelan.crypto.model.HybridEncryptedData;
import com.shterneregen.securelan.crypto.model.PasswordEncryptedData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServicesTest {
    private final CryptoServices cryptoServices = CryptoServices.createDefault();

    @Test
    void shouldEncryptAndDecryptWithAesGcm() {
        byte[] payload = "hello aes".getBytes(StandardCharsets.UTF_8);
        var key = cryptoServices.keyGenerationService().generateAesKey();

        byte[] encrypted = cryptoServices.aesGcmCryptoService().encrypt(payload, key);
        byte[] decrypted = cryptoServices.aesGcmCryptoService().decrypt(encrypted, key);

        assertArrayEquals(payload, decrypted);
    }

    @Test
    void shouldEncryptAndDecryptWithRsaAndSignatures() {
        byte[] payload = "hello rsa".getBytes(StandardCharsets.UTF_8);
        KeyPair keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair();

        byte[] encrypted = cryptoServices.rsaCryptoService().encrypt(payload, keyPair.getPublic());
        byte[] decrypted = cryptoServices.rsaCryptoService().decrypt(encrypted, keyPair.getPrivate());
        byte[] signature = cryptoServices.signatureService().sign(payload, keyPair.getPrivate());

        assertArrayEquals(payload, decrypted);
        assertTrue(cryptoServices.signatureService().verify(payload, signature, keyPair.getPublic()));
    }

    @Test
    void shouldRoundTripHybridAndPasswordFileWorkflows() throws Exception {
        byte[] payload = "hello file crypto".getBytes(StandardCharsets.UTF_8);
        KeyPair keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair();

        HybridEncryptedData hybridEncryptedData = cryptoServices.hybridFileCryptoWorkflow().encrypt(payload, keyPair.getPublic());
        byte[] hybridDecrypted = cryptoServices.hybridFileCryptoWorkflow().decrypt(hybridEncryptedData, keyPair.getPrivate());

        PasswordEncryptedData passwordEncryptedData = cryptoServices.passwordFileCryptoWorkflow().encrypt(payload, "secret".toCharArray());
        byte[] passwordDecrypted = cryptoServices.passwordFileCryptoWorkflow().decrypt(passwordEncryptedData, "secret".toCharArray());

        Path tempFile = Files.createTempFile("secure-lan-suite", ".bin");
        try {
            Files.write(tempFile, payload);
            byte[] fileDecrypted = cryptoServices.fileCryptoService().decryptFileWithPrivateKey(
                    cryptoServices.fileCryptoService().encryptFileWithPublicKey(tempFile, keyPair.getPublic()),
                    keyPair.getPrivate()
            );
            assertArrayEquals(payload, fileDecrypted);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        assertArrayEquals(payload, hybridDecrypted);
        assertArrayEquals(payload, passwordDecrypted);
    }

    @Test
    void shouldEncodeAndDecodeRsaKeys() {
        KeyPair keyPair = cryptoServices.keyGenerationService().generateRsaKeyPair();

        byte[] encodedPublic = cryptoServices.keyEncodingService().encodePublicKey(keyPair.getPublic());
        byte[] encodedPrivate = cryptoServices.keyEncodingService().encodePrivateKey(keyPair.getPrivate());

        assertTrue(Arrays.equals(encodedPublic,
                cryptoServices.keyEncodingService().encodePublicKey(
                        cryptoServices.keyEncodingService().decodePublicKey(encodedPublic))));
        assertTrue(Arrays.equals(encodedPrivate,
                cryptoServices.keyEncodingService().encodePrivateKey(
                        cryptoServices.keyEncodingService().decodePrivateKey(encodedPrivate))));
    }
}
