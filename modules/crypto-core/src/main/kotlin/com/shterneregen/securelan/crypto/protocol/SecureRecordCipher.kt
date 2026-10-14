package com.shterneregen.securelan.crypto.protocol

import java.io.IOException
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Ordered AES-256-GCM records with separate direction keys and deterministic unique nonces.
 * Header and handshake context are AAD. Any authentication/order error permanently poisons the
 * reader. One reader and serialized writers are required. A key is limited to 2^32 records.
 */
class SecureRecordCipher(keys: SecureSessionKeys, client: Boolean) {
    private val sendKey = SecretKeySpec(keys.expand(if (client) "client-key" else "server-key", 32), "AES")
    private val receiveKey = SecretKeySpec(keys.expand(if (client) "server-key" else "client-key", 32), "AES")
    private val sendPrefix = keys.expand(if (client) "client-iv" else "server-iv", 4)
    private val receivePrefix = keys.expand(if (client) "server-iv" else "client-iv", 4)
    private val context = keys.context.copyOf()
    private var sent = 0L
    private var received = 0L
    private var failed = false

    @Synchronized
    fun encrypt(plain: ByteArray): ByteArray {
        if (plain.size > MAX_PLAINTEXT_BYTES || sent >= MAX_RECORDS) throw IOException("Record limit exceeded")
        val header = ByteBuffer.allocate(9).put(2).putLong(sent).array()
        val nonce = ByteBuffer.allocate(12).put(sendPrefix).putLong(sent).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sendKey, GCMParameterSpec(128, nonce))
        cipher.updateAAD(context + header)
        val result = header + cipher.doFinal(plain)
        sent++
        return result
    }

    fun decrypt(record: ByteArray): ByteArray {
        if (failed) throw IOException("Secure channel failed")
        try {
            if (record.size !in 25..MAX_RECORD_BYTES || received >= MAX_RECORDS) throw IOException("Invalid record size")
            val header = record.copyOfRange(0, 9)
            val parsed = ByteBuffer.wrap(header)
            if (parsed.get().toInt() != 2 || parsed.long != received) throw IOException("Unexpected record sequence")
            val nonce = ByteBuffer.allocate(12).put(receivePrefix).putLong(received).array()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, receiveKey, GCMParameterSpec(128, nonce))
            cipher.updateAAD(context + header)
            val plain = cipher.doFinal(record, 9, record.size - 9)
            received++
            return plain
        } catch (error: Exception) {
            failed = true
            throw IOException("Invalid secure record", error)
        }
    }

    companion object {
        const val MAX_PLAINTEXT_BYTES = 64 * 1024
        const val MAX_RECORD_BYTES = MAX_PLAINTEXT_BYTES + 25
        private const val MAX_RECORDS = 1L shl 32
    }
}
