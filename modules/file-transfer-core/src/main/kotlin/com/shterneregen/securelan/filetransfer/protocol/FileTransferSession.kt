package com.shterneregen.securelan.filetransfer.protocol

import com.shterneregen.securelan.common.net.transport.LengthPrefixedFrameChannel
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Objects
import javax.crypto.SecretKey

/**
 * Manages an active file transfer TCP connection, providing framed plaintext
 * and authenticated encrypted communication using AES-GCM.
 *
 * Initially, frames are transferred in plaintext (e.g. during protocol banner and key exchange).
 * Once [enableTransportEncryption] is called, encrypted text and byte operations become available.
 *
 * @param socket Underlying connected [Socket].
 * @throws IOException If the channel cannot be initialized from the socket.
 */
class FileTransferSession @Throws(IOException::class) constructor(socket: Socket) : Closeable {
    private val channel = LengthPrefixedFrameChannel(socket)

    @Volatile
    private var transportKey: SecretKey? = null

    @Volatile
    private var aesGcmCryptoService: AesGcmCryptoService? = null

    /**
     * Writes a UTF-8 string framed with a length prefix.
     *
     * @param value Text to write.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeUtf(value: String) {
        channel.writeUtf(value)
    }

    /**
     * Reads a UTF-8 string from the underlying channel.
     *
     * @return Decoded string.
     * @throws IOException If an I/O error occurs or the connection is terminated.
     */
    @Throws(IOException::class)
    fun readUtf(): String = channel.readUtf()

    /**
     * Writes raw bytes framed with a length prefix.
     *
     * @param bytes Byte array to transmit.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeBytes(bytes: ByteArray) {
        channel.writeFrame(bytes)
    }

    /**
     * Reads a framed byte array from the channel.
     *
     * @return Received byte array.
     * @throws IOException If an I/O error occurs or the connection is terminated.
     */
    @Throws(IOException::class)
    fun readBytes(): ByteArray = channel.readFrame()

    /**
     * Encrypts and writes a UTF-8 string payload.
     *
     * @param value Text to encrypt and send.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalStateException If transport encryption has not been enabled.
     */
    @Throws(IOException::class)
    fun writeEncryptedText(value: String) {
        writeEncryptedBytes(value.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Reads an encrypted payload and decrypts it into a UTF-8 string.
     *
     * @return Decrypted string.
     * @throws IOException If an I/O error or decryption error occurs.
     * @throws IllegalStateException If transport encryption has not been enabled.
     */
    @Throws(IOException::class)
    fun readEncryptedText(): String = String(readEncryptedBytes(), StandardCharsets.UTF_8)

    /**
     * Encrypts plaintext bytes using AES-GCM and writes them to the channel.
     *
     * @param plainBytes Plaintext bytes to encrypt and send.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalStateException If transport encryption has not been enabled.
     */
    @Throws(IOException::class)
    fun writeEncryptedBytes(plainBytes: ByteArray) {
        val (key, service) = requireSecureContext()
        writeBytes(service.encrypt(plainBytes, key))
    }

    /**
     * Reads an encrypted frame from the channel and decrypts it using AES-GCM.
     *
     * @return Decrypted plaintext bytes.
     * @throws IOException If an I/O error or decryption error occurs.
     * @throws IllegalStateException If transport encryption has not been enabled.
     */
    @Throws(IOException::class)
    fun readEncryptedBytes(): ByteArray {
        val (key, service) = requireSecureContext()
        return service.decrypt(readBytes(), key)
    }

    /**
     * Enables symmetric transport encryption for subsequent encrypted operations.
     *
     * @param secretKey Negotiated AES secret key.
     * @param aesGcmCryptoService Crypto service instance used to encrypt/decrypt frames.
     */
    fun enableTransportEncryption(secretKey: SecretKey, aesGcmCryptoService: AesGcmCryptoService) {
        transportKey = Objects.requireNonNull(secretKey, "secretKey")
        this.aesGcmCryptoService = Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService")
    }

    /**
     * Returns the formatted remote socket address.
     *
     * @return Remote address representation.
     */
    fun remoteAddress(): String = channel.remoteAddress()

    private fun requireSecureContext(): Pair<SecretKey, AesGcmCryptoService> {
        val key = transportKey
        val service = aesGcmCryptoService
        if (key == null || service == null) {
            throw IllegalStateException("Secure transport is not enabled")
        }
        return Pair(key, service)
    }

    /**
     * Closes the underlying socket and frame channel.
     *
     * @throws IOException If an I/O error occurs during channel closure.
     */
    @Throws(IOException::class)
    override fun close() {
        channel.close()
    }
}
