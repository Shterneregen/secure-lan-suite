package com.shterneregen.securelan.androidclient.protocol

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.SecretKey

/**
 * UTF-8 line-framed chat transport: plaintext handshake followed by Base64 AES-GCM frames.
 * One reader may run alongside serialized writers. Enable encryption before publishing the session;
 * closing the session closes its socket and unblocks the reader.
 */
class ChatSession(private val socket: Socket) : Closeable {
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
    private val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
    private var transportKey: SecretKey? = null

    /** Returns the next decoded message, or null at transport EOF; malformed frames throw. */
    fun readMessage(): WireMessage? {
        var line = reader.readLine() ?: return null
        val key = transportKey
        if (key != null) {
            val decrypted = CryptoCompat.aesDecrypt(Base64.getDecoder().decode(line), key)
            line = String(decrypted, StandardCharsets.UTF_8)
        }
        return WireMessage.deserialize(line)
    }

    @Synchronized
    fun writeMessage(message: WireMessage) {
        var line = message.serialize()
        val key = transportKey
        if (key != null) {
            val encrypted = CryptoCompat.aesEncrypt(line.toByteArray(StandardCharsets.UTF_8), key)
            line = Base64.getEncoder().encodeToString(encrypted)
        }
        writer.write(line)
        writer.newLine()
        writer.flush()
    }

    fun enableTransportEncryption(key: SecretKey) {
        transportKey = key
    }

    override fun close() {
        socket.close()
    }
}
