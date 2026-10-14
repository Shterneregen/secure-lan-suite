package com.shterneregen.securelan.filetransfer.protocol

import com.shterneregen.securelan.common.net.transport.LengthPrefixedFrameChannel
import com.shterneregen.securelan.common.net.transport.SocketDeadlines
import com.shterneregen.securelan.crypto.protocol.SecureRecordCipher
import com.shterneregen.securelan.crypto.protocol.SecureSessionKeys
import java.io.Closeable
import java.io.IOException
import java.net.Socket

/** Bounded framing and ordered authenticated records shared by desktop and Android. */
class FileTransferSession(private val socket: Socket) : Closeable {
    private val channel = LengthPrefixedFrameChannel(socket, SecureRecordCipher.MAX_RECORD_BYTES)
    private var cipher: SecureRecordCipher? = null
    private var keys: SecureSessionKeys? = null
    fun writeUtf(value: String) = channel.writeUtf(value)
    fun readUtf(): String = channel.readUtf()
    fun writeBytes(bytes: ByteArray) = channel.writeFrame(bytes)
    fun readBytes(): ByteArray = channel.readFrame()
    fun writeEncryptedText(value: String) = writeEncryptedBytes(value.toByteArray(Charsets.UTF_8))
    fun readEncryptedText(timeoutMillis: Long = 60_000): String = readEncryptedBytes(timeoutMillis).toString(Charsets.UTF_8)

    @Synchronized
    fun writeEncryptedBytes(bytes: ByteArray) {
        val secure = cipher ?: throw IOException("Secure transport is not enabled")
        SocketDeadlines.within(socket, 60_000) { channel.writeFrame(secure.encrypt(bytes)) }
    }

    fun readEncryptedBytes(timeoutMillis: Long = 60_000): ByteArray {
        val secure = cipher ?: throw IOException("Secure transport is not enabled")
        return SocketDeadlines.within(socket, timeoutMillis) {
            try { secure.decrypt(channel.readFrame()) } catch (error: Exception) {
                close()
                throw IOException("Invalid file record", error)
            }
        }
    }

    fun enableTransportEncryption(keys: SecureSessionKeys, client: Boolean = true) {
        check(cipher == null) { "Secure transport already enabled" }
        this.keys = keys
        cipher = SecureRecordCipher(keys, client)
    }

    fun binding(): ByteArray = keys?.binding() ?: throw IOException("Secure transport is not enabled")
    fun <T> handshake(action: () -> T): T = SocketDeadlines.within(socket, 10_000, action)
    fun remoteAddress(): String = channel.remoteAddress()
    override fun close() = channel.close()
}
