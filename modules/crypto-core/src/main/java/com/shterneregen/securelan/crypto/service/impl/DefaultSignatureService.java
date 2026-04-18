package com.shterneregen.securelan.crypto.service.impl;

import com.shterneregen.securelan.crypto.service.SignatureService;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

public final class DefaultSignatureService implements SignatureService {
    private static final String ALGORITHM = "SHA256withRSA";

    @Override
    public byte[] sign(byte[] data, PrivateKey privateKey) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(privateKey, "privateKey");
        try {
            java.security.Signature signature = java.security.Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to sign payload", exception);
        }
    }

    @Override
    public boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(signatureBytes, "signatureBytes");
        Objects.requireNonNull(publicKey, "publicKey");
        try {
            java.security.Signature signature = java.security.Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to verify signature", exception);
        }
    }
}
