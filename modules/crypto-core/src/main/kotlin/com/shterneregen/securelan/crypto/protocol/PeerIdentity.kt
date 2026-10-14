package com.shterneregen.securelan.crypto.protocol

import java.io.IOException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Ephemeral room identity; its public key is distributed through authenticated room presence. */
class PeerIdentity {
    private val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    val publicKey: String = Base64.getEncoder().encodeToString(pair.public.encoded)

    fun sign(payload: ByteArray): String = Base64.getEncoder().encodeToString(
        Signature.getInstance("SHA256withECDSA").run { initSign(pair.private); update(payload); sign() },
    )

    companion object {
        fun verify(publicKey: String, payload: ByteArray, proof: String) {
            try {
                if (publicKey.length !in 1..256 || proof.length !in 1..256) throw IOException("Invalid peer proof")
                val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
                val valid = Signature.getInstance("SHA256withECDSA").run {
                    initVerify(key); update(payload); verify(Base64.getDecoder().decode(proof))
                }
                if (!valid) throw IOException("Peer authentication failed")
            } catch (error: IOException) { throw error } catch (error: Exception) { throw IOException("Peer authentication failed", error) }
        }
    }
}
