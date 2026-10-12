package com.shterneregen.securelan.stego

import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.stego.service.SteganographyService
import com.shterneregen.securelan.stego.service.StegoInspectionService
import com.shterneregen.securelan.stego.service.impl.BmpSteganographyService
import com.shterneregen.securelan.stego.service.impl.BmpStegoInspectionService
import java.util.Objects

/**
 * Service registry and factory providing access to steganography operations and analysis services.
 *
 * Instances can be created using [createDefault] or [create].
 */
class StegoServices private constructor(cryptoServices: CryptoServices) {
    private val steganographyService: SteganographyService

    init {
        Objects.requireNonNull(cryptoServices, "cryptoServices")
        steganographyService = BmpSteganographyService(cryptoServices.passwordFileCryptoWorkflow())
    }

    /**
     * Returns the [SteganographyService] instance for embedding and extracting steganographic payloads.
     *
     * @return The steganography service instance.
     */
    fun steganographyService(): SteganographyService = steganographyService

    /**
     * Returns a [StegoInspectionService] instance for statistical analysis and inspection of carrier images.
     *
     * @return The steganography inspection service instance.
     */
    fun inspectionService(): StegoInspectionService = BmpStegoInspectionService()

    companion object {
        /**
         * Creates a new [StegoServices] container initialized with the default [CryptoServices] configuration.
         *
         * @return A default [StegoServices] instance.
         */
        @JvmStatic
        fun createDefault(): StegoServices = StegoServices(CryptoServices.createDefault())

        /**
         * Creates a new [StegoServices] container with the specified [CryptoServices] dependency.
         *
         * @param cryptoServices The cryptographic services instance used for password-based encryption and decryption.
         * @return A configured [StegoServices] instance.
         */
        @JvmStatic
        fun create(cryptoServices: CryptoServices): StegoServices = StegoServices(cryptoServices)
    }
}
