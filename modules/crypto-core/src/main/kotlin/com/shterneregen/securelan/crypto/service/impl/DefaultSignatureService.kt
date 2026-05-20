package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.service.SignatureService
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Objects

class DefaultSignatureService : SignatureService {
    override fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(privateKey, "privateKey")
        try {
            val signature = java.security.Signature.getInstance(ALGORITHM)
            signature.initSign(privateKey)
            signature.update(data)
            return signature.sign()
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to sign payload", exception)
        }
    }

    override fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        Objects.requireNonNull(data, "data")
        Objects.requireNonNull(signature, "signatureBytes")
        Objects.requireNonNull(publicKey, "publicKey")
        try {
            val verifier = java.security.Signature.getInstance(ALGORITHM)
            verifier.initVerify(publicKey)
            verifier.update(data)
            return verifier.verify(signature)
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to verify signature", exception)
        }
    }

    companion object {
        private const val ALGORITHM = "SHA256withRSA"
    }
}
