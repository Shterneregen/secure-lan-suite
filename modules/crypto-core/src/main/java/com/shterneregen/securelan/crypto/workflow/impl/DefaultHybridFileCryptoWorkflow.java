package com.shterneregen.securelan.crypto.workflow.impl;

import com.shterneregen.securelan.crypto.model.HybridEncryptedData;
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService;
import com.shterneregen.securelan.crypto.service.KeyEncodingService;
import com.shterneregen.securelan.crypto.service.KeyGenerationService;
import com.shterneregen.securelan.crypto.service.RsaCryptoService;
import com.shterneregen.securelan.crypto.workflow.HybridFileCryptoWorkflow;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

public final class DefaultHybridFileCryptoWorkflow implements HybridFileCryptoWorkflow {
    private final AesGcmCryptoService aesGcmCryptoService;
    private final RsaCryptoService rsaCryptoService;
    private final KeyGenerationService keyGenerationService;
    private final KeyEncodingService keyEncodingService;

    public DefaultHybridFileCryptoWorkflow(AesGcmCryptoService aesGcmCryptoService,
                                           RsaCryptoService rsaCryptoService,
                                           KeyGenerationService keyGenerationService,
                                           KeyEncodingService keyEncodingService) {
        this.aesGcmCryptoService = Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService");
        this.rsaCryptoService = Objects.requireNonNull(rsaCryptoService, "rsaCryptoService");
        this.keyGenerationService = Objects.requireNonNull(keyGenerationService, "keyGenerationService");
        this.keyEncodingService = Objects.requireNonNull(keyEncodingService, "keyEncodingService");
    }

    @Override
    public HybridEncryptedData encrypt(byte[] fileBytes, PublicKey publicKey) {
        Objects.requireNonNull(fileBytes, "fileBytes");
        Objects.requireNonNull(publicKey, "publicKey");
        SecretKey sessionKey = keyGenerationService.generateAesKey();
        byte[] encryptedPayload = aesGcmCryptoService.encrypt(fileBytes, sessionKey);
        ByteBuffer buffer = ByteBuffer.wrap(encryptedPayload);
        int ivLength = Byte.toUnsignedInt(buffer.get());
        byte[] iv = new byte[ivLength];
        buffer.get(iv);
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);
        byte[] encryptedKey = rsaCryptoService.encrypt(keyEncodingService.encodeSecretKey(sessionKey), publicKey);
        return new HybridEncryptedData(encryptedKey, iv, cipherText);
    }

    @Override
    public byte[] decrypt(HybridEncryptedData encryptedData, PrivateKey privateKey) {
        Objects.requireNonNull(encryptedData, "encryptedData");
        Objects.requireNonNull(privateKey, "privateKey");
        byte[] encodedAesKey = rsaCryptoService.decrypt(encryptedData.encryptedKey(), privateKey);
        SecretKey sessionKey = keyEncodingService.decodeAesKey(encodedAesKey);
        byte[] payload = ByteBuffer.allocate(1 + encryptedData.iv().length + encryptedData.cipherText().length)
                .put((byte) encryptedData.iv().length)
                .put(encryptedData.iv())
                .put(encryptedData.cipherText())
                .array();
        return aesGcmCryptoService.decrypt(payload, sessionKey);
    }
}
