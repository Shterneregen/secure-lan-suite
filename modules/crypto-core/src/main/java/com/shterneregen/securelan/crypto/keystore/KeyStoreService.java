package com.shterneregen.securelan.crypto.keystore;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public interface KeyStoreService {
    KeyStore createPkcs12();

    KeyStore load(Path path, char[] password) throws IOException, GeneralSecurityException;

    void store(KeyStore keyStore, Path path, char[] password) throws IOException, GeneralSecurityException;

    void setPrivateKeyEntry(KeyStore keyStore,
                            String alias,
                            PrivateKey privateKey,
                            char[] entryPassword,
                            Certificate[] chain) throws GeneralSecurityException;

    void setCertificateEntry(KeyStore keyStore, String alias, Certificate certificate) throws GeneralSecurityException;

    PrivateKey getPrivateKey(KeyStore keyStore, String alias, char[] entryPassword) throws GeneralSecurityException;

    PublicKey getPublicKey(KeyStore keyStore, String alias) throws GeneralSecurityException;

    Certificate getCertificate(KeyStore keyStore, String alias) throws GeneralSecurityException;
}
