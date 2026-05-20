package com.shterneregen.securelan.crypto.workflow

import com.shterneregen.securelan.crypto.model.HybridEncryptedData
import java.security.PrivateKey
import java.security.PublicKey

interface HybridFileCryptoWorkflow {
    fun encrypt(fileBytes: ByteArray, publicKey: PublicKey): HybridEncryptedData

    fun decrypt(encryptedData: HybridEncryptedData, privateKey: PrivateKey): ByteArray
}
