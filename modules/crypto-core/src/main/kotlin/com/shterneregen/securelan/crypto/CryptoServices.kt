package com.shterneregen.securelan.crypto

import com.shterneregen.securelan.crypto.keystore.KeyStoreService
import com.shterneregen.securelan.crypto.keystore.impl.DefaultKeyStoreService
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService
import com.shterneregen.securelan.crypto.service.FileCryptoService
import com.shterneregen.securelan.crypto.service.HashService
import com.shterneregen.securelan.crypto.service.KeyEncodingService
import com.shterneregen.securelan.crypto.service.KeyGenerationService
import com.shterneregen.securelan.crypto.service.RsaCryptoService
import com.shterneregen.securelan.crypto.service.SignatureService
import com.shterneregen.securelan.crypto.service.impl.DefaultAesGcmCryptoService
import com.shterneregen.securelan.crypto.service.impl.DefaultFileCryptoService
import com.shterneregen.securelan.crypto.service.impl.DefaultHashService
import com.shterneregen.securelan.crypto.service.impl.DefaultKeyEncodingService
import com.shterneregen.securelan.crypto.service.impl.DefaultKeyGenerationService
import com.shterneregen.securelan.crypto.service.impl.DefaultRsaCryptoService
import com.shterneregen.securelan.crypto.service.impl.DefaultSignatureService
import com.shterneregen.securelan.crypto.workflow.HybridFileCryptoWorkflow
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow
import com.shterneregen.securelan.crypto.workflow.impl.DefaultHybridFileCryptoWorkflow
import com.shterneregen.securelan.crypto.workflow.impl.DefaultPasswordFileCryptoWorkflow

/**
 * Service registry and factory providing access to cryptographic services and workflows.
 *
 * Instances can be created using [createDefault].
 */
class CryptoServices private constructor() {
    private val keyGenerationService: KeyGenerationService = DefaultKeyGenerationService()
    private val keyEncodingService: KeyEncodingService = DefaultKeyEncodingService()
    private val aesGcmCryptoService: AesGcmCryptoService = DefaultAesGcmCryptoService()
    private val rsaCryptoService: RsaCryptoService = DefaultRsaCryptoService()
    private val hashService: HashService = DefaultHashService()
    private val signatureService: SignatureService = DefaultSignatureService()
    private val hybridFileCryptoWorkflow: HybridFileCryptoWorkflow = DefaultHybridFileCryptoWorkflow(
        aesGcmCryptoService,
        rsaCryptoService,
        keyGenerationService,
        keyEncodingService,
    )
    private val passwordFileCryptoWorkflow: PasswordFileCryptoWorkflow = DefaultPasswordFileCryptoWorkflow()
    private val fileCryptoService: FileCryptoService = DefaultFileCryptoService(hybridFileCryptoWorkflow, passwordFileCryptoWorkflow)
    private val keyStoreService: KeyStoreService = DefaultKeyStoreService()

    /**
     * Returns the [KeyGenerationService] for generating AES keys and RSA key pairs.
     *
     * @return The key generation service instance.
     */
    fun keyGenerationService(): KeyGenerationService = keyGenerationService

    /**
     * Returns the [KeyEncodingService] for encoding and decoding keys.
     *
     * @return The key encoding service instance.
     */
    fun keyEncodingService(): KeyEncodingService = keyEncodingService

    /**
     * Returns the [AesGcmCryptoService] for symmetric AES-GCM encryption and decryption.
     *
     * @return The AES-GCM crypto service instance.
     */
    fun aesGcmCryptoService(): AesGcmCryptoService = aesGcmCryptoService

    /**
     * Returns the [RsaCryptoService] for asymmetric RSA-OAEP encryption and decryption.
     *
     * @return The RSA crypto service instance.
     */
    fun rsaCryptoService(): RsaCryptoService = rsaCryptoService

    /**
     * Returns the [HashService] for SHA-256 hashing.
     *
     * @return The hash service instance.
     */
    fun hashService(): HashService = hashService

    /**
     * Returns the [SignatureService] for SHA256withRSA signing and verification.
     *
     * @return The signature service instance.
     */
    fun signatureService(): SignatureService = signatureService

    /**
     * Returns the [HybridFileCryptoWorkflow] for hybrid AES/RSA payload encryption and decryption.
     *
     * @return The hybrid file crypto workflow instance.
     */
    fun hybridFileCryptoWorkflow(): HybridFileCryptoWorkflow = hybridFileCryptoWorkflow

    /**
     * Returns the [PasswordFileCryptoWorkflow] for PBKDF2/AES-GCM password-based payload encryption.
     *
     * @return The password file crypto workflow instance.
     */
    fun passwordFileCryptoWorkflow(): PasswordFileCryptoWorkflow = passwordFileCryptoWorkflow

    /**
     * Returns the [FileCryptoService] for high-level file-based encryption workflows.
     *
     * @return The file crypto service instance.
     */
    fun fileCryptoService(): FileCryptoService = fileCryptoService

    /**
     * Returns the [KeyStoreService] for PKCS#12 keystore operations.
     *
     * @return The keystore service instance.
     */
    fun keyStoreService(): KeyStoreService = keyStoreService

    companion object {
        /**
         * Creates a new [CryptoServices] registry populated with default service implementations.
         *
         * @return A new [CryptoServices] instance.
         */
        @JvmStatic
        fun createDefault(): CryptoServices = CryptoServices()
    }
}
