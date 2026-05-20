package com.shterneregen.securelan.crypto.service

import java.security.KeyPair
import javax.crypto.SecretKey

interface KeyGenerationService {
    fun generateAesKey(): SecretKey

    fun generateRsaKeyPair(): KeyPair
}
