package com.shterneregen.securelan.crypto.workflow

import com.shterneregen.securelan.crypto.model.PasswordEncryptedData

interface PasswordFileCryptoWorkflow {
    fun encrypt(fileBytes: ByteArray, password: CharArray): PasswordEncryptedData

    fun decrypt(encryptedData: PasswordEncryptedData, password: CharArray): ByteArray
}
