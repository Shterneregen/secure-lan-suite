package com.shterneregen.securelan.crypto.service;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface RsaCryptoService {
    byte[] encrypt(byte[] plainText, PublicKey publicKey);

    byte[] decrypt(byte[] cipherText, PrivateKey privateKey);
}
