package com.shterneregen.securelan.chat.transport

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.common.net.transport.LineTextChannel
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Objects
import javax.crypto.SecretKey

class ChatSocketSession @Throws(IOException::class) constructor(socket: Socket) : Closeable {
    private val channel = LineTextChannel(socket)

    @Volatile
    private var transportKey: SecretKey? = null

    @Volatile
    private var aesGcmCryptoService: AesGcmCryptoService? = null

    @Throws(IOException::class)
    fun readMessage(): WireMessage? {
        var line = channel.readLine() ?: return null
        if (isSecure()) {
            val encrypted = Base64.getDecoder().decode(line)
            val decrypted = aesGcmCryptoService!!.decrypt(encrypted, transportKey!!)
            line = String(decrypted, StandardCharsets.UTF_8)
        }
        return WireMessage.deserialize(line)
    }

    @Throws(IOException::class)
    fun writeMessage(message: WireMessage) {
        var line = message.serialize()
        if (isSecure()) {
            val encrypted = aesGcmCryptoService!!.encrypt(line.toByteArray(StandardCharsets.UTF_8), transportKey!!)
            line = Base64.getEncoder().encodeToString(encrypted)
        }
        channel.writeLine(line)
    }

    fun enableTransportEncryption(secretKey: SecretKey, aesGcmCryptoService: AesGcmCryptoService) {
        transportKey = Objects.requireNonNull(secretKey, "secretKey")
        this.aesGcmCryptoService = Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService")
    }

    fun isSecure(): Boolean = transportKey != null && aesGcmCryptoService != null

    fun remoteAddress(): String = channel.remoteAddress()

    @Throws(IOException::class)
    override fun close() {
        channel.close()
    }
}
