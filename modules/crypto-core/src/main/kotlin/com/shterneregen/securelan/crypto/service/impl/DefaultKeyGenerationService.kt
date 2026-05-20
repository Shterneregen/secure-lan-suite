package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.service.KeyGenerationService
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class DefaultKeyGenerationService : KeyGenerationService {
    override fun generateAesKey(): SecretKey = try {
        KeyGenerator.getInstance("AES").apply { init(AES_KEY_SIZE) }.generateKey()
    } catch (exception: GeneralSecurityException) {
        throw IllegalStateException("Failed to generate AES key", exception)
    }

    override fun generateRsaKeyPair(): KeyPair = try {
        KeyPairGenerator.getInstance("RSA").apply { initialize(RSA_KEY_SIZE) }.generateKeyPair()
    } catch (exception: GeneralSecurityException) {
        throw IllegalStateException("Failed to generate RSA key pair", exception)
    }

    companion object {
        private const val AES_KEY_SIZE = 256
        private const val RSA_KEY_SIZE = 2048
    }
}
