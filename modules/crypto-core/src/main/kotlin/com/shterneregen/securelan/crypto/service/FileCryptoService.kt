package com.shterneregen.securelan.crypto.service

import com.shterneregen.securelan.crypto.model.HybridEncryptedData
import com.shterneregen.securelan.crypto.model.PasswordEncryptedData
import java.nio.file.Path
import java.security.PrivateKey
import java.security.PublicKey

interface FileCryptoService {
    fun encryptFileWithPublicKey(path: Path, publicKey: PublicKey): HybridEncryptedData

    fun decryptFileWithPrivateKey(encryptedData: HybridEncryptedData, privateKey: PrivateKey): ByteArray

    fun encryptFileWithPassword(path: Path, password: CharArray): PasswordEncryptedData

    fun decryptFileWithPassword(encryptedData: PasswordEncryptedData, password: CharArray): ByteArray
}
