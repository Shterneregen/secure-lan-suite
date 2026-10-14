package com.shterneregen.securelan.crypto.protocol

import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Per-handshake secrets. Never exposes secrets through diagnostics or value-object toString. */
class SecureSessionKeys private constructor(internal val secret: ByteArray, internal val context: ByteArray) {
    internal fun expand(label: String, size: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return mac.doFinal(label.toByteArray(Charsets.UTF_8) + context + byteArrayOf(1)).copyOf(size)
    }

    /** Channel binding for application-level peer signatures; never the traffic key itself. */
    fun binding(): ByteArray = expand("securelan-v2-channel-binding", 32)

    companion object {
        internal fun derive(key: BigInteger, identity: String, salt: ByteArray, clientA: BigInteger, serverB: BigInteger): SecureSessionKeys {
            val digest = MessageDigest.getInstance("SHA-256")
            val context = digest.digest(identity.toByteArray(Charsets.UTF_8) + salt +
                clientA.toString(16).toByteArray(Charsets.US_ASCII) + byteArrayOf(0) +
                serverB.toString(16).toByteArray(Charsets.US_ASCII))
            val encoded = key.toByteArray().takeLast(32).toByteArray()
            val padded = ByteArray(32)
            encoded.copyInto(padded, 32 - encoded.size)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(context, "HmacSHA256"))
            val secret = mac.doFinal(padded)
            padded.fill(0)
            encoded.fill(0)
            return SecureSessionKeys(secret, context)
        }
    }
}
