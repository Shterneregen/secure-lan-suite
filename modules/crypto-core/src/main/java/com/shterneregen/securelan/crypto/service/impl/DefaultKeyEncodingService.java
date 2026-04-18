package com.shterneregen.securelan.crypto.service.impl;

import com.shterneregen.securelan.crypto.service.KeyEncodingService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

public final class DefaultKeyEncodingService implements KeyEncodingService {
    @Override
    public byte[] encodePublicKey(PublicKey publicKey) {
        Objects.requireNonNull(publicKey, "publicKey");
        return publicKey.getEncoded().clone();
    }

    @Override
    public byte[] encodePrivateKey(PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "privateKey");
        return privateKey.getEncoded().clone();
    }

    @Override
    public PublicKey decodePublicKey(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decode public key", exception);
        }
    }

    @Override
    public PrivateKey decodePrivateKey(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decode private key", exception);
        }
    }

    @Override
    public byte[] encodeSecretKey(SecretKey secretKey) {
        Objects.requireNonNull(secretKey, "secretKey");
        return secretKey.getEncoded().clone();
    }

    @Override
    public SecretKey decodeAesKey(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        return new SecretKeySpec(encoded.clone(), "AES");
    }
}
