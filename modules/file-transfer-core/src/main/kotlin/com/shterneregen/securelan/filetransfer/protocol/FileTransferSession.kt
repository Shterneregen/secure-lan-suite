package com.shterneregen.securelan.filetransfer.protocol

import com.shterneregen.securelan.common.net.transport.LengthPrefixedFrameChannel
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Objects
import javax.crypto.SecretKey

class FileTransferSession @Throws(IOException::class) constructor(socket: Socket) : Closeable {
    private val channel = LengthPrefixedFrameChannel(socket)

    @Volatile
    private var transportKey: SecretKey? = null

    @Volatile
    private var aesGcmCryptoService: AesGcmCryptoService? = null

    @Throws(IOException::class)
    fun writeUtf(value: String) {
        channel.writeUtf(value)
    }

    @Throws(IOException::class)
    fun readUtf(): String = channel.readUtf()

    @Throws(IOException::class)
    fun writeBytes(bytes: ByteArray) {
        channel.writeFrame(bytes)
    }

    @Throws(IOException::class)
    fun readBytes(): ByteArray = channel.readFrame()

    @Throws(IOException::class)
    fun writeEncryptedText(value: String) {
        writeEncryptedBytes(value.toByteArray(StandardCharsets.UTF_8))
    }

    @Throws(IOException::class)
    fun readEncryptedText(): String = String(readEncryptedBytes(), StandardCharsets.UTF_8)

    @Throws(IOException::class)
    fun writeEncryptedBytes(plainBytes: ByteArray) {
        ensureSecure()
        writeBytes(aesGcmCryptoService!!.encrypt(plainBytes, transportKey!!))
    }

    @Throws(IOException::class)
    fun readEncryptedBytes(): ByteArray {
        ensureSecure()
        return aesGcmCryptoService!!.decrypt(readBytes(), transportKey!!)
    }

    fun enableTransportEncryption(secretKey: SecretKey, aesGcmCryptoService: AesGcmCryptoService) {
        transportKey = Objects.requireNonNull(secretKey, "secretKey")
        this.aesGcmCryptoService = Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService")
    }

    fun remoteAddress(): String = channel.remoteAddress()

    private fun ensureSecure() {
        if (transportKey == null || aesGcmCryptoService == null) {
            throw IllegalStateException("Secure transport is not enabled")
        }
    }

    @Throws(IOException::class)
    override fun close() {
        channel.close()
    }
}
