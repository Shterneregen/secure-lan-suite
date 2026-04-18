package com.shterneregen.securelan.crypto.service.impl;

import com.shterneregen.securelan.crypto.service.AesGcmCryptoService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;

public final class DefaultAesGcmCryptoService implements AesGcmCryptoService {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public byte[] encrypt(byte[] plainText, SecretKey key) {
        Objects.requireNonNull(plainText, "plainText");
        Objects.requireNonNull(key, "key");
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText);
            return ByteBuffer.allocate(1 + iv.length + encrypted.length)
                    .put((byte) iv.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt AES-GCM payload", exception);
        }
    }

    @Override
    public byte[] decrypt(byte[] cipherPayload, SecretKey key) {
        Objects.requireNonNull(cipherPayload, "cipherPayload");
        Objects.requireNonNull(key, "key");
        try {
            ByteBuffer buffer = ByteBuffer.wrap(cipherPayload);
            int ivLength = Byte.toUnsignedInt(buffer.get());
            if (ivLength <= 0 || buffer.remaining() <= ivLength) {
                throw new IllegalArgumentException("Invalid AES-GCM payload");
            }
            byte[] iv = new byte[ivLength];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt AES-GCM payload", exception);
        }
    }
}
