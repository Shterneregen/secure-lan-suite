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

/**
 * Wraps a TCP [Socket] with a line-oriented text protocol and optional AES-GCM transport encryption.
 *
 * ## Plaintext mode
 * [WireMessage]s are serialized to a single line by [WireMessage.serialize], written to the socket
 * via [LineTextChannel], and deserialized on the read path.
 *
 * ## Encrypted mode
 * Once [enableTransportEncryption] has been called, every outbound line is encrypted with
 * AES-GCM (via [AesGcmCryptoService]), Base64-encoded, and written as a single line.  Inbound
 * lines are Base64-decoded and decrypted symmetrically before being passed to
 * [WireMessage.deserialize].
 *
 * Encryption is one-way latching: once enabled it cannot be disabled for the lifetime of the
 * session, matching the handshake protocol design.
 *
 * @param socket The connected TCP socket.
 * @throws IOException if the underlying [LineTextChannel] cannot be opened.
 */
class ChatSocketSession @Throws(IOException::class) constructor(socket: Socket) : Closeable {

    private val channel = LineTextChannel(socket)

    @Volatile
    private var transportKey: SecretKey? = null

    @Volatile
    private var aesGcmCryptoService: AesGcmCryptoService? = null

    /**
     * Reads the next [WireMessage] from the socket.
     *
     * If the session is in encrypted mode, the raw line is Base64-decoded and decrypted before
     * deserialization.
     *
     * @return The next [WireMessage], or `null` if the remote end closed the connection (EOF).
     * @throws IOException if a network error occurs or the decrypted data cannot be deserialized.
     */
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

    /**
     * Serializes and writes [message] to the socket.
     *
     * If the session is in encrypted mode, the serialized line is AES-GCM-encrypted and
     * Base64-encoded before being written.
     *
     * @param message The [WireMessage] to send.
     * @throws IOException if a network error occurs or encryption fails.
     */
    @Throws(IOException::class)
    fun writeMessage(message: WireMessage) {
        var line = message.serialize()
        if (isSecure()) {
            val encrypted = aesGcmCryptoService!!.encrypt(line.toByteArray(StandardCharsets.UTF_8), transportKey!!)
            line = Base64.getEncoder().encodeToString(encrypted)
        }
        channel.writeLine(line)
    }

    /**
     * Switches the session into encrypted mode using [secretKey] and [aesGcmCryptoService].
     *
     * After this call, all subsequent [readMessage] and [writeMessage] operations are
     * transparently encrypted/decrypted.  This method should be called exactly once per
     * session, at the end of the handshake.
     *
     * @param secretKey          The AES session key negotiated during the handshake.
     * @param aesGcmCryptoService The AES-GCM service implementation.
     * @throws NullPointerException if either parameter is `null`.
     */
    fun enableTransportEncryption(secretKey: SecretKey, aesGcmCryptoService: AesGcmCryptoService) {
        transportKey = Objects.requireNonNull(secretKey, "secretKey")
        this.aesGcmCryptoService = Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService")
    }

    /**
     * Returns `true` if transport encryption has been enabled via [enableTransportEncryption].
     */
    fun isSecure(): Boolean = transportKey != null && aesGcmCryptoService != null

    /**
     * Returns the remote address of the underlying socket (e.g. `"192.168.1.5:54321"`).
     *
     * @return The remote address string, or an empty string if not available.
     */
    fun remoteAddress(): String = channel.remoteAddress()

    /**
     * Sets the socket read timeout.
     *
     * A value of `0` means infinite timeout (no timeout).  Should be set to a finite value
     * during the handshake phase to prevent indefinite blocking.
     *
     * @param timeoutMillis The timeout in milliseconds; `0` disables the timeout.
     */
    fun setReadTimeout(timeoutMillis: Int) {
        channel.setReadTimeout(timeoutMillis)
    }

    /**
     * Closes the underlying socket and releases all associated resources.
     *
     * @throws IOException if the socket cannot be closed.
     */
    @Throws(IOException::class)
    override fun close() {
        channel.close()
    }
}
