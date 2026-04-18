package com.shterneregen.securelan.crypto.service.impl;

import com.shterneregen.securelan.crypto.service.RsaCryptoService;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

public final class DefaultRsaCryptoService implements RsaCryptoService {
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    @Override
    public byte[] encrypt(byte[] plainText, PublicKey publicKey) {
        Objects.requireNonNull(plainText, "plainText");
        Objects.requireNonNull(publicKey, "publicKey");
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(plainText);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt RSA payload", exception);
        }
    }

    @Override
    public byte[] decrypt(byte[] cipherText, PrivateKey privateKey) {
        Objects.requireNonNull(cipherText, "cipherText");
        Objects.requireNonNull(privateKey, "privateKey");
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(cipherText);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt RSA payload", exception);
        }
    }
}
