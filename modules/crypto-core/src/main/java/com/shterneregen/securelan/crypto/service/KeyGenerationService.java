package com.shterneregen.securelan.crypto.service;

import javax.crypto.SecretKey;
import java.security.KeyPair;

public interface KeyGenerationService {
    SecretKey generateAesKey();

    KeyPair generateRsaKeyPair();
}
