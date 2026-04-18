package com.shterneregen.securelan.crypto.model;

import java.util.Arrays;
import java.util.Objects;

public record HybridEncryptedData(byte[] encryptedKey, byte[] iv, byte[] cipherText) {
    public HybridEncryptedData {
        Objects.requireNonNull(encryptedKey, "encryptedKey");
        Objects.requireNonNull(iv, "iv");
        Objects.requireNonNull(cipherText, "cipherText");
        encryptedKey = encryptedKey.clone();
        iv = iv.clone();
        cipherText = cipherText.clone();
    }

    @Override
    public byte[] encryptedKey() {
        return encryptedKey.clone();
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
        return "HybridEncryptedData[encryptedKey=" + encryptedKey.length
                + " bytes, iv=" + iv.length
                + " bytes, cipherText=" + cipherText.length + " bytes]";
    }
}
