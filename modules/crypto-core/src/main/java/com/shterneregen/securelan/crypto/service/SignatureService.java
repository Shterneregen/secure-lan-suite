package com.shterneregen.securelan.crypto.service;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface SignatureService {
    byte[] sign(byte[] data, PrivateKey privateKey);

    boolean verify(byte[] data, byte[] signature, PublicKey publicKey);
}
