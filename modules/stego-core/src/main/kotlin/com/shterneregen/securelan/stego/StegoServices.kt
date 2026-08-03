package com.shterneregen.securelan.stego

import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.stego.service.SteganographyService
import com.shterneregen.securelan.stego.service.impl.BmpSteganographyService
import com.shterneregen.securelan.stego.service.StegoInspectionService
import com.shterneregen.securelan.stego.service.impl.BmpStegoInspectionService
import java.util.Objects

class StegoServices private constructor(cryptoServices: CryptoServices) {
    private val steganographyService: SteganographyService

    init {
        Objects.requireNonNull(cryptoServices, "cryptoServices")
        steganographyService = BmpSteganographyService(cryptoServices.passwordFileCryptoWorkflow())
    }

    fun steganographyService(): SteganographyService = steganographyService

    fun inspectionService(): StegoInspectionService = BmpStegoInspectionService()

    companion object {
        @JvmStatic
        fun createDefault(): StegoServices = StegoServices(CryptoServices.createDefault())

        @JvmStatic
        fun create(cryptoServices: CryptoServices): StegoServices = StegoServices(cryptoServices)
    }
}
