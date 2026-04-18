package com.shterneregen.securelan.crypto.workflow;

import com.shterneregen.securelan.crypto.model.HybridEncryptedData;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface HybridFileCryptoWorkflow {
    HybridEncryptedData encrypt(byte[] fileBytes, PublicKey publicKey);

    byte[] decrypt(HybridEncryptedData encryptedData, PrivateKey privateKey);
}
