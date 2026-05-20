package com.shterneregen.securelan.crypto.service

import java.security.PrivateKey
import java.security.PublicKey

interface SignatureService {
    fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray

    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean
}
