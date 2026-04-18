package com.shterneregen.securelan.crypto.workflow.impl;

import com.shterneregen.securelan.crypto.model.PasswordEncryptedData;
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;

public final class DefaultPasswordFileCryptoWorkflow implements PasswordFileCryptoWorkflow {
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int ITERATIONS = 65_536;
    private static final int KEY_SIZE = 256;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public PasswordEncryptedData encrypt(byte[] fileBytes, char[] password) {
        Objects.requireNonNull(fileBytes, "fileBytes");
        Objects.requireNonNull(password, "password");
        try {
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(salt);
            SECURE_RANDOM.nextBytes(iv);
            SecretKey key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new PasswordEncryptedData(salt, iv, cipher.doFinal(fileBytes));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt file with password", exception);
        }
    }

    @Override
    public byte[] decrypt(PasswordEncryptedData encryptedData, char[] password) {
        Objects.requireNonNull(encryptedData, "encryptedData");
        Objects.requireNonNull(password, "password");
        try {
            SecretKey key = deriveKey(password, encryptedData.salt());
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, encryptedData.iv()));
            return cipher.doFinal(encryptedData.cipherText());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt file with password", exception);
        }
    }

    private SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = secretKeyFactory.generateSecret(keySpec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
