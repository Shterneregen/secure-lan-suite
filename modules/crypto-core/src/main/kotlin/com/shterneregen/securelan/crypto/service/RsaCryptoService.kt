package com.shterneregen.securelan.crypto.service

import java.security.PrivateKey
import java.security.PublicKey

interface RsaCryptoService {
    fun encrypt(plainText: ByteArray, publicKey: PublicKey): ByteArray

    fun decrypt(cipherText: ByteArray, privateKey: PrivateKey): ByteArray
}
