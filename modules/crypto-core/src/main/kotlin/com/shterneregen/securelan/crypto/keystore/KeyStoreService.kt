package com.shterneregen.securelan.crypto.keystore

import java.io.IOException
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate

/**
 * Service providing PKCS#12 keystore lifecycle management, persistence, and entry retrieval.
 */
interface KeyStoreService {
    /**
     * Creates a new, empty in-memory PKCS#12 [KeyStore].
     *
     * @return An initialized, empty [KeyStore].
     * @throws IllegalStateException If creating the PKCS#12 keystore fails.
     */
    fun createPkcs12(): KeyStore

    /**
     * Loads a PKCS#12 [KeyStore] from the specified file [path] protected by [password].
     *
     * @param path The path to the keystore file.
     * @param password The keystore password.
     * @return The loaded [KeyStore] instance.
     * @throws IOException If an I/O error occurs while reading the file.
     * @throws GeneralSecurityException If keystore decryption or loading fails.
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun load(path: Path, password: CharArray): KeyStore

    /**
     * Persists the given [keyStore] to the specified file [path] protected by [password].
     * Parent directories will be created automatically if they do not exist.
     *
     * @param keyStore The [KeyStore] instance to store.
     * @param path The destination file path.
     * @param password The keystore protection password.
     * @throws IOException If an I/O error occurs while writing the file.
     * @throws GeneralSecurityException If keystore serialization or encryption fails.
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun store(keyStore: KeyStore, path: Path, password: CharArray)

    /**
     * Assigns the given [privateKey] and associated certificate [chain] to the specified [alias] in the [keyStore].
     *
     * @param keyStore The target keystore.
     * @param alias The entry alias.
     * @param privateKey The private key to store.
     * @param entryPassword The password protecting this key entry.
     * @param chain The certificate chain associated with the private key.
     * @throws GeneralSecurityException If storing the key entry fails.
     */
    @Throws(GeneralSecurityException::class)
    fun setPrivateKeyEntry(
        keyStore: KeyStore,
        alias: String,
        privateKey: PrivateKey,
        entryPassword: CharArray,
        chain: Array<Certificate>,
    )

    /**
     * Assigns the given [certificate] to the specified [alias] in the [keyStore].
     *
     * @param keyStore The target keystore.
     * @param alias The entry alias.
     * @param certificate The certificate to store.
     * @throws GeneralSecurityException If storing the certificate entry fails.
     */
    @Throws(GeneralSecurityException::class)
    fun setCertificateEntry(keyStore: KeyStore, alias: String, certificate: Certificate)

    /**
     * Retrieves the [PrivateKey] associated with the specified [alias] from the [keyStore].
     *
     * @param keyStore The keystore to inspect.
     * @param alias The entry alias.
     * @param entryPassword The password protecting this key entry.
     * @return The retrieved [PrivateKey].
     * @throws GeneralSecurityException If key extraction fails.
     * @throws IllegalStateException If no key is found or the key is not a [PrivateKey].
     */
    @Throws(GeneralSecurityException::class)
    fun getPrivateKey(keyStore: KeyStore, alias: String, entryPassword: CharArray): PrivateKey

    /**
     * Retrieves the [PublicKey] associated with the specified [alias] from the [keyStore] certificate entry.
     *
     * @param keyStore The keystore to inspect.
     * @param alias The entry alias.
     * @return The [PublicKey] extracted from the certificate, or `null` if no certificate entry exists for [alias].
     * @throws GeneralSecurityException If accessing the certificate fails.
     */
    @Throws(GeneralSecurityException::class)
    fun getPublicKey(keyStore: KeyStore, alias: String): PublicKey?

    /**
     * Retrieves the [Certificate] associated with the specified [alias] from the [keyStore].
     *
     * @param keyStore The keystore to inspect.
     * @param alias The entry alias.
     * @return The [Certificate], or `null` if no entry exists for [alias].
     * @throws GeneralSecurityException If accessing the certificate fails.
     */
    @Throws(GeneralSecurityException::class)
    fun getCertificate(keyStore: KeyStore, alias: String): Certificate?
}
