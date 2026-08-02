package com.shterneregen.securelan.crypto.service.impl

import com.shterneregen.securelan.crypto.service.RsaCryptoService
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.util.Objects
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class DefaultRsaCryptoService : RsaCryptoService {
    override fun encrypt(plainText: ByteArray, publicKey: PublicKey): ByteArray {
        Objects.requireNonNull(plainText, "plainText")
        Objects.requireNonNull(publicKey, "publicKey")
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMETERS)
            return cipher.doFinal(plainText)
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to encrypt RSA payload", exception)
        }
    }

    override fun decrypt(cipherText: ByteArray, privateKey: PrivateKey): ByteArray {
        Objects.requireNonNull(cipherText, "cipherText")
        Objects.requireNonNull(privateKey, "privateKey")
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_PARAMETERS)
            return cipher.doFinal(cipherText)
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("Failed to decrypt RSA payload", exception)
        }
    }

    companion object {
        private const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private val OAEP_PARAMETERS = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA1,
            PSource.PSpecified.DEFAULT,
        )
    }
}
