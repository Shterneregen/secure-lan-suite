package com.shterneregen.securelan.crypto.keystore

import java.io.IOException
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate

interface KeyStoreService {
    fun createPkcs12(): KeyStore

    @Throws(IOException::class, GeneralSecurityException::class)
    fun load(path: Path, password: CharArray): KeyStore

    @Throws(IOException::class, GeneralSecurityException::class)
    fun store(keyStore: KeyStore, path: Path, password: CharArray)

    @Throws(GeneralSecurityException::class)
    fun setPrivateKeyEntry(
        keyStore: KeyStore,
        alias: String,
        privateKey: PrivateKey,
        entryPassword: CharArray,
        chain: Array<Certificate>,
    )

    @Throws(GeneralSecurityException::class)
    fun setCertificateEntry(keyStore: KeyStore, alias: String, certificate: Certificate)

    @Throws(GeneralSecurityException::class)
    fun getPrivateKey(keyStore: KeyStore, alias: String, entryPassword: CharArray): PrivateKey

    @Throws(GeneralSecurityException::class)
    fun getPublicKey(keyStore: KeyStore, alias: String): PublicKey?

    @Throws(GeneralSecurityException::class)
    fun getCertificate(keyStore: KeyStore, alias: String): Certificate?
}
