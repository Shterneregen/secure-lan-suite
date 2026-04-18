package com.shterneregen.securelan.crypto.keystore.impl;

import com.shterneregen.securelan.crypto.keystore.KeyStoreService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Objects;

public final class DefaultKeyStoreService implements KeyStoreService {
    private static final String TYPE = "PKCS12";

    @Override
    public KeyStore createPkcs12() {
        try {
            KeyStore keyStore = KeyStore.getInstance(TYPE);
            keyStore.load(null, null);
            return keyStore;
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to create PKCS12 keystore", exception);
        }
    }

    @Override
    public KeyStore load(Path path, char[] password) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(path, "path");
        KeyStore keyStore = KeyStore.getInstance(TYPE);
        try (InputStream inputStream = Files.newInputStream(path)) {
            keyStore.load(inputStream, password);
        }
        return keyStore;
    }

    @Override
    public void store(KeyStore keyStore, Path path, char[] password) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(keyStore, "keyStore");
        Objects.requireNonNull(path, "path");
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            keyStore.store(outputStream, password);
        }
    }

    @Override
    public void setPrivateKeyEntry(KeyStore keyStore,
                                   String alias,
                                   PrivateKey privateKey,
                                   char[] entryPassword,
                                   Certificate[] chain) throws GeneralSecurityException {
        Objects.requireNonNull(keyStore, "keyStore");
        keyStore.setKeyEntry(alias, privateKey, entryPassword, chain);
    }

    @Override
    public void setCertificateEntry(KeyStore keyStore, String alias, Certificate certificate) throws GeneralSecurityException {
        Objects.requireNonNull(keyStore, "keyStore");
        keyStore.setCertificateEntry(alias, certificate);
    }

    @Override
    public PrivateKey getPrivateKey(KeyStore keyStore, String alias, char[] entryPassword) throws GeneralSecurityException {
        Objects.requireNonNull(keyStore, "keyStore");
        return (PrivateKey) keyStore.getKey(alias, entryPassword);
    }

    @Override
    public PublicKey getPublicKey(KeyStore keyStore, String alias) throws GeneralSecurityException {
        Certificate certificate = getCertificate(keyStore, alias);
        return certificate == null ? null : certificate.getPublicKey();
    }

    @Override
    public Certificate getCertificate(KeyStore keyStore, String alias) throws GeneralSecurityException {
        Objects.requireNonNull(keyStore, "keyStore");
        return keyStore.getCertificate(alias);
    }
}
