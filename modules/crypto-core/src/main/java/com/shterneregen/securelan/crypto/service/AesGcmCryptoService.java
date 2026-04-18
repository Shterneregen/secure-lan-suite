package com.shterneregen.securelan.crypto.service;

import javax.crypto.SecretKey;

public interface AesGcmCryptoService {
    byte[] encrypt(byte[] plainText, SecretKey key);

    byte[] decrypt(byte[] cipherPayload, SecretKey key);
}
