package com.shterneregen.securelan.crypto.service.impl;

import com.shterneregen.securelan.crypto.model.HybridEncryptedData;
import com.shterneregen.securelan.crypto.model.PasswordEncryptedData;
import com.shterneregen.securelan.crypto.service.FileCryptoService;
import com.shterneregen.securelan.crypto.workflow.HybridFileCryptoWorkflow;
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

public final class DefaultFileCryptoService implements FileCryptoService {
    private final HybridFileCryptoWorkflow hybridWorkflow;
    private final PasswordFileCryptoWorkflow passwordWorkflow;

    public DefaultFileCryptoService(HybridFileCryptoWorkflow hybridWorkflow,
                                    PasswordFileCryptoWorkflow passwordWorkflow) {
        this.hybridWorkflow = Objects.requireNonNull(hybridWorkflow, "hybridWorkflow");
        this.passwordWorkflow = Objects.requireNonNull(passwordWorkflow, "passwordWorkflow");
    }

    @Override
    public HybridEncryptedData encryptFileWithPublicKey(Path path, PublicKey publicKey) {
        return hybridWorkflow.encrypt(readAllBytes(path), publicKey);
    }

    @Override
    public byte[] decryptFileWithPrivateKey(HybridEncryptedData encryptedData, PrivateKey privateKey) {
        return hybridWorkflow.decrypt(encryptedData, privateKey);
    }

    @Override
    public PasswordEncryptedData encryptFileWithPassword(Path path, char[] password) {
        return passwordWorkflow.encrypt(readAllBytes(path), password);
    }

    @Override
    public byte[] decryptFileWithPassword(PasswordEncryptedData encryptedData, char[] password) {
        return passwordWorkflow.decrypt(encryptedData, password);
    }

    private byte[] readAllBytes(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read file bytes: " + path, exception);
        }
    }
}
