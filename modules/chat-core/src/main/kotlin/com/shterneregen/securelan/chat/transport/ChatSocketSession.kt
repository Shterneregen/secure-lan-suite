package com.shterneregen.securelan.chat.transport

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.common.net.transport.LineTextChannel
import com.shterneregen.securelan.common.net.transport.SocketDeadlines
import com.shterneregen.securelan.crypto.protocol.SecureRecordCipher
import com.shterneregen.securelan.crypto.protocol.SecureSessionKeys
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.util.Base64

/** Bounded line transport with a latching, ordered, mutually authenticated V2 record channel. */
class ChatSocketSession(private val socket: Socket) : Closeable {
    private val channel = LineTextChannel(socket)
    @Volatile private var cipher: SecureRecordCipher? = null

    fun readMessage(): WireMessage? {
        val line = channel.readLine(if (cipher == null) 4096 else 90_000) ?: return null
        if (cipher == null && line.length > 4096) throw IOException("Handshake exceeds maximum size")
        return try {
            val text = cipher?.decrypt(Base64.getDecoder().decode(line))?.toString(Charsets.UTF_8) ?: line
            WireMessage.deserialize(text)
        } catch (error: Exception) {
            close()
            throw IOException("Invalid chat frame", error)
        }
    }

    @Synchronized
    fun writeMessage(message: WireMessage) {
        val text = message.serialize()
        if (cipher == null && text.length > 4096) throw IOException("Handshake exceeds maximum size")
        val line = cipher?.let { Base64.getEncoder().encodeToString(it.encrypt(text.toByteArray(Charsets.UTF_8))) } ?: text
        channel.writeLine(line)
    }

    fun enableTransportEncryption(keys: SecureSessionKeys, client: Boolean = true) {
        check(cipher == null) { "Secure transport already enabled" }
        cipher = SecureRecordCipher(keys, client)
    }

    fun <T> handshake(action: () -> T): T = SocketDeadlines.within(socket, 10_000, action)
    fun isSecure(): Boolean = cipher != null
    fun remoteAddress(): String = channel.remoteAddress()
    fun setReadTimeout(timeoutMillis: Int) = channel.setReadTimeout(timeoutMillis)
    override fun close() = channel.close()
}
