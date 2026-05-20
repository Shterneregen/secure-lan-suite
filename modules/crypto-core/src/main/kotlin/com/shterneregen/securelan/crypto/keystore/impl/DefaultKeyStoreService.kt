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
        val keyStore = KeyStore.getInstance(TYPE)
        Files.newInputStream(path).use { inputStream -> keyStore.load(inputStream, password) }
        return keyStore
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    override fun store(keyStore: KeyStore, path: Path, password: CharArray) {
        Objects.requireNonNull(keyStore, "keyStore")
        Objects.requireNonNull(path, "path")
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
        keyStore.setKeyEntry(alias, privateKey, entryPassword, chain)
    }

    @Throws(GeneralSecurityException::class)
    override fun setCertificateEntry(keyStore: KeyStore, alias: String, certificate: Certificate) {
        Objects.requireNonNull(keyStore, "keyStore")
        keyStore.setCertificateEntry(alias, certificate)
    }

    @Throws(GeneralSecurityException::class)
    override fun getPrivateKey(keyStore: KeyStore, alias: String, entryPassword: CharArray): PrivateKey {
        Objects.requireNonNull(keyStore, "keyStore")
        return keyStore.getKey(alias, entryPassword) as PrivateKey
    }

    @Throws(GeneralSecurityException::class)
    override fun getPublicKey(keyStore: KeyStore, alias: String): PublicKey? = getCertificate(keyStore, alias)?.publicKey

    @Throws(GeneralSecurityException::class)
    override fun getCertificate(keyStore: KeyStore, alias: String): Certificate? {
        Objects.requireNonNull(keyStore, "keyStore")
        return keyStore.getCertificate(alias)
    }

    companion object {
        private const val TYPE = "PKCS12"
    }
}
