package com.shterneregen.securelan.crypto.keystore.impl

import com.shterneregen.securelan.crypto.keystore.KeyStoreService
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate
import java.util.Objects

/**
 * Default implementation of [KeyStoreService] backed by standard PKCS#12 keystores.
 */
class DefaultKeyStoreService : KeyStoreService {
    override fun createPkcs12(): KeyStore = try {
        KeyStore.getInstance(TYPE).apply { load(null, null) }
    } catch (exception: IOException) {
        throw IllegalStateException("Failed to create PKCS12 keystore", exception)
    } catch (exception: GeneralSecurityException) {
        throw IllegalStateException("Failed to create PKCS12 keystore", exception)
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    override fun load(path: Path, password: CharArray): KeyStore {
        Objects.requireNonNull(path, "path")
        Objects.requireNonNull(password, "password")
        val keyStore = KeyStore.getInstance(TYPE)
        Files.newInputStream(path).use { inputStream -> keyStore.load(inputStream, password) }
        return keyStore
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    override fun store(keyStore: KeyStore, path: Path, password: CharArray) {
        Objects.requireNonNull(keyStore, "keyStore")
        Objects.requireNonNull(path, "path")
        Objects.requireNonNull(password, "password")
        path.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(path).use { outputStream -> keyStore.store(outputStream, password) }
    }

    @Throws(GeneralSecurityException::class)
    override fun setPrivateKeyEntry(
        keyStore: KeyStore,
        alias: String,
        privateKey: PrivateKey,
        entryPassword: CharArray,
        chain: Array<Certificate>,
    ) {
        Objects.requireNonNull(keyStore, "keyStore")
        Objects.requireNonNull(alias, "alias")
        Objects.requireNonNull(privateKey, "privateKey")
        Objects.requireNonNull(entryPassword, "entryPassword")
        Objects.requireNonNull(chain, "chain")
        keyStore.setKeyEntry(alias, privateKey, entryPassword, chain)
    }

    @Throws(GeneralSecurityException::class)
    override fun setCertificateEntry(keyStore: KeyStore, alias: String, certificate: Certificate) {
        Objects.requireNonNull(keyStore, "keyStore")
        Objects.requireNonNull(alias, "alias")
        Objects.requireNonNull(certificate, "certificate")
        keyStore.setCertificateEntry(alias, certificate)
    }

    @Throws(GeneralSecurityException::class)
    override fun getPrivateKey(keyStore: KeyStore, alias: String, entryPassword: CharArray): PrivateKey {
        Objects.requireNonNull(keyStore, "keyStore")
        Objects.requireNonNull(alias, "alias")
        Objects.requireNonNull(entryPassword, "entryPassword")
        val key = keyStore.getKey(alias, entryPassword)
            ?: throw IllegalStateException("No key entry found for alias: $alias")
        if (key !is PrivateKey) {
            throw IllegalStateException("Key for alias '$alias' is not a PrivateKey (found: ${key::class.java.name})")
        }
        return key
    }

    @Throws(GeneralSecurityException::class)
    override fun getPublicKey(keyStore: KeyStore, alias: String): PublicKey? {
        Objects.requireNonNull(keyStore, "keyStore")
        Objects.requireNonNull(alias, "alias")
        return getCertificate(keyStore, alias)?.publicKey
    }

    @Throws(GeneralSecurityException::class)
    override fun getCertificate(keyStore: KeyStore, alias: String): Certificate? {
        Objects.requireNonNull(keyStore, "keyStore")
        Objects.requireNonNull(alias, "alias")
        return keyStore.getCertificate(alias)
    }

    companion object {
        private const val TYPE = "PKCS12"
    }
}
