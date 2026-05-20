package com.shterneregen.securelan.crypto.service

import javax.crypto.SecretKey

interface AesGcmCryptoService {
    fun encrypt(plainText: ByteArray, key: SecretKey): ByteArray

    fun decrypt(cipherPayload: ByteArray, key: SecretKey): ByteArray
}
