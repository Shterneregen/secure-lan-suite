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

    fun keyGenerationService(): KeyGenerationService = keyGenerationService

    fun keyEncodingService(): KeyEncodingService = keyEncodingService

    fun aesGcmCryptoService(): AesGcmCryptoService = aesGcmCryptoService

    fun rsaCryptoService(): RsaCryptoService = rsaCryptoService

    fun hashService(): HashService = hashService

    fun signatureService(): SignatureService = signatureService

    fun hybridFileCryptoWorkflow(): HybridFileCryptoWorkflow = hybridFileCryptoWorkflow

    fun passwordFileCryptoWorkflow(): PasswordFileCryptoWorkflow = passwordFileCryptoWorkflow

    fun fileCryptoService(): FileCryptoService = fileCryptoService

    fun keyStoreService(): KeyStoreService = keyStoreService

    companion object {
        @JvmStatic
        fun createDefault(): CryptoServices = CryptoServices()
    }
}
