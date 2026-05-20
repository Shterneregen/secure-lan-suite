package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.model.HybridEncryptedData
import com.shterneregen.securelan.crypto.model.PasswordEncryptedData
import com.shterneregen.securelan.crypto.service.FileCryptoService
import com.shterneregen.securelan.crypto.workflow.HybridFileCryptoWorkflow
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Objects

class DefaultFileCryptoService(
    private val hybridWorkflow: HybridFileCryptoWorkflow,
    private val passwordWorkflow: PasswordFileCryptoWorkflow,
) : FileCryptoService {
    init {
        Objects.requireNonNull(hybridWorkflow, "hybridWorkflow")
        Objects.requireNonNull(passwordWorkflow, "passwordWorkflow")
    }

    override fun encryptFileWithPublicKey(path: Path, publicKey: PublicKey): HybridEncryptedData = hybridWorkflow.encrypt(readAllBytes(path), publicKey)

    override fun decryptFileWithPrivateKey(encryptedData: HybridEncryptedData, privateKey: PrivateKey): ByteArray = hybridWorkflow.decrypt(encryptedData, privateKey)

    override fun encryptFileWithPassword(path: Path, password: CharArray): PasswordEncryptedData = passwordWorkflow.encrypt(readAllBytes(path), password)

    override fun decryptFileWithPassword(encryptedData: PasswordEncryptedData, password: CharArray): ByteArray = passwordWorkflow.decrypt(encryptedData, password)

    private fun readAllBytes(path: Path): ByteArray {
        Objects.requireNonNull(path, "path")
        try {
            return Files.readAllBytes(path)
        } catch (exception: IOException) {
            throw IllegalStateException("Failed to read file bytes: $path", exception)
        }
    }
}
