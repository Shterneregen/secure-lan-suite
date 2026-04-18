package com.shterneregen.securelan.crypto.model;

import java.util.Objects;

public record PasswordEncryptedData(byte[] salt, byte[] iv, byte[] cipherText) {
    public PasswordEncryptedData {
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(iv, "iv");
        Objects.requireNonNull(cipherText, "cipherText");
        salt = salt.clone();
        iv = iv.clone();
        cipherText = cipherText.clone();
    }

    @Override
    public byte[] salt() {
        return salt.clone();
    }

    @Override
    public byte[] iv() {
        return iv.clone();
    }

    @Override
    public byte[] cipherText() {
        return cipherText.clone();
    }

    @Override
    public String toString() {
        return "PasswordEncryptedData[salt=" + salt.length
                + " bytes, iv=" + iv.length
                + " bytes, cipherText=" + cipherText.length + " bytes]";
    }
}
