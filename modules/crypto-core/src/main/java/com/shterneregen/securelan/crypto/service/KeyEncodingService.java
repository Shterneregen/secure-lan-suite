package com.shterneregen.securelan.crypto.service;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface KeyEncodingService {
    byte[] encodePublicKey(PublicKey publicKey);

    byte[] encodePrivateKey(PrivateKey privateKey);

    PublicKey decodePublicKey(byte[] encoded);

    PrivateKey decodePrivateKey(byte[] encoded);

    byte[] encodeSecretKey(SecretKey secretKey);

    SecretKey decodeAesKey(byte[] encoded);
}
