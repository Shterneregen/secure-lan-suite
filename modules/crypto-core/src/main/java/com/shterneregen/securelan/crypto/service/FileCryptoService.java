package com.shterneregen.securelan.crypto.service;

import com.shterneregen.securelan.crypto.model.HybridEncryptedData;
import com.shterneregen.securelan.crypto.model.PasswordEncryptedData;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface FileCryptoService {
    HybridEncryptedData encryptFileWithPublicKey(Path path, PublicKey publicKey);

    byte[] decryptFileWithPrivateKey(HybridEncryptedData encryptedData, PrivateKey privateKey);

    PasswordEncryptedData encryptFileWithPassword(Path path, char[] password);

    byte[] decryptFileWithPassword(PasswordEncryptedData encryptedData, char[] password);
}
