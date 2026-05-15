package com.shterneregen.securelan.androidclient.protocol

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

class FileTransferSession(private val socket: Socket) : Closeable {
    private val input = DataInputStream(socket.getInputStream())
    private val output = DataOutputStream(socket.getOutputStream())
    private var transportKey: SecretKey? = null

    fun writeUtf(value: String) {
        synchronized(output) {
            output.writeUTF(value)
            output.flush()
        }
    }

    fun readUtf(): String = input.readUTF()

    fun writeBytes(bytes: ByteArray) {
        synchronized(output) {
            output.writeInt(bytes.size)
            output.write(bytes)
            output.flush()
        }
    }

    fun readBytes(): ByteArray {
        val length = input.readInt()
        require(length >= 0) { "Negative frame length" }
        return input.readNBytesCompat(length)
    }

    fun enableTransportEncryption(key: SecretKey) {
        transportKey = key
    }

    fun writeEncryptedText(value: String) = writeEncryptedBytes(value.toByteArray(StandardCharsets.UTF_8))

    fun readEncryptedText(): String = String(readEncryptedBytes(), StandardCharsets.UTF_8)

    fun writeEncryptedBytes(bytes: ByteArray) {
        val key = requireNotNull(transportKey) { "Secure transport is not enabled" }
        writeBytes(CryptoCompat.aesEncrypt(bytes, key))
    }

    fun readEncryptedBytes(): ByteArray {
        val key = requireNotNull(transportKey) { "Secure transport is not enabled" }
        return CryptoCompat.aesDecrypt(readBytes(), key)
    }

    override fun close() {
        socket.close()
    }

    private fun DataInputStream.readNBytesCompat(length: Int): ByteArray {
        val result = ByteArray(length)
        readFully(result)
        return result
    }
}
